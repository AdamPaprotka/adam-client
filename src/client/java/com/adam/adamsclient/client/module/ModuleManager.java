package com.adam.adamsclient.client.module;

import com.adam.adamsclient.client.RotationManager;
import com.adam.adamsclient.client.module.combat.AimAssist;
import com.adam.adamsclient.client.module.combat.AntiAntiKillaura;
import com.adam.adamsclient.client.module.combat.AutoClicker;
import com.adam.adamsclient.client.module.combat.AutoShield;
import com.adam.adamsclient.client.module.combat.AutoTotem;
import com.adam.adamsclient.client.module.combat.BowAssist;
import com.adam.adamsclient.client.module.combat.Criticals;
import com.adam.adamsclient.client.module.combat.HitBox;
import com.adam.adamsclient.client.module.combat.KillAura;
import com.adam.adamsclient.client.module.combat.Reach;
import com.adam.adamsclient.client.module.combat.TriggerBot;
import com.adam.adamsclient.client.module.combat.Velocity;
import com.adam.adamsclient.client.module.movement.AntiVoid;
import com.adam.adamsclient.client.module.movement.Fly;
import com.adam.adamsclient.client.module.movement.Jesus;
import com.adam.adamsclient.client.module.movement.LongJump;
import com.adam.adamsclient.client.module.movement.NoFall;
import com.adam.adamsclient.client.module.movement.NoSlow;
import com.adam.adamsclient.client.module.movement.Speed;
import com.adam.adamsclient.client.module.movement.Sprint;
import com.adam.adamsclient.client.module.movement.Spider;
import com.adam.adamsclient.client.module.movement.Step;
import com.adam.adamsclient.client.module.player.AutoEat;
import com.adam.adamsclient.client.module.player.ChestStealer;
import com.adam.adamsclient.client.module.player.FakeLag;
import com.adam.adamsclient.client.module.player.FastPlace;
import com.adam.adamsclient.client.module.utils.AntiCrash;
import com.adam.adamsclient.client.module.utils.ChunkExpander;
import com.adam.adamsclient.client.module.visual.ESP;
import com.adam.adamsclient.client.module.visual.Fullbright;
import com.adam.adamsclient.client.module.visual.NoHurtCam;
import com.adam.adamsclient.client.module.visual.Tracers;
import com.adam.adamsclient.client.module.visual.XRay;
import com.adam.adamsclient.client.module.visual.Zoom;
import com.adam.adamsclient.client.module.world.Nuker;
import com.adam.adamsclient.client.module.world.Scaffold;
import com.adam.adamsclient.client.module.world.Timer;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();

    public static void init() {
        // Combat
        register(new KillAura());
        register(new AutoClicker());
        register(new Reach());
        register(new Criticals());
        register(new Velocity());
        register(new BowAssist());
        register(new AutoTotem());
        register(new HitBox());
        register(new AntiAntiKillaura());
        register(new AutoShield());
        register(new TriggerBot());
        register(new AimAssist());

        // Movement
        register(new Sprint());
        register(new Speed());
        register(new Step());
        register(new Fly());
        register(new NoFall());
        register(new Jesus());
        register(new Spider());
        register(new LongJump());
        register(new NoSlow());
        register(new AntiVoid());

        // Visual
        register(new Fullbright());
        register(new XRay());
        register(new NoHurtCam());
        register(new ESP());
        register(new Tracers());
        register(new Zoom());

        // Player
        register(new FakeLag());
        register(new AutoEat());
        register(new FastPlace());
        register(new ChestStealer());

        // World
        register(new Nuker());
        register(new Scaffold());
        register(new Timer());

        // Utils
        register(new ChunkExpander());
        register(new AntiCrash());
    }

    private static void register(Module module) {
        modules.add(module);
    }

    public static List<Module> getModules() { return modules; }

    public static List<Module> getByCategory(Module.Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    public static boolean isModuleEnabled(String name) {
        return modules.stream().anyMatch(m -> m.getName().equalsIgnoreCase(name) && m.isEnabled());
    }

    public static void onEarlyTick() {
        for (Module module : modules) {
            if (module.isEnabled()) module.onEarlyTick();
        }
    }

    public static void onTick() {
        RotationManager.tick();
        for (Module module : modules) {
            if (module.isEnabled()) module.onTick();
        }
    }
}
