package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.player.FakeLag;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class FakeLagMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (FakeLag.flushing) return;
        if (!FakeLag.isActive()) return;
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        FakeLag.INSTANCE.enqueue((ClientConnection) (Object) this, packet);
        ci.cancel();
    }
}
