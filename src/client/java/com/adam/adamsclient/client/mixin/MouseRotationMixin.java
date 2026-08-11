package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.RotationManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseRotationMixin {
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void onUpdateMouse(double time, CallbackInfo ci) {
        RotationManager.trackMouseDelta(cursorDeltaX, cursorDeltaY);
    }
}
