package net.cataclysmweather;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CataclysmWeather implements ModInitializer {
    public static final String MOD_ID = "cataclysmweather";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Cataclysm Weather initialized - Starting from scratch!");
        HailNetworking.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            WeatherPersistence.Data data = WeatherPersistence.getData();
            HailNetworking.HailPayload payload = new HailNetworking.HailPayload(data.hailLevel, data.lightningLevel, data.meteorLevel, data.quakeLevel, false);
            server.execute(() -> {
                ServerPlayNetworking.send(handler.player, payload);
            });
        });
    }
}
