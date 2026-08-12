package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.utils.AntiCrash;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * A malformed packet that throws while decoding/handling normally disconnects the client with a
 * generic "Internal Exception" message (see ClientConnection#exceptionCaught) - this is NOT the
 * same thing as a real server kick, which arrives as a DisconnectS2CPacket through a different
 * path entirely and is untouched here. Genuine IOExceptions (actual socket/connection loss) are
 * left alone so the client still disconnects when the connection is really gone.
 */
@Mixin(ClientConnection.class)
public class AntiKickMixin {

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void onExceptionCaught(ChannelHandlerContext ctx, Throwable throwable, CallbackInfo ci) {
        AntiCrash ac = AntiCrash.INSTANCE;
        if (ac == null || !ac.isEnabled() || !ac.isAntiKickEnabled()) return;
        if (throwable instanceof IOException) return;
        ci.cancel();
    }
}
