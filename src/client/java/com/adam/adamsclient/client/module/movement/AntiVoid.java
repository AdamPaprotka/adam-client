package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/** Teleports back to the last position you were actually standing on solid ground, right before
 * falling below the configured Y threshold, instead of dying to a void/gap fall. */
public class AntiVoid extends Module {
    private final FloatSetting threshold = new FloatSetting.Builder("Y Threshold")
            .defaultValue(-56f).min(-500f).max(0f).minSlider(-256f).maxSlider(0f).build();

    private Vec3d lastSafePos = null;

    public AntiVoid() {
        super("AntiVoid", Category.MOVEMENT);
        addSetting(threshold);
    }

    @Override
    protected void onDisable() {
        lastSafePos = null;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround()) {
            lastSafePos = mc.player.getPos();
            return;
        }

        if (mc.player.getY() >= threshold.getValue()) return;
        if (lastSafePos == null) return;

        mc.player.setVelocity(0, 0, 0);
        mc.player.setPosition(lastSafePos.x, lastSafePos.y, lastSafePos.z);
        mc.player.fallDistance = 0f;
    }
}
