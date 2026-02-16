package net.cataclysmweather;

import net.fabricmc.api.ClientModInitializer;

public class CataclysmWeatherClient implements ClientModInitializer {
    public static int clientHailLevel = 0;
    public static int clientLightningLevel = 0;
    public static int clientMeteorLevel = 0;
    public static int clientQuakeLevel = 0;

    @Override
    public void onInitializeClient() {
        HailNetworking.registerClient();
    }

    public static void tick() {
        clientHailLevel = WeatherState.getHailLevel();
        clientLightningLevel = WeatherState.getLightningLevel();
        clientMeteorLevel = WeatherState.getMeteorLevel();
        clientQuakeLevel = WeatherState.getQuakeLevel();
    }
}
