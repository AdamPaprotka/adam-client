package com.adam.adamsclient.client.module.visual;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class ESP extends Module {
    private final BoolSetting players = new BoolSetting("Players", true);
    private final BoolSetting mobs = new BoolSetting("Mobs", true);
    private final BoolSetting animals = new BoolSetting("Animals", false);
    private final BoolSetting healthBar = new BoolSetting("Health Bar", true);
    private final FloatSetting lineWidth = new FloatSetting.Builder("Line Width")
            .defaultValue(2f).min(0.5f).max(5f).minSlider(0.5f).maxSlider(3f).build();

    public ESP() {
        super("ESP", Category.VISUAL);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(healthBar);
        addSetting(lineWidth);
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

            Box box = living.getBoundingBox();
            var cam = mc.gameRenderer.getCamera();
            double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX()) - cam.getPos().x;
            double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) - cam.getPos().y;
            double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()) - cam.getPos().z;

            Box renderBox = new Box(
                box.minX - entity.getX() + x,
                box.minY - entity.getY() + y,
                box.minZ - entity.getZ() + z,
                box.maxX - entity.getX() + x,
                box.maxY - entity.getY() + y,
                box.maxZ - entity.getZ() + z
            );

            float r, g, b;
            if (isPlayer) { r = 1f; g = 0f; b = 0f; }
            else if (isMob) { r = 1f; g = 1f; b = 0f; }
            else { r = 0f; g = 1f; b = 0f; }

            drawBox(matrices, renderBox, r, g, b, 1f);
        }
    }

    private void drawBox(MatrixStack matrices, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.minY, (float)box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
