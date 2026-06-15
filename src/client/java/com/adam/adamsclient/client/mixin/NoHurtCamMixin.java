package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.visual.NoHurtCam;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class NoHurtCamMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTilt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoHurtCam.INSTANCE != null && NoHurtCam.INSTANCE.isEnabled()) ci.cancel();
    }
}
