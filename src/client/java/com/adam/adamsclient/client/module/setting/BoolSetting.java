package com.adam.adamsclient.client.module.setting;

import imgui.ImGui;
import imgui.type.ImBoolean;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public void render() {
        ImBoolean b = new ImBoolean(value);
        if (ImGui.checkbox(getName(), b)) {
            value = b.get();
        }
    }
}
