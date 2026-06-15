package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.utils.IViewDistanceUnclamped;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin implements IViewDistanceUnclamped {
    // Generic field erases to Object at bytecode level — Shadow matches by name
    @Shadow private Object value;

    @Override
    @Unique
    public void adam$setValueUnclamped(int newValue) {
        this.value = newValue;
    }
}
