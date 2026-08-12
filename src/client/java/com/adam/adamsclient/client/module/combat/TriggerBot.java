package com.adam.adamsclient.client.module.combat;

import com.adam.adamsclient.client.mixin.KeyBindingAccessor;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Attacks whatever's already under vanilla's own crosshair target (mc.crosshairTarget) - no aim
 * assist, no rotation touched at all. Rather than calling attackEntity directly, this simulates
 * the attack key actually being pressed (runs on the early tick hook, before vanilla's own
 * wasPressed()/doAttack() loop consumes queued presses this same tick), so the resulting attack
 * runs through the exact same code path, timing, and side effects a genuine left-click produces.
 */
public class TriggerBot extends Module {
    private final BoolSetting players = new BoolSetting("Players", true);
    private final BoolSetting mobs    = new BoolSetting("Mobs", true);
    private final BoolSetting animals = new BoolSetting("Animals", false);
    private final FloatSetting delay  = new FloatSetting.Builder("Delay")
            .defaultValue(110f).min(0f).minSlider(0f).maxSlider(1000f).build();
    private final FloatSetting randomTicks = new FloatSetting.Builder("Random Ticks")
            .defaultValue(2f).min(0f).max(40f).minSlider(0f).maxSlider(20f).build();

    private long lastAttackTime = 0;
    private int ticksSinceReady = 0;
    private int currentRandomTickDelay = 0;

    public TriggerBot() {
        super("TriggerBot", Category.COMBAT);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(delay);
        addSetting(randomTicks);
    }

    @Override
    public void onEarlyTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity target)) return;
        if (target.isDead() || target.getHealth() <= 0) return;

        boolean isPlayer = target instanceof PlayerEntity;
        boolean isMob    = target instanceof MobEntity;
        boolean isAnimal = target instanceof AnimalEntity;
        if (isPlayer  && !players.getValue()) return;
        if (isMob     && !mobs.getValue())    return;
        if (isAnimal  && !animals.getValue()) return;
        if (!isPlayer && !isMob && !isAnimal) return;

        long now = System.currentTimeMillis();
        if (now - lastAttackTime < (long) (float) delay.getValue()) return;

        if (ticksSinceReady++ < currentRandomTickDelay) return;
        ticksSinceReady = 0;
        currentRandomTickDelay = (int) (Math.random() * (randomTicks.getValue() + 1));

        KeyBindingAccessor attackKey = (KeyBindingAccessor) mc.options.attackKey;
        attackKey.setTimesPressed(attackKey.getTimesPressed() + 1);
        lastAttackTime = now;
    }
}
