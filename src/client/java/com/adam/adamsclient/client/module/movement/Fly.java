package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.GroundSpoofManager;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * The old implementation just flipped the vanilla "flying" ability flag - that only actually
 * moves you when the server itself has granted creative/allow-flying, so on a normal survival
 * server it silently did nothing. This instead disables local gravity and drives vertical
 * velocity directly off jump/sneak, so movement rides on the same trusted position packets normal
 * walking already uses, with Ground Spoof (see GroundSpoofManager) hiding the sustained airborne
 * state from the server the same way NoFall hides a fall.
 */
public class Fly extends Module {
    private final FloatSetting speed = new FloatSetting.Builder("Speed")
            .defaultValue(0.5f).min(0.05f).max(3f).minSlider(0.1f).maxSlider(2f).build();
    private final BoolSetting groundSpoof = new BoolSetting("Ground Spoof", true);

    public Fly() {
        super("Fly", Category.MOVEMENT);
        addSetting(speed);
        addSetting(groundSpoof);
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.setNoGravity(true);
        mc.player.fallDistance = 0f;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            GroundSpoofManager.flyRequest = false;
            return;
        }

        player.setNoGravity(true);
        player.fallDistance = 0f;

        float vy;
        if (mc.options.jumpKey.isPressed()) {
            vy = speed.getValue();
        } else if (mc.options.sneakKey.isPressed()) {
            vy = -speed.getValue();
        } else {
            vy = 0f;
        }
        Vec3d v = player.getVelocity();
        player.setVelocity(v.x, vy, v.z);

        GroundSpoofManager.flyRequest = groundSpoof.getValue();
    }

    @Override
    protected void onDisable() {
        GroundSpoofManager.flyRequest = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.setNoGravity(false);
    }
}
