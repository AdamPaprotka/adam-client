package com.adam.adamsclient.client.module.visual;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class Tracers extends Module {
    private final BoolSetting players = new BoolSetting("Players", true);
    private final BoolSetting mobs = new BoolSetting("Mobs", false);
    private final BoolSetting animals = new BoolSetting("Animals", false);

    public Tracers() {
        super("Tracers", Category.VISUAL);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
    }

    public void onRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.gameRenderer.getCamera() == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (living.isDead() || living.getHealth() <= 0) continue;

            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isMob = entity instanceof MobEntity;
            boolean isAnimal = entity instanceof AnimalEntity;

            if (isPlayer && !players.getValue()) continue;
            if (isMob && !mobs.getValue()) continue;
            if (isAnimal && !animals.getValue()) continue;
            if (!isPlayer && !isMob && !isAnimal) continue;

            var cam = mc.gameRenderer.getCamera();
            double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX()) - cam.getPos().x;
            double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) - cam.getPos().y + entity.getStandingEyeHeight() * 0.5;
            double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()) - cam.getPos().z;

            float r, g, b;
            if (isPlayer) { r = 1f; g = 0f; b = 0f; }
            else if (isMob) { r = 1f; g = 1f; b = 0f; }
            else { r = 0f; g = 1f; b = 0f; }

            drawLine(matrices, 0, 0, 0, x, y, z, r, g, b, 1f);
        }
    }

    private void drawLine(MatrixStack matrices, double x1, double y1, double z1,
                          double x2, double y2, double z2, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
