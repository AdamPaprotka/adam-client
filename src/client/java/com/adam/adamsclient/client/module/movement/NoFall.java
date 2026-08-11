package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * The client-side damage suppression (see NoFallMixin) only stops the local hurt animation —
 * vanilla servers track fall distance themselves and apply damage regardless of what the client
 * thinks happened. To actually stop damage on a real server we spoof an "on ground" packet once
 * fall distance passes the threshold, which resets the server's own fall-distance tracker early.
 */
public class NoFall extends Module {
    private final FloatSetting threshold = new FloatSetting.Builder("Threshold")
            .defaultValue(3f).min(1f).max(20f).minSlider(1f).maxSlider(10f).build();

    private boolean sentReset = false;

    public NoFall() {
        super("NoFall", Category.MOVEMENT);
        addSetting(threshold);
    }

    @Override
    protected void onDisable() {
        sentReset = false;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (mc.player.isOnGround() || mc.player.fallDistance < threshold.getValue()) {
            sentReset = false;
            return;
        }

        if (sentReset) return;
        sentReset = true;
        mc.getNetworkHandler().getConnection().send(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
    }
}
