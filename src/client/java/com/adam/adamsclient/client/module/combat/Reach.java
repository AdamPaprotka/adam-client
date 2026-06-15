package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;

public class Reach extends Module {
    private static final Identifier REACH_ID = Identifier.of("adam-client", "reach");

    private final FloatSetting distance = new FloatSetting.Builder("Distance")
            .defaultValue(1.5f).min(0f).minSlider(0f).maxSlider(3f).build();

    public Reach() {
        super("Reach", Category.COMBAT);
        addSetting(distance);
    }

    @Override
    protected void onEnable() { apply(); }

    @Override
    public void onTick() { apply(); }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        remove(mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE));
        remove(mc.player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE));
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        double val = distance.getValue();
        applyTo(mc.player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE), val);
        applyTo(mc.player.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE), val);
    }

    private void applyTo(EntityAttributeInstance instance, double value) {
        if (instance == null) return;
        instance.removeModifier(REACH_ID);
        instance.addTemporaryModifier(new EntityAttributeModifier(REACH_ID, value, EntityAttributeModifier.Operation.ADD_VALUE));
    }

    private void remove(EntityAttributeInstance instance) {
        if (instance != null) instance.removeModifier(REACH_ID);
    }
}
