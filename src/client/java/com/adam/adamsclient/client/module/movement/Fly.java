package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.mixin.PlayerAbilitiesAccessor;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class Fly extends Module {
    private final FloatSetting speed        = new FloatSetting.Builder("Speed")
            .defaultValue(1f).min(0f).minSlider(0f).maxSlider(5f).build();
    private final BoolSetting antiAntiCheat = new BoolSetting("Anti Anti-Flight", false);

    private int phaseTimer = 0;

    public Fly() {
        super("Fly", Category.MOVEMENT);
        addSetting(speed);
        addSetting(antiAntiCheat);
    }

    @Override
    protected void onEnable() {
        phaseTimer = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.sendAbilitiesUpdate();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        mc.player.getAbilities().allowFlying = true;
        ((PlayerAbilitiesAccessor) mc.player.getAbilities()).setFlySpeed(speed.getValue() * 0.05f);

        if (antiAntiCheat.getValue()) {
            phaseTimer++;
            // Every 20 ticks briefly cut flying for one tick so the server
            // sees a non-flying state and resets its flight timer.
            if (phaseTimer >= 20) {
                mc.player.getAbilities().flying = false;
                phaseTimer = 0;
            } else {
                mc.player.getAbilities().flying = true;
            }
        } else {
            mc.player.getAbilities().flying = true;
        }
    }

    @Override
    protected void onDisable() {
        phaseTimer = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        ClientPlayerEntity player = mc.player;
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        ((PlayerAbilitiesAccessor) player.getAbilities()).setFlySpeed(0.05f);
        player.sendAbilitiesUpdate();
    }
}
