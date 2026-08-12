package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.GroundSpoofManager;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;

/**
 * A single "on ground" packet sent right as fall distance crosses the threshold doesn't stop
 * damage - the server derives fall damage from its own tracked fallDistance (built from real Y
 * deltas across every movement packet), so a late one-off packet just triggers the landing check
 * early instead of preventing it. Spoofing onGround=true on every outgoing movement packet for the
 * whole fall instead stops the server's fallDistance from ever accumulating in the first place.
 */
public class NoFall extends Module {
    private final FloatSetting threshold = new FloatSetting.Builder("Threshold")
            .defaultValue(3f).min(1f).max(20f).minSlider(1f).maxSlider(10f).build();

    public NoFall() {
        super("NoFall", Category.MOVEMENT);
        addSetting(threshold);
    }

    @Override
    protected void onDisable() {
        GroundSpoofManager.noFallRequest = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            GroundSpoofManager.noFallRequest = false;
            return;
        }
        GroundSpoofManager.noFallRequest = !mc.player.isOnGround() && mc.player.fallDistance >= threshold.getValue();
    }
}
