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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class KillAura extends Module {
    public static KillAura INSTANCE;

    // Vanilla's entity-attack reach attribute defaults to 3.0 - 4.5 is the *block* interaction
    // reach, not entity. 3.0 is the exact legal boundary with zero margin, though: against a
    // moving target, ordinary latency between the client's rendered position for them and what
    // the server currently has pushes attacks right at that edge over the server's own limit.
    // 2.7 leaves enough room to absorb that jitter without meaningfully changing effective reach.
    private final FloatSetting range = new FloatSetting.Builder("Range")
            .defaultValue(2.7f).min(0f).max(10f).minSlider(0f).maxSlider(6f).build();
    private final BoolSetting players       = new BoolSetting("Players", true);
    private final BoolSetting mobs          = new BoolSetting("Mobs", true);
    private final BoolSetting animals       = new BoolSetting("Animals", false);
    // 0ms means attacking every single client tick (20/sec) - far past any human click rate,
    // which is exactly what a multi-actions/rate check catches. ~9 CPS is a believable default.
    private final FloatSetting delay = new FloatSetting.Builder("Delay")
            .defaultValue(110f).min(0f).minSlider(0f).maxSlider(1000f).build();
    private final BoolSetting noRotate      = new BoolSetting("NoRotate", false);
    // A believable "fast flick" default: fast enough to feel responsive, but still a real
    // multi-tick turn the server sees you actually make toward the target before you hit it,
    // rather than an instant snap (which is what triggers a rotation-consistency flag).
    private final FloatSetting rotationSpeed = new FloatSetting.Builder("Rotation Speed")
            .defaultValue(55f).min(1f).max(180f).minSlider(5f).maxSlider(90f).build();
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
            .defaultValue(2f).min(0f).max(40f).minSlider(0f).maxSlider(20f).build();

    // Delay is time BETWEEN attacks; this is different - time from a target FIRST becoming
    // targetable to the FIRST hit on it. Instantly hitting the moment a target appears is a near-
    // zero reaction time, which is exactly what a transaction-timestamp-based check is built to
    // catch (see class doc on onEarlyTick for why plain packet-order tricks can't address this).
    private final FloatSetting reactionDelay = new FloatSetting.Builder("Reaction Delay")
            .defaultValue(120f).min(0f).max(500f).minSlider(0f).maxSlider(400f).build();

    private long lastAttackTime = 0;
    private int ticksSinceReady = 0;
    private int currentRandomTickDelay = 0;

    /** Target picked in onEarlyTick, consumed by onTick - see class doc for why the split exists. */
    private LivingEntity currentTarget = null;
    private LivingEntity lastAcquiredTarget = null;
    private long targetAcquiredTime = 0;
    private long currentReactionDelay = 0;

    /** Last rotation actually sent via flushLook - resending the exact same values is a dead giveaway. */
    private boolean hasFlushedLook = false;
    private float lastFlushedYaw = 0f;
    private float lastFlushedPitch = 0f;

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
        addSetting(reactionDelay);
    }

    @Override
    protected void onDisable() {
        attacking = false;
        hasFlushedLook = false;
        currentTarget = null;
        lastAcquiredTarget = null;
    }

    /**
     * Finds the target and rotates BEFORE the tick's normal movement packet is sent (this runs
     * on START_CLIENT_TICK), so smoothed rotation lands in that already-happening packet instead
     * of needing an extra explicit send later. Sending an extra packet per attack, even a
     * correctly-ordered one, still raises the average packet rate above vanilla's fixed one-per-
     * tick budget once sustained during combat - that's what was triggering a Timer flag.
     * Snap Hit deliberately does NOT rotate here - snapping every single tick a target exists
     * (rather than only at the moment of attack) would be a constant, obvious instant-lock.
     */
    @Override
    public void onEarlyTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        currentTarget = null;
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = findTarget(mc);
        currentTarget = target;
        if (target != lastAcquiredTarget) {
            lastAcquiredTarget = target;
            targetAcquiredTime = System.currentTimeMillis();
            currentReactionDelay = (long) (float) reactionDelay.getValue() + (long) (Math.random() * 100);
        }
        if (target == null) return;

        if (tp.getValue()) {
            double dist = Math.sqrt(mc.player.squaredDistanceTo(target));
            if (dist > range.getValue()) {
                double dx = target.getX() - mc.player.getX();
                double dz = target.getZ() - mc.player.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                double offset = Math.max(0, range.getValue() - 0.5);
                mc.player.setPosition(
                    target.getX() - dx / len * offset,
                    target.getY(),
                    target.getZ() - dz / len * offset
                );
            }
        }

        if (smart.getValue()) applySmart(mc, target);

        // Always step gradually here regardless of the Smoothing setting - this runs every
        // single tick a target exists, so a true instant snap here is a permanent lock onto the
        // target's exact angle every tick, not a one-off snap. That's strictly worse than Snap
        // Hit's attack-only snap and is exactly what was still tripping Post. True instant
        // rotation is reserved for Snap Hit's attack-moment-only path below.
        if (!snapHit.getValue() && !noRotate.getValue()) {
            rotateTo(mc, target, true);
        }
    }

    private LivingEntity findTarget(MinecraftClient mc) {
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
        return target;
    }

    @Override
    public void onTick() {
        attacking = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        LivingEntity target = currentTarget;
        if (target == null || target.isRemoved() || target.isDead() || target.getHealth() <= 0) return;

        long now = System.currentTimeMillis();
        if (now - targetAcquiredTime < currentReactionDelay) return;

        long effectiveDelay = autoDelay.getValue()
                ? computeAutoDelay(mc, target)
                : (long)(float) delay.getValue();
        if (now - lastAttackTime < effectiveDelay) return;

        if (ticksSinceReady++ < currentRandomTickDelay) return;
        ticksSinceReady = 0;
        currentRandomTickDelay = (int) (Math.random() * (randomTicks.getValue() + 1));

        attacking = true;

        if (snapHit.getValue()) {
            // Snap Hit's rotation happens here, not in onEarlyTick, so the snap only ever occurs
            // on the actual attack tick - flushLook still exists to keep packet order correct
            // for this specific mode, at the cost of the extra-packet tradeoff explained above.
            float savedYaw = mc.player.getYaw();
            float savedPitch = mc.player.getPitch();
            rotateTo(mc, target, true);
            flushLook(mc);
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.setYaw(savedYaw);
            mc.player.setPitch(savedPitch);
        } else {
            // Rotation (if any) already happened in onEarlyTick and is already reflected in
            // this tick's normal movement packet - no extra send needed here.
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        lastAttackTime = now;
    }

    /**
     * Sends the current real rotation immediately, so it reaches the server before the attack
     * packet does. Skips the send entirely if it wouldn't change anything - real mouse input
     * never produces bit-identical yaw/pitch tick to tick, so repeating the exact same values is
     * itself a tell, distinct from whether the rotation is correct in the first place.
     */
    private void flushLook(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null) return;
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        if (hasFlushedLook && yaw == lastFlushedYaw && pitch == lastFlushedPitch) return;

        mc.getNetworkHandler().getConnection().send(
                new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), false));
        hasFlushedLook = true;
        lastFlushedYaw = yaw;
        lastFlushedPitch = pitch;
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
