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

    public AntiCrash() {
        super("AntiCrash", Category.UTILS);
        INSTANCE = this;
        addSetting(particleGuard);
        addSetting(maxParticles);
        addSetting(nbtGuard);
        addSetting(maxNbtDepth);
    }

    public boolean isParticleGuardEnabled() { return particleGuard.getValue(); }
    public int getMaxParticles() { return maxParticles.getValue().intValue(); }

    public boolean isNbtGuardEnabled() { return nbtGuard.getValue(); }
    public int getMaxNbtDepth() { return maxNbtDepth.getValue().intValue(); }
}
