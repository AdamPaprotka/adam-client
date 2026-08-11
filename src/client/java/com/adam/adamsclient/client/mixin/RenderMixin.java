package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.gui.GuiManager;
import com.adam.adamsclient.client.module.ModuleManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class RenderMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(CallbackInfo ci) {
        GuiManager.render();
    }
}