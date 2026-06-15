package com.adam.adamsclient.client.module.setting;

import imgui.ImGui;
import imgui.type.ImInt;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;
    private final String[] names;

    @SuppressWarnings("unchecked")
    public EnumSetting(String name, E defaultValue) {
        super(name, defaultValue);
        this.values = (E[]) defaultValue.getDeclaringClass().getEnumConstants();
        this.names  = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name().charAt(0)
                     + values[i].name().substring(1).toLowerCase().replace('_', ' ');
        }
    }

    public void setByName(String name) {
        for (E v : values) {
            if (v.name().equalsIgnoreCase(name)) { value = v; return; }
        }
    }

    @Override
    public void render() {
        ImInt idx = new ImInt(value.ordinal());
        if (ImGui.combo(getName(), idx, names)) {
            value = values[idx.get()];
        }
    }
}
