package com.adam.adamsclient;
import net.fabricmc.api.ClientModInitializer;

public class AdamClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("Adam's Client Initialized!");
    }
}