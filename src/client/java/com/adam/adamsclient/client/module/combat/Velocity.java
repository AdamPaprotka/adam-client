package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;

public class Velocity extends Module {
    public static Velocity INSTANCE;

    private final FloatSetting horizontal = new FloatSetting.Builder("Horizontal %")
            .defaultValue(0f).min(0f).max(100f).minSlider(0f).maxSlider(100f).build();
    // Floored at 60 rather than 0 - a flat/near-zero vertical trajectory after taking a hit is an
    // obvious, unspoofable server-side observation. Keeping some real vertical knockback makes
    // the deviation look like normal variance instead of a dead giveaway.
    private final FloatSetting vertical = new FloatSetting.Builder("Vertical %")
            .defaultValue(100f).min(60f).max(100f).minSlider(60f).maxSlider(100f).build();

    public Velocity() {
        super("Velocity", Category.COMBAT);
        INSTANCE = this;
        addSetting(horizontal);
        addSetting(vertical);
    }

    public float getHorizontal() { return horizontal.getValue() / 100f; }
    public float getVertical()   { return vertical.getValue()   / 100f; }
}
