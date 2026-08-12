package com.adam.adamsclient.client.module.world;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * The old implementation just interacted with a face without ever rotating to it, placed while
 * sprinting, and never touched speed - each of those is exactly what a "BadPackets (Post
 * BlockPlace)" / "Scaffold (Sprint)" / "Scaffold (Speed)" check is built to catch:
 * - Post BlockPlace: the interact packet must be preceded by a look packet that actually faces
 *   the clicked face. We now genuinely rotate (real, reported rotation - not hidden) and flush an
 *   explicit look packet before the interact packet, instead of clicking a face you never turned to.
 * - Sprint: legitimate bridging can't keep sprinting through every single placement, so we drop
 *   sprint for the tick a block goes down.
 * - Speed: placing at full sprint speed produces a speed profile no legitimate bridge-and-look
 *   player matches; horizontal velocity is clamped to walking speed on placement ticks.
 */
public class Scaffold extends Module {
    private final BoolSetting tower = new BoolSetting("Tower", false);
    private final BoolSetting rotations = new BoolSetting("Rotations", true);
    private final BoolSetting safeSprint = new BoolSetting("Safe Sprint", true);
    private final FloatSetting maxSpeed = new FloatSetting.Builder("Max Speed")
            .defaultValue(0.13f).min(0.05f).max(0.3f).minSlider(0.08f).maxSlider(0.22f).build();

    public Scaffold() {
        super("Scaffold", Category.WORLD);
        addSetting(tower);
        addSetting(rotations);
        addSetting(safeSprint);
        addSetting(maxSpeed);
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

            if (safeSprint.getValue() && mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }

            if (rotations.getValue()) {
                faceHit(mc, hitVec);
            }

            clampHorizontalSpeed(mc);

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(hitVec, face, adj, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    /** Genuinely rotates to the hit point and flushes a look packet before the place packet follows. */
    private void faceHit(MinecraftClient mc, Vec3d hitVec) {
        Vec3d eye = mc.player.getEyePos();
        double dx = hitVec.x - eye.x;
        double dy = hitVec.y - eye.y;
        double dz = hitVec.z - eye.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = MathHelper.clamp((float) -Math.toDegrees(Math.atan2(dy, hDist)), -90f, 90f);

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().getConnection().send(
                    new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), false));
        }
    }

    /** Caps horizontal speed to walking pace on a placement tick, matching legit bridging speed. */
    private void clampHorizontalSpeed(MinecraftClient mc) {
        Vec3d v = mc.player.getVelocity();
        double hSpeed = Math.sqrt(v.x * v.x + v.z * v.z);
        double max = maxSpeed.getValue();
        if (hSpeed <= max) return;

        double scale = max / hSpeed;
        mc.player.setVelocity(v.x * scale, v.y, v.z * scale);
    }

    private int findBlockSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
