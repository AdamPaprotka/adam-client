package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class HitBox extends Module {
    private final FloatSetting expand = new FloatSetting.Builder("Expand")
            .defaultValue(0.3f).min(0f).max(2f).minSlider(0f).maxSlider(1f).build();

    private final BoolSetting silentAim = new BoolSetting("Silent Aim", false);
    private final FloatSetting silentAimRange = new FloatSetting.Builder("Silent Aim Range")
            .defaultValue(4.5f).min(1f).max(10f).minSlider(1f).maxSlider(6f).build();

    public HitBox() {
        super("HitBox", Category.COMBAT);
        addSetting(expand);
        addSetting(silentAim);
        addSetting(silentAimRange);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        float e = expand.getValue();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (entity instanceof LivingEntity living && living.getHealth() > 0) {
                double halfWidth = entity.getWidth() / 2.0;
                double height = entity.getHeight();
                double x = entity.getX(), y = entity.getY(), z = entity.getZ();
                Box naturalBox = new Box(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
                entity.setBoundingBox(naturalBox.expand(e));
            }
        }

        if (silentAim.getValue() && mc.options.attackKey.wasPressed()) {
            LivingEntity target = findTarget(mc, silentAimRange.getValue());
            if (target != null) {
                float savedYaw = mc.player.getYaw();
                float savedPitch = mc.player.getPitch();

                rotateTo(mc, target);
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);

                mc.player.setYaw(savedYaw);
                mc.player.setPitch(savedPitch);
            }
        }
    }

    /** Nearest living entity within range whose (expanded) bounding box the crosshair could plausibly be near. */
    private LivingEntity findTarget(MinecraftClient mc, float range) {
        LivingEntity best = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity living) || living.getHealth() <= 0) continue;

            double dist = mc.player.squaredDistanceTo(entity);
            if (dist > range * range || dist >= closestDist) continue;

            closestDist = dist;
            best = living;
        }
        return best;
    }

    private void rotateTo(MinecraftClient mc, Entity target) {
        double dx = target.getX() - mc.player.getX();
        double dy = target.getEyeY() - mc.player.getEyeY();
        double dz = target.getZ() - mc.player.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, hDist));

        mc.player.setYaw(targetYaw);
        mc.player.setPitch(MathHelper.clamp(targetPitch, -90f, 90f));
    }
}
