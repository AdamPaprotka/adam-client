package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;

public class LongJump extends Module {
    private final FloatSetting boost = new FloatSetting.Builder("Boost")
            .defaultValue(1f).min(0.1f).max(5f).minSlider(0.1f).maxSlider(3f).build();
    private boolean hasJumped = false;

    public LongJump() {
        super("LongJump", Category.MOVEMENT);
        addSetting(boost);
    }

    @Override
    protected void onEnable() {
        hasJumped = false;
    }

    @Override
    protected void onDisable() {
        hasJumped = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mc.player.isOnGround()) {
            hasJumped = false;
        }

        if (mc.options.jumpKey.isPressed() && mc.player.isOnGround() && !hasJumped) {
            hasJumped = true;
            double motionY = 0.42 * boost.getValue();
            double motionX = mc.player.getVelocity().x * boost.getValue();
            double motionZ = mc.player.getVelocity().z * boost.getValue();
            mc.player.setVelocity(motionX, motionY, motionZ);
        }

        if (hasJumped && !mc.player.isOnGround() && mc.player.getVelocity().y > 0) {
            mc.player.setVelocity(
                mc.player.getVelocity().x * 1.01,
                mc.player.getVelocity().y,
                mc.player.getVelocity().z * 1.01
            );
        }
    }
}
