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
import net.minecraft.util.math.MathHelper;

/**
 * Distinct from KillAura: this never attacks or picks a target on its own. It only nudges your
 * rotation toward a nearby target that's already close to your crosshair, and only while you're
 * actively attacking - closer to assisted aim than automated combat. Still a continuous rotation
 * step each tick, so it carries the same statistical-aim-check exposure KillAura's Smoothing mode
 * does on checks like Vulcan's Aim (Constant/Linear/Analysis).
 */
public class AimAssist extends Module {
    private final FloatSetting range = new FloatSetting.Builder("Range")
            .defaultValue(4.5f).min(1f).max(10f).minSlider(1f).maxSlider(6f).build();
    private final FloatSetting fov = new FloatSetting.Builder("FOV")
            .defaultValue(20f).min(1f).max(90f).minSlider(5f).maxSlider(60f).build();
    private final FloatSetting strength = new FloatSetting.Builder("Strength")
            .defaultValue(25f).min(1f).max(180f).minSlider(5f).maxSlider(90f).build();
    private final BoolSetting onlyWhileAttacking = new BoolSetting("Only While Attacking", true);
    private final BoolSetting players = new BoolSetting("Players", true);
    private final BoolSetting mobs    = new BoolSetting("Mobs", true);
    private final BoolSetting animals = new BoolSetting("Animals", false);

    public AimAssist() {
        super("AimAssist", Category.COMBAT);
        addSetting(range);
        addSetting(fov);
        addSetting(strength);
        addSetting(onlyWhileAttacking);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
    }

    @Override
    public void onEarlyTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (onlyWhileAttacking.getValue() && !mc.options.attackKey.isPressed()) return;

        LivingEntity target = findTarget(mc);
        if (target == null) return;

        rotateToward(mc, target);
    }

    /** Nearest eligible entity within range AND within FOV of the player's current look direction. */
    private LivingEntity findTarget(MinecraftClient mc) {
        float r = range.getValue();
        float halfFov = fov.getValue() / 2f;
        LivingEntity best = null;
        double closestAngle = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity living) || living.isDead() || living.getHealth() <= 0) continue;

            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isMob    = entity instanceof MobEntity;
            boolean isAnimal = entity instanceof AnimalEntity;
            if (isPlayer  && !players.getValue()) continue;
            if (isMob     && !mobs.getValue())    continue;
            if (isAnimal  && !animals.getValue()) continue;
            if (!isPlayer && !isMob && !isAnimal) continue;

            if (mc.player.squaredDistanceTo(entity) > r * r) continue;

            double angle = angleTo(mc, living);
            if (angle > halfFov || angle >= closestAngle) continue;

            closestAngle = angle;
            best = living;
        }
        return best;
    }

    /** Angle in degrees between the player's current look direction and the target's eye position. */
    private double angleTo(MinecraftClient mc, Entity target) {
        double dx = target.getX() - mc.player.getX();
        double dy = target.getEyeY() - mc.player.getEyeY();
        double dz = target.getZ() - mc.player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float wantYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));

        float yawDiff = MathHelper.wrapDegrees(wantYaw - mc.player.getYaw());
        float pitchDiff = wantPitch - mc.player.getPitch();
        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private void rotateToward(MinecraftClient mc, Entity target) {
        double dx = target.getX() - mc.player.getX();
        double dy = target.getEyeY() - mc.player.getEyeY();
        double dz = target.getZ() - mc.player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));

        float maxStep = strength.getValue();
        float curYaw   = mc.player.getYaw();
        float curPitch = mc.player.getPitch();

        float yawDiff   = MathHelper.wrapDegrees(targetYaw - curYaw);
        float pitchDiff = targetPitch - curPitch;

        mc.player.setYaw(curYaw + MathHelper.clamp(yawDiff, -maxStep, maxStep));
        mc.player.setPitch(MathHelper.clamp(
                curPitch + MathHelper.clamp(pitchDiff, -maxStep, maxStep), -90f, 90f));
    }
}
