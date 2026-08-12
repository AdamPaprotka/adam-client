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
 * Reports a tiny fake hop (position up 0.0625, then back down to the real position, both
 * airborne) right before an eligible attack, so the server's own fallDistance tracker briefly
 * reads nonzero exactly when the attack packet arrives - matching Meteor Client's reference
 * implementation. A single onGround=false flag with no position change (the old approach) never
 * touches fallDistance at all, so it alone can't satisfy vanilla's real crit condition.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class CriticalsMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void beforeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        Criticals c = Criticals.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || mc.getNetworkHandler() == null) return;
        if (!c.shouldForceCrit(mc.player)) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        var connection = mc.getNetworkHandler().getConnection();
        connection.send(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, false));
        connection.send(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
    }
}
