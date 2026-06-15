package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class SprintMixin {
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void preventSprintReset(boolean sprinting, CallbackInfo ci) {
        if (!sprinting
                && (Object) this == MinecraftClient.getInstance().player
                && ModuleManager.isModuleEnabled("Sprint")) {
            ci.cancel();
        }
    }
}
