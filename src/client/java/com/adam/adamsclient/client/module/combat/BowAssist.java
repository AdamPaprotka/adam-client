package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.math.MathHelper;

public class BowAssist extends Module {
    private final FloatSetting fov = new FloatSetting.Builder("FOV")
            .defaultValue(90f).min(1f).max(360f).minSlider(10f).maxSlider(360f).build();
    private final FloatSetting range = new FloatSetting.Builder("Range")
            .defaultValue(50f).min(5f).max(120f).minSlider(10f).maxSlider(100f).build();
    private final BoolSetting players = new BoolSetting("Players", true);
    private final BoolSetting mobs    = new BoolSetting("Mobs", true);
    private final BoolSetting animals = new BoolSetting("Animals", false);
    private final BoolSetting predict = new BoolSetting("Predict Motion", true);
    private final BoolSetting smoothRotation = new BoolSetting("Smoothing", true);
    private final FloatSetting rotationSpeed = new FloatSetting.Builder("Rotation Speed")
            .defaultValue(30f).min(1f).max(180f).minSlider(5f).maxSlider(90f).build();

    // Arrow physics (no-drag approximation, blocks/tick).
    private static final double GRAVITY = 0.05;

    public BowAssist() {
        super("BowAssist", Category.COMBAT);
        addSetting(fov);
        addSetting(range);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(predict);
        addSetting(smoothRotation);
        addSetting(rotationSpeed);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Only assist while actually drawing a bow.
        if (!mc.player.isUsingItem()) return;
        if (!(mc.player.getActiveItem().getItem() instanceof BowItem)) return;

        // Arrow launch speed scales with how far the bow is drawn (max 3.0 blocks/tick).
        float charge = BowItem.getPullProgress(mc.player.getItemUseTime());
        if (charge <= 0f) charge = 0.1f;
        double v = charge * 3.0;

        LivingEntity target = findTarget(mc);
        if (target == null) return;

        aimAt(mc, target, v);
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        float r = range.getValue();
        float maxFov = fov.getValue();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;
            if (living.isDead() || living.getHealth() <= 0) continue;

            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isMob    = entity instanceof MobEntity;
            boolean isAnimal = entity instanceof AnimalEntity;
            if (isPlayer && !players.getValue()) continue;
            if (isMob    && !mobs.getValue())    continue;
            if (isAnimal && !animals.getValue()) continue;
            if (!isPlayer && !isMob && !isAnimal) continue;

            double dist = Math.sqrt(mc.player.squaredDistanceTo(entity));
            if (dist > r) continue;

            // Angle between current look and the straight-line direction to the target.
            double angle = angleTo(mc, entity);
            if (angle > maxFov / 2.0) continue;

            // Prefer the target closest to the crosshair, tie-broken by distance.
            double score = angle + dist * 0.05;
            if (score < bestScore) { bestScore = score; best = living; }
        }
        return best;
    }

    /** Angular difference (degrees) between the player's view and the target. */
    private double angleTo(MinecraftClient mc, Entity target) {
        double dx = target.getX() - mc.player.getX();
        double dy = (target.getY() + target.getHeight() * 0.5) - mc.player.getEyeY();
        double dz = target.getZ() - mc.player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float wantYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));

        float yawDiff   = MathHelper.wrapDegrees(wantYaw - mc.player.getYaw());
        float pitchDiff = wantPitch - mc.player.getPitch();
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private void aimAt(MinecraftClient mc, LivingEntity target, double v) {
        // Aim at center mass, optionally leading by the target's velocity.
        double tx = target.getX();
        double ty = target.getY() + target.getHeight() * 0.5;
        double tz = target.getZ();

        double dx = tx - mc.player.getX();
        double dz = tz - mc.player.getZ();
        double d  = Math.sqrt(dx * dx + dz * dz);

        if (predict.getValue() && v > 0) {
            // Rough time of flight, then offset target by its horizontal velocity.
            double tof = d / v;
            tx += target.getVelocity().x * tof;
            tz += target.getVelocity().z * tof;
            dx = tx - mc.player.getX();
            dz = tz - mc.player.getZ();
            d  = Math.sqrt(dx * dx + dz * dz);
        }

        double dy = ty - mc.player.getEyeY();

        float targetYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = ballisticPitch(d, dy, v);

        if (smoothRotation.getValue()) {
            float maxStep  = rotationSpeed.getValue();
            float curYaw   = mc.player.getYaw();
            float curPitch = mc.player.getPitch();
            float yawDiff   = MathHelper.wrapDegrees(targetYaw - curYaw);
            float pitchDiff = targetPitch - curPitch;
            mc.player.setYaw(curYaw + MathHelper.clamp(yawDiff, -maxStep, maxStep));
            mc.player.setPitch(MathHelper.clamp(
                    curPitch + MathHelper.clamp(pitchDiff, -maxStep, maxStep), -90f, 90f));
        } else {
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(MathHelper.clamp(targetPitch, -90f, 90f));
        }
    }

    /**
     * Launch angle needed to hit a target at horizontal distance d and vertical
     * offset dy with initial speed v, using the projectile-motion solution
     * (drag ignored). Returns a Minecraft pitch (negative = up).
     */
    private float ballisticPitch(double d, double dy, double v) {
        double v2 = v * v;
        double root = v2 * v2 - GRAVITY * (GRAVITY * d * d + 2 * dy * v2);
        if (root < 0 || d <= 0) {
            // Out of range — just point straight at the target.
            return (float) -Math.toDegrees(Math.atan2(dy, d));
        }
        // Lower of the two arcs (flatter, faster shot).
        double angle = Math.atan((v2 - Math.sqrt(root)) / (GRAVITY * d));
        return (float) -Math.toDegrees(angle);
    }
}
