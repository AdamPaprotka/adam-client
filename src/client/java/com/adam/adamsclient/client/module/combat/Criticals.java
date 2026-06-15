package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import net.minecraft.client.MinecraftClient;

public class Criticals extends Module {
    public Criticals() {
        super("Criticals", Category.COMBAT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.isOnGround()) return;
        // Critical hits happen when the player is falling — handled via mixin
    }
}
