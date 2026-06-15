package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.combat.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class VelocityMixin {
    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void onVelocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        if (Velocity.INSTANCE == null || !Velocity.INSTANCE.isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || packet.getEntityId() != mc.player.getId()) return;

        float h = Velocity.INSTANCE.getHorizontal();
        float v = Velocity.INSTANCE.getVertical();

        mc.player.setVelocity(
                (packet.getVelocityX() / 8000.0) * h,
                (packet.getVelocityY() / 8000.0) * v,
                (packet.getVelocityZ() / 8000.0) * h
        );
        ci.cancel();
    }
}
