package com.adam.adamsclient.client.module.player;

import com.adam.adamsclient.client.mixin.MinecraftClientAccessor;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;

/**
 * Vanilla gates right-click item use (block placement) to once every 4 ticks via
 * MinecraftClient's private itemUseCooldown field, decremented once per tick and only letting
 * doItemUse() through once it hits zero. The old implementation toggled the use key's pressed
 * state, which does nothing to that cooldown at all. Zeroing the cooldown directly, every tick
 * the key is held, is what actually lets placement happen every tick instead of every 4th.
 */
public class FastPlace extends Module {
    private final BoolSetting noDelay = new BoolSetting("No Delay", true);

    public FastPlace() {
        super("FastPlace", Category.PLAYER);
        addSetting(noDelay);
    }

    @Override
    public void onEarlyTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!noDelay.getValue() || !mc.options.useKey.isPressed()) return;

        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
    }
}
