package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.module.visual.XRay;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class XRayBlockStateMixin {

    /**
     * Face-culling hook: the chunk builder calls neighbor.isSideInvisible(thisState, facing)
     * to decide whether neighbor occludes thisState's face.
     * Returning true for non-ore blocks means they never occlude adjacent faces,
     * so ore blocks stay fully visible through any surrounding block.
     */
    @Inject(method = "isSideInvisible", at = @At("HEAD"), cancellable = true)
    private void onIsSideInvisible(BlockState adjacentState, Direction facing,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!XRay.isActive()) return;
        if (!((Object) this instanceof BlockState state)) return;
        if (!XRay.isOreBlock(state.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
