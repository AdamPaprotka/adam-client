package com.adam.adamsclient.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

/**
 * Tracks what the player's rotation would be from mouse input alone, independent of any
 * module snapping mc.player's real yaw/pitch for aiming. While {@link #silent} is on, outgoing
 * movement packets get their yaw/pitch swapped for this "real" rotation instead of the bot's,
 * so the client can look wherever it wants locally without the server ever seeing it.
 */
public final class RotationManager {
    private RotationManager() {}

    public static volatile float realYaw = 0f;
    public static volatile float realPitch = 0f;
    /** True while some module wants its rotation hidden from the server. */
    public static volatile boolean silent = false;
    /** Reentrancy guard for the resend inside the mixin. */
    public static volatile boolean rewriting = false;

    private static boolean initialized = false;

    /** Called from the Mouse mixin with the same raw deltas vanilla feeds into player look. */
    public static void trackMouseDelta(double dx, double dy) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (!initialized) {
            realYaw = mc.player.getYaw();
            realPitch = mc.player.getPitch();
            initialized = true;
        }

        double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double scale = sensitivity * sensitivity * sensitivity * 8.0 * 0.15;

        realYaw += (float) (dx * scale);
        realPitch = MathHelper.clamp((float) (realPitch + dy * scale * (mc.options.getInvertYMouse().getValue() ? -1 : 1)), -90f, 90f);
    }

    /** Called every client tick; resyncs to the player's actual rotation whenever nothing is hiding it. */
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!silent) {
            realYaw = mc.player.getYaw();
            realPitch = mc.player.getPitch();
        }
    }

    /** Returns a copy of the packet with its yaw/pitch swapped for the real rotation, or null if not applicable. */
    public static Packet<?> rewrite(PlayerMoveC2SPacket packet) {
        if (packet instanceof PlayerMoveC2SPacket.Full f) {
            return new PlayerMoveC2SPacket.Full(
                    f.getX(0), f.getY(0), f.getZ(0), realYaw, realPitch, f.isOnGround(), f.horizontalCollision());
        }
        if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround l) {
            return new PlayerMoveC2SPacket.LookAndOnGround(realYaw, realPitch, l.isOnGround(), l.horizontalCollision());
        }
        return null;
    }
}
