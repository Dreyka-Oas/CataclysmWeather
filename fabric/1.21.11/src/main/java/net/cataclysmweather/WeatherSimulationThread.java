package net.cataclysmweather;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherSimulationThread {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CataclysmWeather-Simulation");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1); // Légèrement moins prioritaire pour ne pas étouffer le reste
        return t;
    });

    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }
}
