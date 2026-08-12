package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.GroundSpoofManager;
import com.adam.adamsclient.client.RotationManager;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Single interception point for rewriting outgoing movement packets, combining rotation-hiding
 * (BowAssist) and ground-state spoofing (NoFall/Fly) so the two concerns never fight over
 * cancelling the same packet.
 */
@Mixin(ClientConnection.class)
public class MovementPacketMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (RotationManager.rewriting || GroundSpoofManager.rewriting) return;
        if (!(packet instanceof PlayerMoveC2SPacket move)) return;

        boolean wantRotation = RotationManager.isSilent();
        boolean wantGround = GroundSpoofManager.isActive();
        if (!wantRotation && !wantGround) return;

        Packet<?> rewritten = rewrite(move, wantRotation, wantGround);
        if (rewritten == null) return;

        RotationManager.rewriting = true;
        GroundSpoofManager.rewriting = true;
        ((ClientConnection) (Object) this).send(rewritten);
        RotationManager.rewriting = false;
        GroundSpoofManager.rewriting = false;
        ci.cancel();
    }

    private static Packet<?> rewrite(PlayerMoveC2SPacket packet, boolean rotation, boolean ground) {
        if (packet instanceof PlayerMoveC2SPacket.Full f) {
            float yaw = rotation ? RotationManager.realYaw : f.getYaw(0);
            float pitch = rotation ? RotationManager.realPitch : f.getPitch(0);
            boolean onGround = ground || f.isOnGround();
            return new PlayerMoveC2SPacket.Full(f.getX(0), f.getY(0), f.getZ(0), yaw, pitch, onGround, f.horizontalCollision());
        }
        if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround l) {
            float yaw = rotation ? RotationManager.realYaw : l.getYaw(0);
            float pitch = rotation ? RotationManager.realPitch : l.getPitch(0);
            boolean onGround = ground || l.isOnGround();
            return new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround, l.horizontalCollision());
        }
        if (ground && packet instanceof PlayerMoveC2SPacket.PositionAndOnGround p) {
            return new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(0), p.getY(0), p.getZ(0), true, p.horizontalCollision());
        }
        if (ground && packet instanceof PlayerMoveC2SPacket.OnGroundOnly o) {
            return new PlayerMoveC2SPacket.OnGroundOnly(true, o.horizontalCollision());
        }
        return null;
    }
}
