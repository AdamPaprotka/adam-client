package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;

public class NoSlow extends Module {
    private final FloatSetting multiplier = new FloatSetting.Builder("Multiplier")
            .defaultValue(1f).min(0.1f).max(1f).minSlider(0.1f).maxSlider(1f).build();

    public NoSlow() {
        super("NoSlow", Category.MOVEMENT);
        addSetting(multiplier);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mc.player.isUsingItem()) {
            mc.player.setMovementSpeed(
                (float)(mc.player.getMovementSpeed() * multiplier.getValue())
            );
        }
    }
}
