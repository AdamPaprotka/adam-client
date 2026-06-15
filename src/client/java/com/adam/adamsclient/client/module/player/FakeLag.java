package com.adam.adamsclient.client.module.player;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.setting.BoolSetting;
import com.adam.adamsclient.client.module.setting.FloatSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FakeLag extends Module {
    public static FakeLag INSTANCE;

    /** Set to true while flushing so the mixin doesn't re-intercept. */
    public static boolean flushing = false;

    private final FloatSetting delay = new FloatSetting.Builder("Delay")
            .defaultValue(200f).min(0f).minSlider(0f).maxSlider(1000f).build();
    private final FloatSetting randomExtra = new FloatSetting.Builder("Random Extra")
            .defaultValue(100f).min(0f).minSlider(0f).maxSlider(500f).build();
    private final BoolSetting renderBox = new BoolSetting("Render Box", true);

    private final List<QueuedPacket> queue = new ArrayList<>();
    /** The position the server last acknowledged — updated when packets are flushed. */
    public volatile Vec3d serverPos = null;
    /** Tracks the scheduled send time of the last queued packet to preserve ordering. */
    private long nextSendAt = 0;

    public FakeLag() {
        super("FakeLag", Category.PLAYER);
        INSTANCE = this;
        addSetting(delay);
        addSetting(randomExtra);
        addSetting(renderBox);

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled() || !renderBox.getValue() || serverPos == null) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;
            var consumers = context.consumers();
            if (consumers == null) return;

            Vec3d camPos = context.camera().getPos();
            matrices.push();
            matrices.translate(serverPos.x - camPos.x, serverPos.y - camPos.y, serverPos.z - camPos.z);

            drawOutlinedBox(matrices, consumers.getBuffer(RenderLayer.getLines()),
                    -0.3f, 0f, -0.3f, 0.3f, 1.8f, 0.3f,
                    1f, 0f, 0f, 1f);

            matrices.pop();
        });
    }

    // Draws the 12 edges of an axis-aligned box.
    private static void drawOutlinedBox(MatrixStack matrices, VertexConsumer buf,
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

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public void enqueue(ClientConnection connection, Packet<?> packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d playerPos = mc.player != null ? mc.player.getPos() : Vec3d.ZERO;
        float base  = delay.getValue();
        float extra = (float) (Math.random() * randomExtra.getValue());
        long now = System.currentTimeMillis();
        long earliest = Math.max(nextSendAt, now);
        long sendAt = earliest + (long)(base + extra);
        nextSendAt = sendAt;
        queue.add(new QueuedPacket(connection, packet, sendAt, playerPos));
    }

    @Override
    protected void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) serverPos = mc.player.getPos();
    }

    @Override
    public void onTick() {
        flushReady();
    }

    @Override
    protected void onDisable() {
        flushing = true;
        for (QueuedPacket qp : queue) qp.connection.send(qp.packet);
        queue.clear();
        flushing = false;
        serverPos = null;
        nextSendAt = 0;
    }

    private void flushReady() {
        if (queue.isEmpty()) return;
        long now = System.currentTimeMillis();
        flushing = true;
        Iterator<QueuedPacket> it = queue.iterator();
        while (it.hasNext()) {
            QueuedPacket qp = it.next();
            if (now >= qp.sendAt) {
                qp.connection.send(qp.packet);
                serverPos = qp.playerPos;
                it.remove();
            }
        }
        flushing = false;
    }

    private record QueuedPacket(ClientConnection connection, Packet<?> packet, long sendAt, Vec3d playerPos) {}
}
