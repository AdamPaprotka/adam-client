package com.adam.adamsclient.client.module.world;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.EnumSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Nuker extends Module {
    public enum Mode { CLOSEST, FARTHEST, HARDNESS }

    private final FloatSetting  range           = new FloatSetting.Builder("Range")
            .defaultValue(4f).min(1f).max(6f).minSlider(1f).maxSlider(6f).build();
    private final FloatSetting  bps             = new FloatSetting.Builder("BPS")
            .defaultValue(20f).min(0.1f).max(1000f).minSlider(1f).maxSlider(100f).build();
    private final EnumSetting<Mode> mode        = new EnumSetting<>("Mode", Mode.CLOSEST);
    private final BoolSetting   skipUnbreakable = new BoolSetting("Skip Unbreakable", true);

    private long lastBreak = 0;

    public Nuker() {
        super("Nuker", Category.WORLD);
        addSetting(range);
        addSetting(bps);
        addSetting(mode);
        addSetting(skipUnbreakable);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        long now = System.currentTimeMillis();
        long breakInterval = (long)(1000f / bps.getValue());
        if (now - lastBreak < breakInterval) return;

        int r = (int) Math.ceil(range.getValue());
        BlockPos origin = mc.player.getBlockPos();
        float rSq = range.getValue() * range.getValue();

        BlockPos target  = null;
        double   bestVal = mode.getValue() == Mode.FARTHEST ? -1 : Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(origin.add(-r, -r, -r), origin.add(r, r, r))) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) continue;
            if (!state.getFluidState().isEmpty()) continue;
            if (skipUnbreakable.getValue() && state.getHardness(mc.world, pos) < 0) continue;

            double dist = mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist > rSq) continue;

            double score = switch (mode.getValue()) {
                case CLOSEST   -> -dist;
                case FARTHEST  -> dist;
                case HARDNESS  -> -state.getHardness(mc.world, pos);
            };

            if (score > bestVal) { bestVal = score; target = pos.toImmutable(); }
        }

        if (target == null) return;

        mc.interactionManager.attackBlock(target, closestFace(mc.player, target));
        mc.player.swingHand(Hand.MAIN_HAND);
        lastBreak = now;
    }

    private static Direction closestFace(ClientPlayerEntity player, BlockPos pos) {
        Vec3d diff = new Vec3d(
                player.getX()   - (pos.getX() + 0.5),
                player.getEyeY()- (pos.getY() + 0.5),
                player.getZ()   - (pos.getZ() + 0.5));
        Direction best = Direction.UP;
        double bestDot = -Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            double dot = dir.getOffsetX() * diff.x
                       + dir.getOffsetY() * diff.y
                       + dir.getOffsetZ() * diff.z;
            if (dot > bestDot) { bestDot = dot; best = dir; }
        }
        return best;
    }
}
