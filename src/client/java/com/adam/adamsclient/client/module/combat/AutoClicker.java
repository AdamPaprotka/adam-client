package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

public class AutoClicker extends Module {
    private final FloatSetting cps = new FloatSetting.Builder("CPS")
            .defaultValue(10f).min(1f).max(20f).minSlider(1f).maxSlider(20f).build();
    private final BoolSetting breakBlocks = new BoolSetting("Break Blocks", false);

    private long lastClick = 0;

    public AutoClicker() {
        super("AutoClicker", Category.COMBAT);
        addSetting(cps);
        addSetting(breakBlocks);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        long now = System.currentTimeMillis();
        if (now - lastClick < (long)(1000f / cps.getValue())) return;

        if (mc.targetedEntity instanceof LivingEntity target) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastClick = now;
        } else if (breakBlocks.getValue() && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            mc.options.attackKey.setPressed(true);
            lastClick = now;
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) mc.options.attackKey.setPressed(false);
    }
}
