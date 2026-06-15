package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;

public class Step extends Module {
    private static final Identifier STEP_ID = Identifier.of("adam-client", "step");

    private final FloatSetting height = new FloatSetting.Builder("Height")
            .defaultValue(1.0f).min(1f).max(3f).minSlider(1f).maxSlider(3f).build();

    public Step() {
        super("Step", Category.MOVEMENT);
        addSetting(height);
    }

    @Override protected void onEnable()  { apply(); }
    @Override public    void onTick()    { apply(); }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr != null) attr.removeModifier(STEP_ID);
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
        if (attr == null) return;
        attr.removeModifier(STEP_ID);
        // Default step height is 0.6; add the difference to reach the desired height
        attr.addTemporaryModifier(new EntityAttributeModifier(
                STEP_ID, height.getValue() - 0.6,
                EntityAttributeModifier.Operation.ADD_VALUE));
    }
}
