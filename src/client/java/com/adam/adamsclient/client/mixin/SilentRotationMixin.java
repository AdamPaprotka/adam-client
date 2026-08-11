package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.RotationManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class SilentRotationMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (RotationManager.rewriting || !RotationManager.silent) return;
        if (!(packet instanceof PlayerMoveC2SPacket move)) return;

        Packet<?> rewritten = RotationManager.rewrite(move);
        if (rewritten == null) return;

        RotationManager.rewriting = true;
        ((ClientConnection) (Object) this).send(rewritten);
        RotationManager.rewriting = false;
        ci.cancel();
    }
}
