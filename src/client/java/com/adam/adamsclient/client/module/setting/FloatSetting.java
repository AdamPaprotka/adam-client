package com.adam.adamsclient.client.module.setting;

import imgui.ImGui;

public class FloatSetting extends Setting<Float> {
    private final Float min, max;           // null = unlimited hard clamp
    private final float sliderMin, sliderMax; // 0,0 = unlimited in ImGui dragFloat

    private FloatSetting(Builder b) {
        super(b.name, b.defaultValue);
        this.min = b.min;
        this.max = b.max;
        // If slider bounds weren't explicitly set, fall back to hard clamp bounds, then 0
        this.sliderMin = b.sliderMin != null ? b.sliderMin : (b.min != null ? b.min : 0f);
        this.sliderMax = b.sliderMax != null ? b.sliderMax : (b.max != null ? b.max : 0f);
    }

    public FloatSetting(String name, float defaultValue, float min, float max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.sliderMin = min;
        this.sliderMax = max;
    }

    @Override
    public void render() {
        float[] arr = { value };
        // sliderMin == sliderMax == 0 tells ImGui "no clamping"
        if (ImGui.dragFloat(getName(), arr, 0.05f, sliderMin, sliderMax, "%.2f")) {
            float v = arr[0];
            if (min != null) v = Math.max(min, v);
            if (max != null) v = Math.min(max, v);
            value = v;
        }
    }

    public static class Builder {
        private final String name;
        private float defaultValue = 0f;
        private Float min = null;
        private Float max = null;
        private Float sliderMin = null;
        private Float sliderMax = null;

        public Builder(String name) { this.name = name; }

        public Builder defaultValue(float v) { this.defaultValue = v; return this; }
        public Builder min(float v)          { this.min = v;          return this; }
        public Builder max(float v)          { this.max = v;          return this; }
        public Builder minSlider(float v)    { this.sliderMin = v;    return this; }
        public Builder maxSlider(float v)    { this.sliderMax = v;    return this; }

        public FloatSetting build() { return new FloatSetting(this); }
    }
}
