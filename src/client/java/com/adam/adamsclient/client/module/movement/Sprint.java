package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;

public class Sprint extends Module {
    private final BoolSetting omniSprint = new BoolSetting("Omni Sprint", false);

    public Sprint() {
        super("Sprint", Category.MOVEMENT);
        addSetting(omniSprint);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        boolean moving = mc.player.input != null && (
                mc.player.input.playerInput.forward() ||
                mc.player.input.playerInput.backward() ||
                mc.player.input.playerInput.left() ||
                mc.player.input.playerInput.right()
        );
        if (omniSprint.getValue() || moving) {
            mc.player.setSprinting(true);
        }
    }
}
