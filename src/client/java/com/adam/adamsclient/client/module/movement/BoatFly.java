package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Boats (and other vehicles) report their own position via a periodic VehicleMoveC2SPacket, the
 * same client-authoritative pattern normal player movement uses - so the same disable-gravity +
 * drive-vertical-velocity-directly approach Fly uses for the player works here on the vehicle
 * you're riding instead.
 */
public class BoatFly extends Module {
    private final FloatSetting speed = new FloatSetting.Builder("Speed")
            .defaultValue(0.5f).min(0.05f).max(3f).minSlider(0.1f).maxSlider(2f).build();

    public BoatFly() {
        super("BoatFly", Category.MOVEMENT);
        addSetting(speed);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof AbstractBoatEntity boat)) return;

        boat.setNoGravity(true);

        float vy;
        if (mc.options.jumpKey.isPressed()) {
            vy = speed.getValue();
        } else if (mc.options.sneakKey.isPressed()) {
            vy = -speed.getValue();
        } else {
            vy = 0f;
        }
        Vec3d v = boat.getVelocity();
        boat.setVelocity(v.x, vy, v.z);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Entity vehicle = mc.player.getVehicle();
        if (vehicle instanceof AbstractBoatEntity boat) {
            boat.setNoGravity(false);
        }
    }
}
