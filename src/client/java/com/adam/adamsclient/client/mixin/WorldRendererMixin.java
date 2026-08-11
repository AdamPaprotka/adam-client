package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.ModuleManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
                          net.minecraft.client.render.Camera camera, net.minecraft.client.render.GameRenderer gameRenderer,
                          org.joml.Matrix4f positionMatrix, org.joml.Matrix4f projectionMatrix, CallbackInfo ci) {
        ModuleManager.onRender(new MatrixStack(), tickCounter.getTickDelta(false));
    }
}
