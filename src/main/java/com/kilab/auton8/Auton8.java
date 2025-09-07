package com.kilab.auton8;

import com.kilab.auton8.hud.Auton8Hud;
import com.kilab.auton8.modules.MqttLinkModule;
import com.kilab.auton8.modules.OpenWebModule;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.kilab.auton8.modules.TimelapseModule;

import meteordevelopment.meteorclient.systems.hud.Hud;
public class Auton8 extends MeteorAddon {
    public static final Category CAT = new Category("Auton8");

    @Override
    public void onInitialize() {
        Modules.get().add(new MqttLinkModule());
        Modules.get().add(new OpenWebModule());
        Modules.get().add(new TimelapseModule(CAT));
        Hud.get().register(Auton8Hud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CAT);
    }

    @Override
    public String getPackage() {
        return "com.kilab.auton8";
    }
}
