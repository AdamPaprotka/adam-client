package com.adam.adamsclient.client.module.utils;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;

public class AntiCrash extends Module {
    public static AntiCrash INSTANCE;

    private final BoolSetting particleGuard = new BoolSetting("Particle Guard", true);
    private final FloatSetting maxParticles = new FloatSetting.Builder("Max Particles")
            .defaultValue(5000f).min(100f).max(50000f).minSlider(500f).maxSlider(20000f).build();

    private final BoolSetting nbtGuard = new BoolSetting("NBT Guard", true);
    private final FloatSetting maxNbtDepth = new FloatSetting.Builder("Max NBT Depth")
            .defaultValue(64f).min(8f).max(512f).minSlider(16f).maxSlider(256f).build();

    /** Guards ExplosionS2CPacket: rejects NaN/absurd center or knockback (known client-crash vector). */
    private final BoolSetting explosionGuard = new BoolSetting("Explosion Guard", true);
    private final FloatSetting maxExplosionDistance = new FloatSetting.Builder("Max Explosion Distance")
            .defaultValue(512f).min(64f).max(4096f).minSlider(128f).maxSlider(2048f).build();
    private final FloatSetting maxKnockback = new FloatSetting.Builder("Max Knockback")
            .defaultValue(1000f).min(50f).max(10000f).minSlider(100f).maxSlider(5000f).build();

    /** Guards recursive/self-referencing text components (chat, titles, action bar) from expanding forever. */
    private final BoolSetting textGuard = new BoolSetting("Text Guard", true);
    private final FloatSetting maxTextNodes = new FloatSetting.Builder("Max Text Nodes")
            .defaultValue(4000f).min(200f).max(50000f).minSlider(500f).maxSlider(20000f).build();

    public AntiCrash() {
        super("AntiCrash", Category.UTILS);
        INSTANCE = this;
        addSetting(particleGuard);
        addSetting(maxParticles);
        addSetting(nbtGuard);
        addSetting(maxNbtDepth);
        addSetting(explosionGuard);
        addSetting(maxExplosionDistance);
        addSetting(maxKnockback);
        addSetting(textGuard);
        addSetting(maxTextNodes);
    }

    public boolean isParticleGuardEnabled() { return particleGuard.getValue(); }
    public int getMaxParticles() { return maxParticles.getValue().intValue(); }

    public boolean isNbtGuardEnabled() { return nbtGuard.getValue(); }
    public int getMaxNbtDepth() { return maxNbtDepth.getValue().intValue(); }

    public boolean isExplosionGuardEnabled() { return explosionGuard.getValue(); }
    public double getMaxExplosionDistance() { return maxExplosionDistance.getValue(); }
    public double getMaxKnockback() { return maxKnockback.getValue(); }

    public boolean isTextGuardEnabled() { return textGuard.getValue(); }
    public int getMaxTextNodes() { return maxTextNodes.getValue().intValue(); }
}
