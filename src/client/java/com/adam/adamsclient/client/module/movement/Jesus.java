package com.adam.adamsclient.client.module.movement;

import com.adam.adamsclient.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Jesus extends Module {
    public Jesus() {
        super("Jesus", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isTouchingWater() && !mc.player.isInLava()) return;
        if (mc.player.isSneaking()) return;

        Vec3d vel = mc.player.getVelocity();
        if (vel.y < 0) {
            mc.player.setVelocity(vel.x, 0.04, vel.z);
        }
    }
}
