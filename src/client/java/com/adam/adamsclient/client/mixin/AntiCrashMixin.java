package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.utils.AntiCrash;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
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

    /**
     * Known crash vector: a malicious explosion center/knockback with NaN, Infinity, or an
     * absurd magnitude propagates into player velocity / world math and crashes the client.
     */
    @Inject(method = "onExplosion", at = @At("HEAD"), cancellable = true)
    private void onExplosion(ExplosionS2CPacket packet, CallbackInfo ci) {
        AntiCrash ac = AntiCrash.INSTANCE;
        if (ac == null || !ac.isEnabled() || !ac.isExplosionGuardEnabled()) return;

        Vec3d center = packet.center();
        if (!isFinite(center)) { ci.cancel(); return; }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getPos().distanceTo(center) > ac.getMaxExplosionDistance()) {
            ci.cancel();
            return;
        }

        if (packet.playerKnockback().isPresent()) {
            Vec3d kb = packet.playerKnockback().get();
            if (!isFinite(kb) || kb.length() > ac.getMaxKnockback()) ci.cancel();
        }
    }

    /** Known CVE-class bug: recursively self-referencing text components blow up when flattened to a string. */
    @Inject(method = "onGameMessage", at = @At("HEAD"), cancellable = true)
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (textExceeds(packet.content())) ci.cancel();
    }

    @Inject(method = "onOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onOverlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        if (textExceeds(packet.text())) ci.cancel();
    }

    @Inject(method = "onTitle", at = @At("HEAD"), cancellable = true)
    private void onTitle(TitleS2CPacket packet, CallbackInfo ci) {
        if (textExceeds(packet.text())) ci.cancel();
    }

    @Inject(method = "onSubtitle", at = @At("HEAD"), cancellable = true)
    private void onSubtitle(SubtitleS2CPacket packet, CallbackInfo ci) {
        if (textExceeds(packet.text())) ci.cancel();
    }

    private static boolean isFinite(Vec3d v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private boolean textExceeds(Text text) {
        AntiCrash ac = AntiCrash.INSTANCE;
        if (ac == null || !ac.isEnabled() || !ac.isTextGuardEnabled()) return false;
        return textExceeds(text, new int[]{0}, ac.getMaxTextNodes());
    }

    /**
     * Walks siblings up to a hard node budget and bails the instant it's hit, so a
     * self-referencing/exponential component can never make us do more than `max` work.
     */
    private static boolean textExceeds(Text text, int[] visited, int max) {
        if (++visited[0] > max) return true;
        for (Text sibling : text.getSiblings()) {
            if (textExceeds(sibling, visited, max)) return true;
        }
        return false;
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
