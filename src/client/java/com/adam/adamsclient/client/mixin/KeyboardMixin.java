package com.adam.adamsclient.client.mixin;

import com.adam.adamsclient.client.ConfigManager;
import com.adam.adamsclient.client.gui.GuiManager;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.ModuleManager;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;

        // Ctrl+V while the menu is open: load a CFG string from the system clipboard.
        if (GuiManager.visible
                && key == GLFW.GLFW_KEY_V
                && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clip = GLFW.glfwGetClipboardString(window);
            boolean ok = ConfigManager.importCfg(clip);
            GuiManager.cfgStatus = ok ? "Loaded CFG (Ctrl+V)" : "No valid CFG in clipboard";
            return;
        }

        if (GuiManager.bindingModule != null) {
            GuiManager.bindingModule.setKey(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key);
            GuiManager.bindingModule = null;
            ci.cancel();
            return;
        }

        // Escape closes our menu instead of falling through to the vanilla pause screen.
        if (GuiManager.visible && key == GLFW.GLFW_KEY_ESCAPE) {
            GuiManager.visible = false;
            ci.cancel();
            return;
        }

        if (key <= 0) return;

        for (Module m : ModuleManager.getModules()) {
            if (m.getKey() == key) {
                m.toggle();
            }
        }
    }
}
