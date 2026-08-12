package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Vanilla's real crit condition is "fallDistance > 0 AND not on ground" (plus the other gates
 * below) - not just the onGround flag alone. fallDistance is the SERVER's own tracked value,
 * incremented only when it observes an actual downward Y movement while airborne. A single
 * onGround=false packet with an unchanged position never touches that value, so it alone can't
 * satisfy the condition. CriticalsMixin instead reports a tiny fake hop (position up 0.0625,
 * then immediately back down to the real position, both airborne) right before the attack -
 * matching Meteor Client's reference implementation - so the server's own fallDistance briefly
 * reads as nonzero exactly when the attack packet arrives.
 */
public class Criticals extends Module {
    public static Criticals INSTANCE;

    /** Ticks left to drop any outgoing packet claiming onGround=true, so the fake hop's
     * fallDistance bump doesn't get reset back to 0 by the next natural (truthful) packet
     * before the attack is fully processed server-side. */
    public static volatile int suppressGroundTrueTicks = 0;

    public Criticals() {
        super("Criticals", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        suppressGroundTrueTicks = 0;
    }

    @Override
    public void onTick() {
        if (suppressGroundTrueTicks > 0) suppressGroundTrueTicks--;
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
