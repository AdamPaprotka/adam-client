package com.adam.adamsclient.client.mixin;

import net.minecraft.entity.player.PlayerAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerAbilities.class)
public interface PlayerAbilitiesAccessor {
    @Accessor("flySpeed")
    float getFlySpeed();

    @Accessor("flySpeed")
    void setFlySpeed(float value);
}
