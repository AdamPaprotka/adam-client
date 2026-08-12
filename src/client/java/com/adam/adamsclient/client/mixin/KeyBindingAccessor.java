package com.adam.adamsclient.client.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the private press-count vanilla's own click handling consumes via wasPressed(). */
@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("timesPressed")
    int getTimesPressed();

    @Accessor("timesPressed")
    @Mutable
    void setTimesPressed(int value);
}
