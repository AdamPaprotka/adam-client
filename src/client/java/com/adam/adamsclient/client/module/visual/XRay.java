package com.adam.adamsclient.client.module.visual;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;

import java.util.Set;

public class XRay extends Module {
    public static XRay INSTANCE;

    // Blocks to keep visible; everything else becomes invisible
    public static final Set<Block> ORES = Set.of(
            Blocks.DIAMOND_ORE,         Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.IRON_ORE,            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE,            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.COAL_ORE,            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.EMERALD_ORE,         Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE,           Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE,        Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.COPPER_ORE,          Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_GOLD_ORE,     Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    // When enabled, only show ores that have at least one air-adjacent face.
    // This matches what most server anti-xray (Paper mode 2) reveals anyway,
    // making the feature harder to fingerprint server-side.
    private final BoolSetting antiAntiXray = new BoolSetting("Anti Anti-Xray", false);

    public XRay() {
        super("XRay", Category.VISUAL);
        INSTANCE = this;
        addSetting(antiAntiXray);
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static boolean isOreBlock(Block block) {
        return ORES.contains(block);
    }

    public boolean isAntiAntiXrayEnabled() {
        return antiAntiXray.getValue();
    }

    @Override
    protected void onEnable()  { reloadTerrain(); }

    @Override
    protected void onDisable() { reloadTerrain(); }

    private void reloadTerrain() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }
}
