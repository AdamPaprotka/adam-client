package com.adam.adamsclient.client;

import com.adam.adamsclient.client.gui.GuiManager;
import com.adam.adamsclient.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import com.adam.adamsclient.client.ConfigManager;

public class AdamClientClient implements ClientModInitializer {
    public static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.adam-client.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_INSERT,
                "key.categories.adam-client"
        ));

        ModuleManager.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            GuiManager.init(client.getWindow().getHandle());
            ConfigManager.load();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigManager.save();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (client.player == null) return;
                client.player.sendMessage(
                    Text.literal("[Adam-Client]").formatted(Formatting.AQUA)
                        .append(Text.literal(" Click INSERT to open menu!").formatted(Formatting.BLUE)),
                    false
                );
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.wasPressed()) {
                GuiManager.visible = !GuiManager.visible;
                if (GuiManager.visible) {
                    client.mouse.unlockCursor();
                } else {
                    ConfigManager.save();
                    client.mouse.lockCursor();
                }
            }
            ModuleManager.onTick();
        });
    }
}
