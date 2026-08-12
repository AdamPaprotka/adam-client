package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.RotationManager;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class KillAura extends Module {
    public static KillAura INSTANCE;

    private final FloatSetting range = new FloatSetting.Builder("Range")
            .defaultValue(4.5f).min(0f).max(10f).minSlider(0f).maxSlider(6f).build();
    private final BoolSetting players       = new BoolSetting("Players", true);
    private final BoolSetting mobs          = new BoolSetting("Mobs", true);
    private final BoolSetting animals       = new BoolSetting("Animals", false);
    private final FloatSetting delay = new FloatSetting.Builder("Delay")
            .defaultValue(0f).min(0f).minSlider(0f).maxSlider(1000f).build();
    private final BoolSetting noRotate      = new BoolSetting("NoRotate", false);
    private final BoolSetting smoothRotation = new BoolSetting("Smoothing", true);
    private final FloatSetting rotationSpeed = new FloatSetting.Builder("Rotation Speed")
            .defaultValue(30f).min(1f).max(180f).minSlider(5f).maxSlider(90f).build();
    private final BoolSetting snapHit       = new BoolSetting("Snap Hit", false);

    private final BoolSetting smart         = new BoolSetting("Smart", false);
    private final FloatSetting keepDistance = new FloatSetting.Builder("Keep Distance")
            .defaultValue(3f).min(0.5f).max(10f).minSlider(0.5f).maxSlider(6f).build();
    private final FloatSetting smartSpeed   = new FloatSetting.Builder("Smart Speed")
            .defaultValue(0.1f).min(0.05f).max(0.2f).minSlider(0.05f).maxSlider(0.2f).build();
    private final BoolSetting antiWall      = new BoolSetting("Anti Wall", true);
    private final BoolSetting antiHole      = new BoolSetting("Anti Hole", true);

    /** Orbit direction for Smart strafing: +1 = CCW, -1 = CW. Flipped by anti-wall/anti-hole. */
    private int circleDir = 1;

    private final BoolSetting tp            = new BoolSetting("TP", false);
    private final FloatSetting tpRange      = new FloatSetting.Builder("TP Range")
            .defaultValue(20f).min(1f).max(1000f).minSlider(1f).maxSlider(100f).build();

    private final BoolSetting autoDelay     = new BoolSetting("Auto Delay", false);

    /** Extra random tick delay before attacking, like FakeLag's random extra but tick-based. */
    private final FloatSetting randomTicks  = new FloatSetting.Builder("Random Ticks")
            .defaultValue(0f).min(0f).max(40f).minSlider(0f).maxSlider(20f).build();

    private long lastAttackTime = 0;
    private int ticksSinceReady = 0;
    private int currentRandomTickDelay = 0;

    /** True only on the tick KillAura actually lands a swing — read by AutoShield to duck the block. */
    public static volatile boolean attacking = false;

    public KillAura() {
        super("KillAura", Category.COMBAT);
        INSTANCE = this;
        addSetting(range);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(delay);
        addSetting(noRotate);
        addSetting(smoothRotation);
        addSetting(rotationSpeed);
        addSetting(snapHit);
        addSetting(smart);
        addSetting(keepDistance);
        addSetting(smartSpeed);
        addSetting(antiWall);
        addSetting(antiHole);
        addSetting(tp);
        addSetting(tpRange);
        addSetting(autoDelay);
        addSetting(randomTicks);
    }

    @Override
    protected void onDisable() {
        attacking = false;
        RotationManager.killAuraRequest = false;
    }

    @Override
    public void onTick() {
        attacking = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        float r = range.getValue();
        float scanR = tp.getValue() ? Math.max(r, tpRange.getValue()) : r;
        LivingEntity target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == mc.player) continue;
            if (living.isDead() || living.getHealth() <= 0) continue;

            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isMob    = entity instanceof MobEntity;
            boolean isAnimal = entity instanceof AnimalEntity;

            if (isPlayer  && !players.getValue()) continue;
            if (isMob     && !mobs.getValue())    continue;
            if (isAnimal  && !animals.getValue()) continue;
            if (!isPlayer && !isMob && !isAnimal) continue;

            double dist = mc.player.squaredDistanceTo(entity);
            if (dist > scanR * scanR || dist >= closestDist) continue;

            closestDist = dist;
            target = living;
        }

        if (target == null) {
            RotationManager.killAuraRequest = false;
            return;
        }

        // Keep the snap hidden from the server for as long as we're tracking a target, not
        // just on the attack tick itself - the anticheat flag fires on the rotation being
        // inconsistent with the player's own mouse movement while it's snapped, and that
        // window covers every tick the snap is held, not only the moment of the hit.
        RotationManager.killAuraRequest = snapHit.getValue();

        // TP to attack range if target is too far away
        if (tp.getValue()) {
            double dist = Math.sqrt(mc.player.squaredDistanceTo(target));
            if (dist > r) {
                double dx = target.getX() - mc.player.getX();
                double dz = target.getZ() - mc.player.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                double offset = Math.max(0, r - 0.5);
                mc.player.setPosition(
                    target.getX() - dx / len * offset,
                    target.getY(),
                    target.getZ() - dz / len * offset
                );
            }
        }

        if (smart.getValue()) applySmart(mc, target);

        long now = System.currentTimeMillis();
        long effectiveDelay = autoDelay.getValue()
                ? computeAutoDelay(mc, target)
                : (long)(float) delay.getValue();
        if (now - lastAttackTime < effectiveDelay) return;

        if (ticksSinceReady++ < currentRandomTickDelay) return;
        ticksSinceReady = 0;
        currentRandomTickDelay = (int) (Math.random() * (randomTicks.getValue() + 1));

        attacking = true;

        if (snapHit.getValue()) {
            // Snap and attack, but do NOT revert rotation right after - an instant
            // snap-then-revert on consecutive packets is exactly what anticheats key
            // off of to flag killaura. Leaving it snapped lets it decay naturally
            // instead of an obvious same-tick round trip.
            rotateTo(mc, target, true);
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            if (!noRotate.getValue()) rotateTo(mc, target, smoothRotation.getValue());
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        lastAttackTime = now;
    }

    /** Strafe in a circle around the target while maintaining keepDistance. */
    private void applySmart(MinecraftClient mc, LivingEntity target) {
        double radius = keepDistance.getValue();

        // Current angle of the player around the target.
        double toPlayerX = mc.player.getX() - target.getX();
        double toPlayerZ = mc.player.getZ() - target.getZ();
        double angle = Math.atan2(toPlayerZ, toPlayerX);

        // Angular step scaled by 1/radius so tangential speed stays ~= smartSpeed.
        double angularStep = smartSpeed.getValue() / Math.max(0.5, radius);

        // Probe the position one step ahead in the current direction. If a wall or
        // hole is in the way, flip the orbit direction and go the OTHER way instead.
        if (antiWall.getValue() || antiHole.getValue()) {
            double probeAngle = angle + angularStep * circleDir;
            double px = target.getX() + Math.cos(probeAngle) * radius;
            double pz = target.getZ() + Math.sin(probeAngle) * radius;
            if (isUnsafe(mc, px, pz)) circleDir = -circleDir;
        }

        // Goal sits on the circle of radius keepDistance at the advanced angle,
        // so steering toward it both circles AND corrects the distance.
        double goalAngle = angle + angularStep * circleDir;
        double goalX = target.getX() + Math.cos(goalAngle) * radius;
        double goalZ = target.getZ() + Math.sin(goalAngle) * radius;

        double dx = goalX - mc.player.getX();
        double dz = goalZ - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        Vec3d vel = mc.player.getVelocity();
        if (dist > 0.05) {
            double spd = Math.min(smartSpeed.getValue(), dist * 0.5);
            mc.player.setVelocity(dx / dist * spd, vel.y, dz / dist * spd);
        } else {
            mc.player.setVelocity(vel.x * 0.3, vel.y, vel.z * 0.3);
        }
    }

    /** True if stepping to (x,z) would run into a wall or over a 3-block hole. */
    private boolean isUnsafe(MinecraftClient mc, double x, double z) {
        BlockPos feet = BlockPos.ofFloored(x, mc.player.getY(), z);

        // Anti-wall: a solid block at foot or head height blocks the strafe.
        if (antiWall.getValue() && (isSolid(mc, feet) || isSolid(mc, feet.up()))) {
            return true;
        }

        // Anti-hole: no solid footing within 3 blocks below the next step.
        if (antiHole.getValue()) {
            boolean hole = true;
            for (int i = 1; i <= 3; i++) {
                if (isSolid(mc, feet.down(i))) { hole = false; break; }
            }
            if (hole) return true;
        }

        return false;
    }

    private boolean isSolid(MinecraftClient mc, BlockPos pos) {
        return !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    /** Delay matching the player's attack cooldown so every hit is a crit-eligible full-charge swing. */
    private long computeAutoDelay(MinecraftClient mc, LivingEntity target) {
        float cooldownTicks = mc.player.getAttackCooldownProgressPerTick();
        // convert ticks to ms, add a small buffer so the cooldown always reaches 1.0
        return (long)(cooldownTicks * 50) + 50;
    }

    private void rotateTo(MinecraftClient mc, Entity target, boolean smooth) {
        double dx = target.getX() - mc.player.getX();
        double dy = target.getEyeY() - mc.player.getEyeY();
        double dz = target.getZ() - mc.player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));

        if (smooth) {
            float maxStep = rotationSpeed.getValue();
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
}
