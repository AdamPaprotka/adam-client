package com.adam.adamsclient.client.module.player;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class ChestStealer extends Module {
    private final FloatSetting delay = new FloatSetting.Builder("Delay")
            .defaultValue(0f).min(0f).max(500f).minSlider(0f).maxSlider(200f).build();
    private final BoolSetting closeAfter = new BoolSetting("Close After", true);

    private long lastStealTime = 0;

    public ChestStealer() {
        super("ChestStealer", Category.PLAYER);
        addSetting(delay);
        addSetting(closeAfter);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;

        long now = System.currentTimeMillis();
        if (now - lastStealTime < delay.getValue()) return;

        boolean hasItems = false;
        int containerSize = handler.getRows() * 9;

        for (int i = 0; i < containerSize; i++) {
            if (!handler.getSlot(i).getStack().isEmpty()) {
                hasItems = true;
                mc.interactionManager.clickSlot(
                    handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player
                );
                lastStealTime = now;
                return;
            }
        }

        if (!hasItems && closeAfter.getValue()) {
            mc.player.closeHandledScreen();
        }
    }
}
