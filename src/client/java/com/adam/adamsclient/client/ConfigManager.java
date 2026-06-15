package com.adam.adamsclient.client;

import com.adam.adamsclient.client.module.Module;
import com.adam.adamsclient.client.module.ModuleManager;
import com.adam.adamsclient.client.module.setting.*;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ConfigManager {

    private static Path configPath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("adam-client")
                .resolve("config.json");
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.getModules()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", m.isEnabled());
            obj.addProperty("key", m.getKey());

            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) {
                if      (s instanceof BoolSetting  b) settings.addProperty(s.getName(), b.getValue());
                else if (s instanceof FloatSetting f) settings.addProperty(s.getName(), f.getValue());
                else if (s instanceof EnumSetting<?> e) settings.addProperty(s.getName(), e.getValue().name());
                else if (s instanceof ListSetting   l) settings.addProperty(s.getName(), l.getValue());
            }
            obj.add("settings", settings);
            root.add(m.getName(), obj);
        }

        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) return;

        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            for (Module m : ModuleManager.getModules()) {
                if (!root.has(m.getName())) continue;
                JsonObject obj = root.getAsJsonObject(m.getName());

                if (obj.has("enabled") && obj.get("enabled").getAsBoolean() != m.isEnabled())
                    m.toggle();

                if (obj.has("key"))
                    m.setKey(obj.get("key").getAsInt());

                if (!obj.has("settings")) continue;
                JsonObject settings = obj.getAsJsonObject("settings");

                for (Setting<?> s : m.getSettings()) {
                    if (!settings.has(s.getName())) continue;
                    JsonElement el = settings.get(s.getName());
                    if      (s instanceof BoolSetting  b) b.setValue(el.getAsBoolean());
                    else if (s instanceof FloatSetting f) f.setValue(el.getAsFloat());
                    else if (s instanceof EnumSetting  e) e.setByName(el.getAsString());
                    else if (s instanceof ListSetting  l) l.setValue(el.getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Shareable CFG strings ──────────────────────────────────────────────────
    // Plaintext form is "CFG<MODULE>:v1,v2,...;CFG<MODULE>:..." (one entry per
    // module, setting values in declaration order). That plaintext is then
    // Base64-encoded for sharing, e.g. the decoded form for Speed is "CFGSPEED:0.05".

    private static String normalize(String name) {
        return name.replace(" ", "").toUpperCase();
    }

    private static String valueToString(Setting<?> s) {
        if      (s instanceof BoolSetting  b) return Boolean.toString(b.getValue());
        else if (s instanceof FloatSetting f) return Float.toString(f.getValue());
        else if (s instanceof EnumSetting<?> e) return e.getValue().name();
        else if (s instanceof ListSetting   l) return l.getValue();
        return "";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void applyValue(Setting<?> s, String v) {
        try {
            if      (s instanceof BoolSetting  b) b.setValue(Boolean.parseBoolean(v));
            else if (s instanceof FloatSetting f) f.setValue(Float.parseFloat(v));
            else if (s instanceof EnumSetting  e) e.setByName(v);
            else if (s instanceof ListSetting  l) l.setValue(v);
        } catch (NumberFormatException ignored) {
            // skip malformed value, keep current
        }
    }

    /** Encode every module's settings into a Base64 CFG string for sharing. */
    public static String exportCfg() {
        StringBuilder sb = new StringBuilder();
        for (Module m : ModuleManager.getModules()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("CFG").append(normalize(m.getName())).append(':');
            List<String> vals = new ArrayList<>();
            for (Setting<?> s : m.getSettings()) vals.add(valueToString(s));
            sb.append(String.join(",", vals));
        }
        return Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Encode a single module's settings into a Base64 CFG string. */
    public static String exportModule(Module m) {
        StringBuilder sb = new StringBuilder();
        sb.append("CFG").append(normalize(m.getName())).append(':');
        List<String> vals = new ArrayList<>();
        for (Setting<?> s : m.getSettings()) vals.add(valueToString(s));
        sb.append(String.join(",", vals));
        return Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Apply a pasted CFG string to a specific module (only its matching entry). */
    public static boolean importModule(Module m, String input) {
        if (input == null || input.isBlank()) return false;
        input = input.trim();

        String decoded = input;
        try {
            String maybe = new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
            if (maybe.contains("CFG")) decoded = maybe;
        } catch (IllegalArgumentException ignored) {
            // not Base64 — treat as raw plaintext
        }
        if (!decoded.contains("CFG")) return false;

        String want = normalize(m.getName());
        for (String entry : decoded.split(";")) {
            entry = entry.trim();
            if (!entry.startsWith("CFG")) continue;
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            if (!entry.substring(3, colon).equals(want)) continue;

            String valuesStr = entry.substring(colon + 1);
            String[] vals = valuesStr.isEmpty() ? new String[0] : valuesStr.split(",");
            List<Setting<?>> settings = m.getSettings();
            for (int i = 0; i < vals.length && i < settings.size(); i++) {
                applyValue(settings.get(i), vals[i].trim());
            }
            save();
            return true;
        }
        return false;
    }

    /** Apply a CFG string (Base64 or raw plaintext) pasted by the user. */
    public static boolean importCfg(String input) {
        if (input == null || input.isBlank()) return false;
        input = input.trim();

        // Accept either Base64 or already-decoded plaintext.
        String decoded = input;
        try {
            String maybe = new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
            if (maybe.contains("CFG")) decoded = maybe;
        } catch (IllegalArgumentException ignored) {
            // not Base64 — treat input as raw plaintext
        }
        if (!decoded.contains("CFG")) return false;

        boolean any = false;
        for (String entry : decoded.split(";")) {
            entry = entry.trim();
            if (!entry.startsWith("CFG")) continue;
            int colon = entry.indexOf(':');
            if (colon < 0) continue;

            String modName   = entry.substring(3, colon);
            String valuesStr = entry.substring(colon + 1);

            Module m = null;
            for (Module candidate : ModuleManager.getModules()) {
                if (normalize(candidate.getName()).equals(modName)) { m = candidate; break; }
            }
            if (m == null) continue;

            String[] vals = valuesStr.isEmpty() ? new String[0] : valuesStr.split(",");
            List<Setting<?>> settings = m.getSettings();
            for (int i = 0; i < vals.length && i < settings.size(); i++) {
                applyValue(settings.get(i), vals[i].trim());
            }
            any = true;
        }

        if (any) save();
        return any;
    }
}
