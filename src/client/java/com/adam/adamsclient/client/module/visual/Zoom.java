package com.adam.adamsclient.client.module.visual;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;

public class Zoom extends Module {
    private final FloatSetting fov = new FloatSetting.Builder("FOV")
            .defaultValue(20f).min(1f).max(90f).minSlider(5f).maxSlider(60f).build();

    private int originalFov = 70;

    public Zoom() {
        super("Zoom", Category.VISUAL);
        addSetting(fov);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        originalFov = mc.options.getFov().getValue();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        mc.options.getFov().setValue((int) (float) fov.getValue());
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        mc.options.getFov().setValue(originalFov);
    }
}
