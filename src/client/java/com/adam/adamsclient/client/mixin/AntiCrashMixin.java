package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.utils.AntiCrash;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class AntiCrashMixin {

    @Inject(method = "onParticle", at = @At("HEAD"), cancellable = true)
    private void onParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        AntiCrash ac = AntiCrash.INSTANCE;
        if (ac == null || !ac.isEnabled() || !ac.isParticleGuardEnabled()) return;
        if (packet.getCount() > ac.getMaxParticles()) ci.cancel();
    }

    @Inject(method = "onBlockEntityUpdate", at = @At("HEAD"), cancellable = true)
    private void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
        AntiCrash ac = AntiCrash.INSTANCE;
        if (ac == null || !ac.isEnabled() || !ac.isNbtGuardEnabled()) return;
        NbtCompound nbt = packet.getNbt();
        if (nbt != null && depthExceeds(nbt, ac.getMaxNbtDepth())) ci.cancel();
    }

    /** True if the element's nesting depth exceeds max, without recursing past max (avoids a stack overflow of our own). */
    private static boolean depthExceeds(NbtElement element, int max) {
        return depthExceeds(element, 0, max);
    }

    private static boolean depthExceeds(NbtElement element, int depth, int max) {
        if (depth > max) return true;
        if (element instanceof NbtCompound compound) {
            for (String key : compound.getKeys()) {
                if (depthExceeds(compound.get(key), depth + 1, max)) return true;
            }
        } else if (element instanceof NbtList list) {
            for (int i = 0; i < list.size(); i++) {
                if (depthExceeds(list.get(i), depth + 1, max)) return true;
            }
        }
        return false;
    }
}
