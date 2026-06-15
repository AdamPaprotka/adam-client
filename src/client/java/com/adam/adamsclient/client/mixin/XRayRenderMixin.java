package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.visual.XRay;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public class XRayRenderMixin {
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void onRenderBlock(BlockState state, BlockPos pos, BlockRenderView world,
                               MatrixStack matrices, VertexConsumer vertices,
                               boolean cull, Random random, CallbackInfo ci) {
        if (!XRay.isActive()) return;
        if (XRay.isOreBlock(state.getBlock())) return;

        if (XRay.INSTANCE.isAntiAntiXrayEnabled()) {
            for (Direction dir : Direction.values()) {
                if (world.getBlockState(pos.offset(dir)).isAir()) return;
            }
        }

        ci.cancel();
    }
}
