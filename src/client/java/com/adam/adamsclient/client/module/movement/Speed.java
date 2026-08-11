package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.EnumSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
    public enum Mode { EFFECT, STRAFE, VELO }

    private static final Identifier SPEED_ID = Identifier.of("adam-client", "speed");

    private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.EFFECT);

    private final FloatSetting amplifier = new FloatSetting.Builder("Amplifier")
            .defaultValue(1f).min(0f).minSlider(0f).maxSlider(4f).build();

    private final FloatSetting strafeSpeed = new FloatSetting.Builder("Strafe Speed")
            .defaultValue(0.4f).min(0.05f).max(2f).minSlider(0.1f).maxSlider(1f).build();

    /** Velo: snaps horizontal velocity to this magnitude each tick; halved on diagonal input. */
    private final FloatSetting veloSpeed = new FloatSetting.Builder("Velo Speed")
            .defaultValue(1f).min(0.1f).max(10f).minSlider(0.1f).maxSlider(5f).build();

    /** Holding A+D together should cancel out, not slow you down — force it to cancel every tick. */
    private final BoolSetting fixDiagonalLock = new BoolSetting("Fix AD Lock", true);

    public Speed() {
        super("Speed", Category.MOVEMENT);
        addSetting(mode);
        addSetting(amplifier);
        addSetting(strafeSpeed);
        addSetting(veloSpeed);
        addSetting(fixDiagonalLock);
    }

    @Override
    protected void onEnable() {
        if (mode.getValue() == Mode.EFFECT) applyEffect();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (fixDiagonalLock.getValue()
                && mc.options.leftKey.isPressed()
                && mc.options.rightKey.isPressed()) {
            mc.player.input.movementSideways = 0f;
        }

        switch (mode.getValue()) {
            case EFFECT -> applyEffect();
            case STRAFE -> applyStrafe(mc);
            case VELO -> applyVelo(mc);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        EntityAttributeInstance attr = mc.player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(SPEED_ID);
    }

    private void applyEffect() {
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

    /** Sets horizontal velocity to a fixed magnitude in the input direction every tick. */
    private void applyStrafe(MinecraftClient mc) {
        float f = readForward(mc);
        float s = readStrafe(mc);
        if (f == 0f && s == 0f) return;

        Vec3d vel = mc.player.getVelocity();
        Vec3d dir = movementInputToVelocity(new Vec3d(s, 0, f), strafeSpeed.getValue(), mc.player.getYaw());
        mc.player.setVelocity(dir.x, vel.y, dir.z);
    }

    /** Snaps velocity to Velo Speed in the input direction; halved when moving diagonally (e.g. W+A). */
    private void applyVelo(MinecraftClient mc) {
        float f = readForward(mc);
        float s = readStrafe(mc);
        if (f == 0f && s == 0f) return;

        boolean diagonal = f != 0f && s != 0f;
        float speed = veloSpeed.getValue() * (diagonal ? 0.5f : 1f);

        Vec3d vel = mc.player.getVelocity();
        Vec3d dir = movementInputToVelocity(new Vec3d(s, 0, f), speed, mc.player.getYaw());
        mc.player.setVelocity(dir.x, vel.y, dir.z);
    }

    private float readForward(MinecraftClient mc) {
        return movementMultiplier(mc.options.forwardKey.isPressed(), mc.options.backKey.isPressed());
    }

    private float readStrafe(MinecraftClient mc) {
        boolean left = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();
        if (fixDiagonalLock.getValue() && left && right) return 0f;
        // Vanilla convention: pressing Left is positive sideways input.
        return movementMultiplier(left, right);
    }

    private static float movementMultiplier(boolean a, boolean b) {
        if (a == b) return 0f;
        return a ? 1f : -1f;
    }

    /** Mirrors vanilla Entity#movementInputToVelocity to turn (strafe, forward) input into a world-space vector. */
    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double lenSq = movementInput.lengthSquared();
        if (lenSq < 1.0E-7) return Vec3d.ZERO;
        Vec3d v = (lenSq > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
        float sinYaw = MathHelper.sin(yaw * ((float) Math.PI / 180F));
        float cosYaw = MathHelper.cos(yaw * ((float) Math.PI / 180F));
        return new Vec3d(v.x * cosYaw - v.z * sinYaw, v.y, v.z * cosYaw + v.x * sinYaw);
    }
}
