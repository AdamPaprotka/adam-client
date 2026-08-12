package com.adam.adamsclient.client;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * Servers derive fall damage (and similar ground-state checks) from their own tracked state,
 * built from the onGround flag reported in each movement packet - not from a one-off packet sent
 * later. Any module that needs the server to believe it's grounded for a stretch of ticks (NoFall
 * while actually falling, Fly while airborne) sets its own request flag here; while any request is
 * active, every outgoing movement packet gets onGround forced to true.
 */
public final class GroundSpoofManager {
    private GroundSpoofManager() {}

    public static volatile boolean noFallRequest = false;
    public static volatile boolean flyRequest = false;
    /** Reentrancy guard for the resend inside the mixin. */
    public static volatile boolean rewriting = false;

    public static boolean isActive() {
        return noFallRequest || flyRequest;
    }

    /** Returns a copy of the packet with onGround forced true, or null if the variant carries no ground state. */
    public static Packet<?> forceGround(PlayerMoveC2SPacket packet) {
        if (packet instanceof PlayerMoveC2SPacket.Full f) {
            return new PlayerMoveC2SPacket.Full(f.getX(0), f.getY(0), f.getZ(0), f.getYaw(0), f.getPitch(0), true, f.horizontalCollision());
        }
        if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround p) {
            return new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(0), p.getY(0), p.getZ(0), true, p.horizontalCollision());
        }
        if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround l) {
            return new PlayerMoveC2SPacket.LookAndOnGround(l.getYaw(0), l.getPitch(0), true, l.horizontalCollision());
        }
        if (packet instanceof PlayerMoveC2SPacket.OnGroundOnly o) {
            return new PlayerMoveC2SPacket.OnGroundOnly(true, o.horizontalCollision());
        }
        return null;
    }
}
