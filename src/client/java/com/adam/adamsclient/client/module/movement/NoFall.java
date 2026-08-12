package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.GroundSpoofManager;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Triggers on downward velocity (matching Meteor Client's reference implementation) rather than
 * accumulated fallDistance crossing a threshold, and runs on the early tick hook so the spoofed
 * ground state is already reflected in the SAME tick's normal movement packet instead of lagging
 * a tick behind by updating after it was already sent.
 */
public class NoFall extends Module {
    private final FloatSetting velocityThreshold = new FloatSetting.Builder("Velocity Threshold")
            .defaultValue(0.5f).min(0.1f).max(2f).minSlider(0.2f).maxSlider(1f).build();

    public NoFall() {
        super("NoFall", Category.MOVEMENT);
        addSetting(velocityThreshold);
    }

    @Override
    protected void onDisable() {
        GroundSpoofManager.noFallRequest = false;
    }

    @Override
    public void onEarlyTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            GroundSpoofManager.noFallRequest = false;
            return;
        }
        GroundSpoofManager.noFallRequest = !mc.player.isOnGround()
                && mc.player.getVelocity().y < -velocityThreshold.getValue();
    }
}
