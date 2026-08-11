package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Makes real, server-synced movement erratic near other players so naive KillAura
 * implementations (fixed-timer attacks, distance checked once per swing) whiff more often.
 * Can't reach into another client's code — this only changes how hard you actually are to hit.
 */
public class AntiAntiKillaura extends Module {
    private final FloatSetting detectRange = new FloatSetting.Builder("Detect Range")
            .defaultValue(6f).min(2f).max(12f).minSlider(3f).maxSlider(10f).build();

    private final BoolSetting juke = new BoolSetting("Juke", true);
    private final FloatSetting jukeStrength = new FloatSetting.Builder("Juke Strength")
            .defaultValue(0.28f).min(0.05f).max(0.6f).minSlider(0.1f).maxSlider(0.5f).build();
    private final FloatSetting jukeInterval = new FloatSetting.Builder("Juke Interval")
            .defaultValue(4f).min(1f).max(20f).minSlider(1f).maxSlider(12f).build();

    private final BoolSetting rangeBait = new BoolSetting("Range Bait", true);
    private final FloatSetting baitRange = new FloatSetting.Builder("Bait Range")
            .defaultValue(3.6f).min(2f).max(6f).minSlider(2.5f).maxSlider(5f).build();
    private final FloatSetting baitAmplitude = new FloatSetting.Builder("Bait Amplitude")
            .defaultValue(0.4f).min(0.1f).max(1.5f).minSlider(0.1f).maxSlider(1f).build();
    private final FloatSetting baitSpeed = new FloatSetting.Builder("Bait Speed")
            .defaultValue(0.3f).min(0.05f).max(1f).minSlider(0.1f).maxSlider(0.6f).build();

    private int jukeTimer = 0;
    private double jukeAngleOffset = 0;
    private int tickCounter = 0;

    public AntiAntiKillaura() {
        super("AntiAntiKillaura", Category.COMBAT);
        addSetting(detectRange);
        addSetting(juke);
        addSetting(jukeStrength);
        addSetting(jukeInterval);
        addSetting(rangeBait);
        addSetting(baitRange);
        addSetting(baitAmplitude);
        addSetting(baitSpeed);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!juke.getValue() && !rangeBait.getValue()) return;

        PlayerEntity nearest = findNearestPlayer(mc);
        if (nearest == null) return;

        tickCounter++;

        double toPlayerX = mc.player.getX() - nearest.getX();
        double toPlayerZ = mc.player.getZ() - nearest.getZ();
        double curDist = Math.sqrt(toPlayerX * toPlayerX + toPlayerZ * toPlayerZ);
        double angle = Math.atan2(toPlayerZ, toPlayerX);

        if (juke.getValue()) {
            if (--jukeTimer <= 0) {
                jukeTimer = Math.max(1, jukeInterval.getValue().intValue());
                jukeAngleOffset = (Math.random() - 0.5) * Math.PI;
            }
            angle += jukeAngleOffset;
        }

        double goalDist = rangeBait.getValue()
                ? baitRange.getValue() + Math.sin(tickCounter * baitSpeed.getValue()) * baitAmplitude.getValue()
                : curDist;

        double goalX = nearest.getX() + Math.cos(angle) * goalDist;
        double goalZ = nearest.getZ() + Math.sin(angle) * goalDist;

        double dx = goalX - mc.player.getX();
        double dz = goalZ - mc.player.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);

        Vec3d vel = mc.player.getVelocity();
        if (d > 0.05) {
            double spd = Math.min(jukeStrength.getValue(), d * 0.5);
            mc.player.setVelocity(dx / d * spd, vel.y, dz / d * spd);
        }
    }

    private PlayerEntity findNearestPlayer(MinecraftClient mc) {
        float r = detectRange.getValue();
        PlayerEntity best = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player) || player == mc.player) continue;
            if (player.isDead() || player.getHealth() <= 0) continue;

            double dist = mc.player.squaredDistanceTo(player);
            if (dist > r * r || dist >= closestDist) continue;

            closestDist = dist;
            best = player;
        }
        return best;
    }
}
