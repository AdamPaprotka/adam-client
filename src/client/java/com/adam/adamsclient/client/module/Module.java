package com.adam.adamsclient.client.module;

import com.adam.adamsclient.client.module.setting.Setting;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        COMBAT, MOVEMENT, VISUAL, PLAYER, WORLD, UTILS
    }

    private final String name;
    private final Category category;
    private boolean enabled = false;
    private int key = -1; // GLFW_KEY_UNKNOWN
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected void addSetting(Setting<?> setting) { settings.add(setting); }
    public List<Setting<?>> getSettings() { return settings; }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void setEnabled(boolean value) {
        if (value != enabled) toggle();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
}
