package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.RotationManager;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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

    private final BoolSetting showLanding = new BoolSetting("Show Landing", true);
    /** When on, only render a marker at the predicted impact point instead of the full arc. */
    private final BoolSetting tipOnly = new BoolSetting("Tip Only", false);
    /** Hide the auto-aim rotation from the server; it only sees rotation from actual mouse movement. */
    private final BoolSetting silentRotation = new BoolSetting("Silent Rotation", true);

    // Arrow physics (no-drag approximation, blocks/tick).
    private static final double GRAVITY = 0.05;
    /** Rough vanilla bow divergence (degrees) at full draw; more at lower charge. */
    private static final double BASE_SPREAD_DEG = 1.0;

    private Vec3d[] lastTrajectory = null;
    private Vec3d lastLanding = null;
    private double lastSpreadRadius = 0;

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
        addSetting(showLanding);
        addSetting(tipOnly);
        addSetting(silentRotation);

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled() || !showLanding.getValue() || lastLanding == null) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;
            var consumers = context.consumers();
            if (consumers == null) return;

            Vec3d camPos = context.camera().getPos();
            VertexConsumer buf = consumers.getBuffer(RenderLayer.getLines());

            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            if (!tipOnly.getValue() && lastTrajectory != null) {
                MatrixStack.Entry entry = matrices.peek();
                for (int i = 0; i < lastTrajectory.length - 1; i++) {
                    Vec3d a = lastTrajectory[i];
                    Vec3d b = lastTrajectory[i + 1];
                    line(entry, buf, (float) a.x, (float) a.y, (float) a.z,
                            (float) b.x, (float) b.y, (float) b.z, 1f, 1f, 0.2f, 1f);
                }
            }

            float r = (float) Math.max(0.05, lastSpreadRadius);
            matrices.translate(lastLanding.x, lastLanding.y, lastLanding.z);
            drawOutlinedBox(matrices, buf, -r, -r, -r, r, r, r, 0.2f, 1f, 0.2f, 1f);

            matrices.pop();
        });
    }

    private static void drawOutlinedBox(MatrixStack matrices, VertexConsumer buf,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        line(entry, buf, x1,y1,z1, x2,y1,z1, r,g,b,a);
        line(entry, buf, x2,y1,z1, x2,y1,z2, r,g,b,a);
        line(entry, buf, x2,y1,z2, x1,y1,z2, r,g,b,a);
        line(entry, buf, x1,y1,z2, x1,y1,z1, r,g,b,a);
        line(entry, buf, x1,y2,z1, x2,y2,z1, r,g,b,a);
        line(entry, buf, x2,y2,z1, x2,y2,z2, r,g,b,a);
        line(entry, buf, x2,y2,z2, x1,y2,z2, r,g,b,a);
        line(entry, buf, x1,y2,z2, x1,y2,z1, r,g,b,a);
        line(entry, buf, x1,y1,z1, x1,y2,z1, r,g,b,a);
        line(entry, buf, x2,y1,z1, x2,y2,z1, r,g,b,a);
        line(entry, buf, x2,y1,z2, x2,y2,z2, r,g,b,a);
        line(entry, buf, x1,y1,z2, x1,y2,z2, r,g,b,a);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer buf,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) len = 1;
        float nx = dx/len, ny = dy/len, nz = dz/len;
        buf.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, a).normal(entry, nx, ny, nz);
        buf.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, a).normal(entry, nx, ny, nz);
    }

    @Override
    protected void onDisable() {
        lastLanding = null;
        lastTrajectory = null;
        RotationManager.bowRequest = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Only assist while actually drawing a bow.
        if (!mc.player.isUsingItem() || !(mc.player.getActiveItem().getItem() instanceof BowItem)) {
            lastLanding = null;
            lastTrajectory = null;
            if (RotationManager.bowRequest) RotationManager.bowRequest = false;
            return;
        }

        // Arrow launch speed scales with how far the bow is drawn (max 3.0 blocks/tick).
        float charge = BowItem.getPullProgress(mc.player.getItemUseTime());
        if (charge <= 0f) charge = 0.1f;
        double v = charge * 3.0;

        // Tip Only is a pure preview: don't touch aim, just show where the current shot would land.
        boolean aiming = false;
        if (!tipOnly.getValue()) {
            LivingEntity target = findTarget(mc);
            if (target != null) {
                aimAt(mc, target, v);
                aiming = true;
            }
        }
        RotationManager.bowRequest = aiming && silentRotation.getValue();

        if (showLanding.getValue()) {
            updateLandingPrediction(mc, v, charge);
        } else {
            lastLanding = null;
            lastTrajectory = null;
        }
    }

    /** Simulates the arrow's flight from the player's current look direction to predict where it lands. */
    private void updateLandingPrediction(MinecraftClient mc, double v, float charge) {
        Vec3d start = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        // In Tip Only mode use the rotation the server has already been sent (last tick's,
        // before any local aim assist this tick) rather than the client's just-updated look.
        float yaw = tipOnly.getValue() ? mc.player.prevYaw : mc.player.getYaw();
        float pitch = tipOnly.getValue() ? mc.player.prevPitch : mc.player.getPitch();

        Vec3d[] path = simulateTrajectory(mc, start, yaw, pitch, v);
        lastTrajectory = path;
        Vec3d landing = path[path.length - 1];
        lastLanding = landing;

        // Vanilla bow inaccuracy grows the less the shot is charged; project that
        // divergence out to the landing distance to approximate an impact radius.
        double spreadDeg = BASE_SPREAD_DEG * (1.0 + (1.0 - charge) * 2.0);
        double dist = start.distanceTo(landing);
        lastSpreadRadius = dist * Math.tan(Math.toRadians(spreadDeg));
    }

    /** Steps a no-drag ballistic path (matching {@link #ballisticPitch}) until it hits a block. */
    private Vec3d[] simulateTrajectory(MinecraftClient mc, Vec3d start, float yaw, float pitch, double v) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        Vec3d velocity = new Vec3d(dirX, dirY, dirZ).multiply(v);
        java.util.List<Vec3d> points = new java.util.ArrayList<>();
        Vec3d pos = start;
        points.add(pos);

        int maxSteps = 400;
        double maxDistSq = (double) range.getValue() * range.getValue() * 4;
        for (int i = 0; i < maxSteps; i++) {
            Vec3d next = pos.add(velocity);
            HitResult hit = mc.world.raycast(new RaycastContext(
                    pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getPos());
                return points.toArray(new Vec3d[0]);
            }
            pos = next;
            points.add(pos);
            velocity = velocity.add(0, -GRAVITY, 0);
            if (start.squaredDistanceTo(pos) > maxDistSq || pos.y < mc.world.getBottomY() - 5) break;
        }
        return points.toArray(new Vec3d[0]);
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
