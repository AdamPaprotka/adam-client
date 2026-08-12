package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.mixin.KeyBindingAccessor;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;

/**
 * Same fixes as TriggerBot: simulates the attack key's press count on the early tick hook
 * (consumed by vanilla's own unmodified wasPressed/doAttack loop this same tick) instead of
 * calling attackEntity directly, and jitters the interval instead of a perfectly fixed CPS -
 * a dead-flat click rate over a long session is what an autoclicker check catches.
 */
public class AutoClicker extends Module {
    private final FloatSetting cps = new FloatSetting.Builder("CPS")
            .defaultValue(10f).min(1f).max(20f).minSlider(1f).maxSlider(20f).build();
    private final FloatSetting jitter = new FloatSetting.Builder("Jitter %")
            .defaultValue(25f).min(0f).max(80f).minSlider(0f).maxSlider(50f).build();
    private final BoolSetting breakBlocks = new BoolSetting("Break Blocks", false);

    private long lastClick = 0;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(cps);
        addSetting(jitter);
        addSetting(breakBlocks);
    }

    @Override
    public void onEarlyTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        boolean onBlock = mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
        // Mining is continuous-hold, not click-based - just mirror whether we're facing a block.
        if (breakBlocks.getValue()) {
            mc.options.attackKey.setPressed(onBlock);
        }

        if (!(mc.targetedEntity instanceof LivingEntity)) return;

        long now = System.currentTimeMillis();
        double baseInterval = 1000.0 / cps.getValue();
        double spread = baseInterval * (jitter.getValue() / 100.0);
        double effectiveInterval = baseInterval - spread / 2 + Math.random() * spread;
        if (now - lastClick < (long) effectiveInterval) return;

        KeyBindingAccessor attackKey = (KeyBindingAccessor) mc.options.attackKey;
        attackKey.setTimesPressed(attackKey.getTimesPressed() + 1);
        lastClick = now;
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) mc.options.attackKey.setPressed(false);
    }
}
