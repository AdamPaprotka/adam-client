package com.adam.adamsclient.client.module.visual;

import com.adam.adamsclient.client.module.Module;

public class NoHurtCam extends Module {
    public static NoHurtCam INSTANCE;

    public NoHurtCam() {
        super("NoHurtCam", Category.VISUAL);
        INSTANCE = this;
    }
}
