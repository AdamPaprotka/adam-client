package com.adam.adamsclient.client.module.world;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Scaffold extends Module {
    private final BoolSetting tower = new BoolSetting("Tower", false);

    public Scaffold() {
        super("Scaffold", Category.WORLD);
        addSetting(tower);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (tower.getValue() && mc.options.jumpKey.isPressed()) {
            placeTower(mc);
        } else {
            placeBelow(mc);
        }
    }

    /** Places a block in the position directly below the player's feet. */
    private void placeBelow(MinecraftClient mc) {
        BlockPos pos = mc.player.getBlockPos().down();
        if (!mc.world.getBlockState(pos).isAir()) return;

        int slot = findBlockSlot(mc);
        if (slot == -1) return;
        mc.player.getInventory().selectedSlot = slot;

        tryPlace(mc, pos);
    }

    /** Continuously places upward when jump is held. */
    private void placeTower(MinecraftClient mc) {
        BlockPos pos = mc.player.getBlockPos();
        if (!mc.world.getBlockState(pos).isAir()) return;

        int slot = findBlockSlot(mc);
        if (slot == -1) return;
        mc.player.getInventory().selectedSlot = slot;

        if (tryPlace(mc, pos)) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
        }
    }

    /**
     * Finds an adjacent solid block to pos and right-clicks its face.
     * Returns true if a placement was attempted.
     */
    private boolean tryPlace(MinecraftClient mc, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.offset(dir);
            BlockState adjState = mc.world.getBlockState(adj);
            if (adjState.isAir() || !adjState.getFluidState().isEmpty()) continue;

            Direction face = dir.getOpposite();
            Vec3d hitVec = Vec3d.ofCenter(adj).add(
                    face.getOffsetX() * 0.5,
                    face.getOffsetY() * 0.5,
                    face.getOffsetZ() * 0.5);

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(hitVec, face, adj, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private int findBlockSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
