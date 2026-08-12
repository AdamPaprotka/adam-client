package com.adam.adamsclient.client.module.visual;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled()) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) return;

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;
            var consumers = context.consumers();
            if (consumers == null) return;

            VertexConsumer buf = consumers.getBuffer(RenderLayer.getLines());
            Vec3d camPos = context.camera().getPos();
            float tickDelta = context.tickCounter().getTickDelta(false);

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

                double ex = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
                double ey = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
                double ez = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

                Box box = living.getBoundingBox();
                float minX = (float) (box.minX - entity.getX());
                float minY = (float) (box.minY - entity.getY());
                float minZ = (float) (box.minZ - entity.getZ());
                float maxX = (float) (box.maxX - entity.getX());
                float maxY = (float) (box.maxY - entity.getY());
                float maxZ = (float) (box.maxZ - entity.getZ());

                float r, g, b;
                if (isPlayer) { r = 1f; g = 0f; b = 0f; }
                else if (isMob) { r = 1f; g = 1f; b = 0f; }
                else { r = 0f; g = 1f; b = 0f; }

                matrices.push();
                matrices.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);
                drawBox(matrices, buf, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 1f);
                matrices.pop();
            }
        });
    }

    private static void drawBox(MatrixStack matrices, VertexConsumer buf,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        // bottom
        line(entry, buf, x1,y1,z1, x2,y1,z1, r,g,b,a);
        line(entry, buf, x2,y1,z1, x2,y1,z2, r,g,b,a);
        line(entry, buf, x2,y1,z2, x1,y1,z2, r,g,b,a);
        line(entry, buf, x1,y1,z2, x1,y1,z1, r,g,b,a);
        // top
        line(entry, buf, x1,y2,z1, x2,y2,z1, r,g,b,a);
        line(entry, buf, x2,y2,z1, x2,y2,z2, r,g,b,a);
        line(entry, buf, x2,y2,z2, x1,y2,z2, r,g,b,a);
        line(entry, buf, x1,y2,z2, x1,y2,z1, r,g,b,a);
        // verticals
        line(entry, buf, x1,y1,z1, x1,y2,z1, r,g,b,a);
        line(entry, buf, x2,y1,z1, x2,y2,z1, r,g,b,a);
        line(entry, buf, x2,y1,z2, x2,y2,z2, r,g,b,a);
        line(entry, buf, x1,y1,z2, x1,y2,z2, r,g,b,a);
    }

    private static void line(MatrixStack.Entry entry, VertexConsumer buf,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        float nx = dx/len, ny = dy/len, nz = dz/len;
        buf.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, a).normal(entry, nx, ny, nz);
        buf.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, a).normal(entry, nx, ny, nz);
    }
}
