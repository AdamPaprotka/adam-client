package com.adam.adamsclient.client.module.world;

import com.adam.adamsclient.client.mixin.MinecraftClientAccessor;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public class Timer extends Module {
    private final FloatSetting speed = new FloatSetting.Builder("Speed")
            .defaultValue(2f).min(0.1f).max(10f).minSlider(0.1f).maxSlider(5f).build();

    public Timer() {
        super("Timer", Category.WORLD);
        addSetting(speed);
    }

    private void setTicksPerSecond(float ticksPerSecond) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        ((MinecraftClientAccessor) mc).setRenderTickCounter(new RenderTickCounter.Dynamic(ticksPerSecond, 0L, v -> v));
    }

    @Override
    protected void onEnable() {
        setTicksPerSecond(20f * speed.getValue());
    }

    @Override
    public void onTick() {
        setTicksPerSecond(20f * speed.getValue());
    }

    @Override
    protected void onDisable() {
        setTicksPerSecond(20f);
    }
}
