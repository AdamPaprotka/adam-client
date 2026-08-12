package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.combat.Criticals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sends a single one-off "not on ground" packet right before an eligible attack, so the server
 * processes that specific hit as airborne (crit). No restore packet afterward - the next tick's
 * normal movement packet already reports the real (grounded) state on its own, so a manual
 * restore is a redundant extra packet, and a second unsolicited movement-type packet in the same
 * tick is exactly what a packet-order check flags.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class CriticalsMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void beforeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        Criticals c = Criticals.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || mc.getNetworkHandler() == null) return;
        if (!c.shouldForceCrit(mc.player)) return;
        mc.getNetworkHandler().getConnection().send(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
    }
}
