package com.adam.adamsclient.client.module.player;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;

public class FastPlace extends Module {
    private final BoolSetting noDelay = new BoolSetting("No Delay", true);

    public FastPlace() {
        super("FastPlace", Category.PLAYER);
        addSetting(noDelay);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        if (noDelay.getValue()) {
            mc.options.useKey.setPressed(false);
            mc.options.useKey.setPressed(true);
        }
    }
}
