package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Spider extends Module {
    private final FloatSetting climbSpeed = new FloatSetting.Builder("Climb Speed")
            .defaultValue(0.2f).min(0.05f).max(0.5f).minSlider(0.05f).maxSlider(0.5f).build();

    public Spider() {
        super("Spider", Category.MOVEMENT);
        addSetting(climbSpeed);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // horizontalCollision is true when the player is pressed against a wall.
        if (!mc.player.horizontalCollision) return;
        if (mc.player.isSneaking()) return;

        // Climb the wall like a ladder.
        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(vel.x, climbSpeed.getValue(), vel.z);

        // Keep the player "on" the wall so the upward push isn't cancelled.
        mc.player.setOnGround(false);
    }
}
