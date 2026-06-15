package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;

public class Speed extends Module {
    private static final Identifier SPEED_ID = Identifier.of("adam-client", "speed");

    private final FloatSetting amplifier = new FloatSetting.Builder("Amplifier")
            .defaultValue(1f).min(0f).minSlider(0f).maxSlider(4f).build();

    public Speed() {
        super("Speed", Category.MOVEMENT);
        addSetting(amplifier);
    }

    @Override
    protected void onEnable() { apply(); }

    @Override
    public void onTick() { apply(); }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        EntityAttributeInstance attr = mc.player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(SPEED_ID);
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        EntityAttributeInstance attr = mc.player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(SPEED_ID);
        attr.addTemporaryModifier(new EntityAttributeModifier(
                SPEED_ID,
                amplifier.getValue() * 0.2,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }
}
