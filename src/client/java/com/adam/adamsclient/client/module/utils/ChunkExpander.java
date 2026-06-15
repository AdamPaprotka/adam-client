package com.adam.adamsclient.client.module.utils;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;

public class ChunkExpander extends Module {
    // Memory cost grows with the SQUARE of render distance: a value of N loads
    // roughly (2N+1)^2 chunk columns. Anything past ~64 needs gigabytes of heap and
    // a disk-backed chunk cache (e.g. Bobby) to avoid OutOfMemoryError, so we cap here.
    private static final int MAX_CHUNKS = 64;

    private final FloatSetting renderDistance = new FloatSetting.Builder("Render Distance")
            .defaultValue(32f).min(2f).max((float) MAX_CHUNKS)
            .minSlider(2f).maxSlider((float) MAX_CHUNKS).build();

    private int originalRenderDistance = 12;
    private int lastApplied = -1;

    public ChunkExpander() {
        super("Chunk Expander", Category.UTILS);
        addSetting(renderDistance);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        originalRenderDistance = mc.options.getViewDistance().getValue();
        apply(mc);
    }

    @Override
    public void onTick() {
        int dist = renderDistance.getValue().intValue();
        if (dist != lastApplied) apply(MinecraftClient.getInstance());
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        setUnclamped(mc, originalRenderDistance);
        lastApplied = originalRenderDistance;
    }

    private void apply(MinecraftClient mc) {
        int dist = renderDistance.getValue().intValue();
        setUnclamped(mc, dist);
        lastApplied = dist;
    }

    private static void setUnclamped(MinecraftClient mc, int dist) {
        // Hard safety clamp so a stale/huge saved config can never OOM the game.
        dist = Math.max(2, Math.min(dist, MAX_CHUNKS));
        ((IViewDistanceUnclamped) (Object) mc.options.getViewDistance()).adam$setValueUnclamped(dist);
        if (mc.worldRenderer != null) mc.worldRenderer.scheduleTerrainUpdate();
    }
}
