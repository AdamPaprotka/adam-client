package com.adam.adamsclient.client.module.player;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class AutoEat extends Module {
    private final FloatSetting threshold = new FloatSetting.Builder("Threshold")
            .defaultValue(15f).min(1f).max(20f).minSlider(1f).maxSlider(20f).build();

    public AutoEat() {
        super("AutoEat", Category.PLAYER);
        addSetting(threshold);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getHungerManager().getFoodLevel() >= (int)(float) threshold.getValue()) return;
        if (mc.player.isUsingItem()) return;

        var inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.contains(DataComponentTypes.FOOD)) {
                inv.selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                return;
            }
        }
    }
}
