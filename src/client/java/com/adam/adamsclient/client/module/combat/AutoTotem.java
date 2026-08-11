package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    private final FloatSetting healthThreshold = new FloatSetting.Builder("Health")
            .defaultValue(8f).min(1f).max(20f).minSlider(1f).maxSlider(20f).build();

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
        addSetting(healthThreshold);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        if (mc.player.getHealth() > healthThreshold.getValue()) return;

        int offhandSlot = 45;
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        int totemSlot = -1;
        for (int i = 0; i < 45; i++) {
            if (mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }

        if (totemSlot == -1) return;

        if (totemSlot < 9) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, offhandSlot, 0, SlotActionType.PICKUP, mc.player);
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
            }
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, offhandSlot, 0, SlotActionType.PICKUP, mc.player);
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totemSlot, 0, SlotActionType.PICKUP, mc.player);
            }
        }
    }
}
