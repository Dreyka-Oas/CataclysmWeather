package net.cataclysmweather;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.world.ServerWorld;
import java.io.*;
import java.nio.file.Path;

public class WeatherPersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static class Data {
        public int hailLevel = 0;
        public int lightningLevel = 0;
        public int meteorLevel = 0;
        public int quakeLevel = 0;
        public int hailTicksLeft = 0;
        public int lightningTicksLeft = 0;
        public int meteorTicksLeft = 0;
        public int quakeTicksLeft = 0;
        public boolean hasSpawnedCrevice = false;
    }

    private static Data currentData = new Data();

    public static Data getData() {
        return currentData;
    }

    public static void save(ServerWorld world) {
        File file = getWorldFile(world);
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(currentData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(ServerWorld world) {
        File file = getWorldFile(world);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                Data loaded = GSON.fromJson(reader, Data.class);
                if (loaded != null) currentData = loaded;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static File getWorldFile(ServerWorld world) {
        // En 1.21.11, getRunDirectory() renvoie un Path
        Path runPath = world.getServer().getRunDirectory();
        File dataDir = runPath.resolve("world/data").toFile();
        if (!dataDir.exists()) dataDir.mkdirs();
        return new File(dataDir, "cataclysm_weather.json");
    }
}
