package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Predicts incoming melee attacks from nearby players and holds the use key so we block
 * with whatever's capable of it — a shield (either hand), or a sword in versions where
 * weapons can block. Just presses "use"; vanilla decides what actually happens with it.
 */
public class AutoShield extends Module {
    private final FloatSetting range = new FloatSetting.Builder("Range")
            .defaultValue(4.5f).min(1f).max(8f).minSlider(1f).maxSlider(6f).build();
    private final FloatSetting fov = new FloatSetting.Builder("Attacker FOV")
            .defaultValue(60f).min(10f).max(180f).minSlider(20f).maxSlider(120f).build();
    private final BoolSetting requireFacing = new BoolSetting("Require Facing", true);
    private final BoolSetting reactToSwing = new BoolSetting("React To Swing", true);
    private final FloatSetting holdTicks = new FloatSetting.Builder("Hold Ticks")
            .defaultValue(6f).min(1f).max(40f).minSlider(1f).maxSlider(20f).build();

    private int blockTicksLeft = 0;

    public AutoShield() {
        super("AutoShield", Category.COMBAT);
        addSetting(range);
        addSetting(fov);
        addSetting(requireFacing);
        addSetting(reactToSwing);
        addSetting(holdTicks);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        boolean threat = findThreat(mc);
        if (threat) {
            blockTicksLeft = Math.max(1, holdTicks.getValue().intValue());
        } else if (blockTicksLeft > 0) {
            blockTicksLeft--;
        }

        mc.options.useKey.setPressed(blockTicksLeft > 0);
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.useKey.setPressed(false);
        blockTicksLeft = 0;
    }

    private boolean findThreat(MinecraftClient mc) {
        float r = range.getValue();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity attacker) || attacker == mc.player) continue;
            if (attacker.isDead() || attacker.getHealth() <= 0) continue;

            double dist = mc.player.squaredDistanceTo(attacker);
            if (dist > r * r) continue;

            if (requireFacing.getValue() && !isFacingUs(mc, attacker)) continue;
            if (reactToSwing.getValue() && !attacker.handSwinging) continue;

            return true;
        }
        return false;
    }

    /** True if the attacker's look direction is within FOV of pointing at us. */
    private boolean isFacingUs(MinecraftClient mc, PlayerEntity attacker) {
        Vec3d toUs = mc.player.getEyePos().subtract(attacker.getEyePos());
        double dx = toUs.x, dz = toUs.z;
        double hDist = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawDiff = MathHelper.wrapDegrees(wantYaw - attacker.getYaw());
        return Math.abs(yawDiff) <= fov.getValue() / 2f;
    }
}
