package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Vanilla awards critical damage when the attacker is server-side airborne (falling, not on
 * ground/climbing/swimming/vehicle/sprinting/blinded) at the moment the attack is processed - it's
 * the server's own tracked ground state that matters, not anything purely local. CriticalsMixin
 * spoofs a one-off "not on ground" packet immediately before an eligible attack (and restores it
 * right after) so a standing-still hit still gets the server to register a crit, without needing
 * to keep spoofing state across ticks the way NoFall/Fly do.
 */
public class Criticals extends Module {
    public static Criticals INSTANCE;

    public Criticals() {
        super("Criticals", Category.COMBAT);
        INSTANCE = this;
    }

    public boolean shouldForceCrit(ClientPlayerEntity player) {
        return isEnabled()
                && player.isOnGround()
                && !player.isSprinting()
                && !player.isTouchingWater()
                && !player.isClimbing()
                && !player.hasVehicle()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS);
    }
}
