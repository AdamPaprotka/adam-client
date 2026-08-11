package com.adam.adamsclient.client.gui;

import com.adam.adamsclient.client.ConfigManager;
import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.ModuleManager;
import com.adam.adamsclient.client.module.setting.Setting;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

public class GuiManager {
    private static ImGuiImplGlfw imguiGlfw;
    private static ImGuiImplGl3 imguiGl3;

    private static boolean initialized = false;

    public static boolean visible = false;
    private static boolean lastVisible = false;
    private static long openTime = 0;
    private static final float FADE_MS = 200f;

    /** Module currently waiting for a key press to bind; null when idle. */
    public static Module bindingModule = null;

    /** Last CFG copy/load result shown under the buttons. Set by GUI or Ctrl+V. */
    public static String cfgStatus = "";

    // ── UI Config state ───────────────────────────────────────────────────────
    public static float[] hubColor     = { 1f, 1f, 1f };
    public static float   hubBgAlpha   = 0.55f;
    public static boolean hubShowKey   = true;
    public static boolean hubEnabled   = true;

    public static void init(long windowHandle) {
        if (initialized) return;

        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.setConfigFlags(io.getConfigFlags() & ~ImGuiConfigFlags.NavEnableKeyboard);
        io.setIniFilename(null);

        imguiGlfw = new ImGuiImplGlfw();
        imguiGlfw.init(windowHandle, true);

        imguiGl3 = new ImGuiImplGl3();
        imguiGl3.init("#version 150");

        initialized = true;
    }

    public static void render() {
        if (!initialized) return;

        // Block ImGui mouse input while a Minecraft screen is open.
        // Always strip both flags first so they never stick across frames.
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean mcScreenOpen = mc.currentScreen != null;
        ImGuiIO io = ImGui.getIO();
        int baseFlags = io.getConfigFlags()
                & ~ImGuiConfigFlags.NavEnableKeyboard
                & ~ImGuiConfigFlags.NoMouse;
        io.setConfigFlags(mcScreenOpen
                ? baseFlags | ImGuiConfigFlags.NoMouse
                : baseFlags);

        // Detect open edge to start fade timer
        if (visible && !lastVisible) openTime = System.currentTimeMillis();
        lastVisible = visible;

        imguiGlfw.newFrame();
        // ImGui's built-in window-switcher (Ctrl/Shift+Tab) pops up an annoying
        // window selector while our menu is open — neutralize Tab before it's processed.
        if (visible) io.setKeysDown(GLFW.GLFW_KEY_TAB, false);
        ImGui.newFrame();

        if (hubEnabled) drawHub();

        if (visible) {
            float alpha = Math.min(1f, (System.currentTimeMillis() - openTime) / FADE_MS);
            float prev = ImGui.getStyle().getAlpha();
            ImGui.getStyle().setAlpha(alpha);
            drawGui();
            ImGui.getStyle().setAlpha(prev);
        }

        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    // ── HUB overlay ──────────────────────────────────────────────────────────

    private static void drawHub() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int flags = ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.AlwaysAutoResize
                | ImGuiWindowFlags.NoCollapse;

        ImGui.setNextWindowBgAlpha(hubBgAlpha);
        ImGui.setNextWindowPos(10, 10, ImGuiCond.Once);
        ImGui.begin("HUB##hub_overlay", flags);

        boolean any = false;
        for (Module m : ModuleManager.getModules()) {
            if (!m.isEnabled()) continue;
            String suffix = (hubShowKey && m.getKey() >= 0) ? " [" + getKeyName(m.getKey()) + "]" : "";
            ImGui.textColored(hubColor[0], hubColor[1], hubColor[2], 1f, m.getName() + suffix);
            any = true;
        }
        if (!any) ImGui.textDisabled("(no modules active)");

        ImGui.end();
    }

    // ── Config GUI ────────────────────────────────────────────────────────────

    private static void drawGui() {
        ImGui.setNextWindowSize(480, 500, ImGuiCond.Once);
        ImGui.begin("Adam Client");

        if (bindingModule != null) {
            ImGui.textColored(1f, 1f, 0.2f, 1f,
                    "Binding: " + bindingModule.getName() + "  —  press a key  (ESC = unbind)");
            ImGui.separator();
        }

        // UI Config section
        if (ImGui.collapsingHeader("UI Config")) {
            ImGui.indent();

            ImBoolean hubOn = new ImBoolean(hubEnabled);
            if (ImGui.checkbox("Show HUB", hubOn)) hubEnabled = hubOn.get();

            ImGui.colorEdit3("HUB Text Color", hubColor);

            float[] alpha = { hubBgAlpha };
            if (ImGui.sliderFloat("HUB BG Alpha", alpha, 0f, 1f)) hubBgAlpha = alpha[0];

            ImBoolean showKey = new ImBoolean(hubShowKey);
            if (ImGui.checkbox("Show Keybind in HUB", showKey)) hubShowKey = showKey.get();

            ImGui.unindent();
            ImGui.spacing();
        }

        ImGui.separator();

        drawCategorySection("Combat",   Module.Category.COMBAT);
        drawCategorySection("Movement", Module.Category.MOVEMENT);
        drawCategorySection("Visual",   Module.Category.VISUAL);
        drawCategorySection("Player",   Module.Category.PLAYER);
        drawCategorySection("World",    Module.Category.WORLD);
        drawCategorySection("Utils",    Module.Category.UTILS);

        ImGui.end();
    }

    private static void drawCategorySection(String label, Module.Category category) {
        ImGui.textColored(0.6f, 0.8f, 1f, 1f, label);
        ImGui.separator();

        for (Module module : ModuleManager.getByCategory(category)) {
            String name = module.getName();

            ImBoolean state = new ImBoolean(module.isEnabled());
            if (ImGui.checkbox(name, state)) module.setEnabled(state.get());
            boolean wantSettings = ImGui.isItemClicked(1);

            ImGui.sameLine();
            if (bindingModule == module) {
                ImGui.textColored(1f, 1f, 0f, 1f, "[...]##k" + name);
            } else {
                if (ImGui.smallButton("[" + getKeyName(module.getKey()) + "]##k" + name)) {
                    bindingModule = module;
                }
            }

            String popupId = "cfg##" + name;
            if (wantSettings) ImGui.openPopup(popupId);
            if (ImGui.beginPopup(popupId)) {
                ImGui.text(name);
                ImGui.separator();
                if (module.getSettings().isEmpty()) {
                    ImGui.textDisabled("No settings");
                } else {
                    for (Setting<?> setting : module.getSettings()) setting.render();
                }

                ImGui.separator();
                if (ImGui.button("Copy CFG##" + name)) {
                    ImGui.setClipboardText(ConfigManager.exportModule(module));
                    cfgStatus = "Copied " + name + " CFG";
                }
                ImGui.sameLine();
                if (ImGui.button("Paste CFG##" + name)) {
                    boolean ok = ConfigManager.importModule(module, ImGui.getClipboardText());
                    cfgStatus = ok ? "Pasted " + name + " CFG" : "Clipboard CFG isn't for " + name;
                }
                if (!cfgStatus.isEmpty()) ImGui.textColored(0.6f, 1f, 0.6f, 1f, cfgStatus);

                ImGui.endPopup();
            }
        }

        ImGui.spacing();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getKeyName(int key) {
        if (key < 0) return "NONE";
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null && !n.isEmpty()) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_ESCAPE        -> "ESC";
            case GLFW.GLFW_KEY_ENTER         -> "ENTER";
            case GLFW.GLFW_KEY_TAB           -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE     -> "BKSP";
            case GLFW.GLFW_KEY_INSERT        -> "INS";
            case GLFW.GLFW_KEY_DELETE        -> "DEL";
            case GLFW.GLFW_KEY_RIGHT         -> "RIGHT";
            case GLFW.GLFW_KEY_LEFT          -> "LEFT";
            case GLFW.GLFW_KEY_DOWN          -> "DOWN";
            case GLFW.GLFW_KEY_UP            -> "UP";
            case GLFW.GLFW_KEY_PAGE_UP       -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN     -> "PGDN";
            case GLFW.GLFW_KEY_HOME          -> "HOME";
            case GLFW.GLFW_KEY_END           -> "END";
            case GLFW.GLFW_KEY_F1            -> "F1";
            case GLFW.GLFW_KEY_F2            -> "F2";
            case GLFW.GLFW_KEY_F3            -> "F3";
            case GLFW.GLFW_KEY_F4            -> "F4";
            case GLFW.GLFW_KEY_F5            -> "F5";
            case GLFW.GLFW_KEY_F6            -> "F6";
            case GLFW.GLFW_KEY_F7            -> "F7";
            case GLFW.GLFW_KEY_F8            -> "F8";
            case GLFW.GLFW_KEY_F9            -> "F9";
            case GLFW.GLFW_KEY_F10           -> "F10";
            case GLFW.GLFW_KEY_F11           -> "F11";
            case GLFW.GLFW_KEY_F12           -> "F12";
            case GLFW.GLFW_KEY_LEFT_SHIFT    -> "LSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL  -> "LCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT      -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT   -> "RSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT     -> "RALT";
            default -> "KEY" + key;
        };
    }

    public static void shutdown() {
        if (!initialized) return;
        imguiGl3.dispose();
        imguiGlfw.dispose();
        ImGui.destroyContext();
        initialized = false;
    }
}
