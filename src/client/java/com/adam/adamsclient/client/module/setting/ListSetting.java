package com.adam.adamsclient.client.module.setting;

import imgui.ImGui;
import imgui.type.ImInt;

import java.util.List;

public class ListSetting extends Setting<String> {
    private final String[] options;
    private final ImInt idx = new ImInt(0);

    public ListSetting(String name, List<String> options) {
        super(name, options.get(0));
        this.options = options.toArray(new String[0]);
    }

    public ListSetting(String name, String defaultValue, List<String> options) {
        super(name, defaultValue);
        this.options = options.toArray(new String[0]);
        for (int i = 0; i < this.options.length; i++) {
            if (this.options[i].equals(defaultValue)) { idx.set(i); break; }
        }
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) { idx.set(i); return; }
        }
    }

    @Override
    public void render() {
        if (ImGui.combo(getName(), idx, options)) {
            value = options[idx.get()];
        }
    }
}
