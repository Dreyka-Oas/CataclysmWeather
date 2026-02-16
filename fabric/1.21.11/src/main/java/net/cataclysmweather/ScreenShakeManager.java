package net.cataclysmweather;

public class ScreenShakeManager {
    private static float intensity = 0.0f;

    public static void addShake(float amount) {
        intensity = Math.min(1.0f, intensity + amount);
    }

    public static void tick() {
        if (intensity > 0) {
            intensity *= 0.9f; // Amortissement rapide
            if (intensity < 0.01f) intensity = 0;
        }
    }

    public static float getIntensity() {
        return intensity;
    }
}
