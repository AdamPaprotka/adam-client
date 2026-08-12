package com.adam.adamsclient.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("renderTickCounter")
    @Mutable
    void setRenderTickCounter(RenderTickCounter.Dynamic tickCounter);

    /** The exact private method vanilla's own left-click handler calls - operates on mc.crosshairTarget. */
    @Invoker("doAttack")
    boolean invokeDoAttack();

    /** Gates doItemUse() to once every 4 ticks while the use key is held - the real block-place rate limit. */
    @Accessor("itemUseCooldown")
    @Mutable
    void setItemUseCooldown(int value);
}
