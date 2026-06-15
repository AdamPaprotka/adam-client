package com.adam.adamsclient.client.module.visual;

import com.adam.adamsclient.client.module.Module;
import net.minecraft.client.MinecraftClient;

public class Fullbright extends Module {
    private double originalGamma = 1.0;

    public Fullbright() {
        super("Fullbright", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        originalGamma = mc.options.getGamma().getValue();
        mc.options.getGamma().setValue(16.0);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;
        mc.options.getGamma().setValue(originalGamma);
    }
}
