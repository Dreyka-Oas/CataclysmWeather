package net.cataclysmweather;

import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QuakeManager {
    public static class ActiveQuake {
        public double x, z;
        public float intensity;
        public int ticksLeft;
        public int maxTicks;

        public ActiveQuake(double x, double z, float intensity, int duration) {
            this.x = x;
            this.z = z;
            this.intensity = intensity;
            this.ticksLeft = duration;
            this.maxTicks = duration;
        }
    }

    private static final List<ActiveQuake> activeQuakes = new ArrayList<>();

    public static void addQuake(double x, double z, float intensity, int duration) {
        activeQuakes.add(new ActiveQuake(x, z, intensity, duration));
    }

    public static void tick() {
        Iterator<ActiveQuake> it = activeQuakes.iterator();
        while (it.hasNext()) {
            ActiveQuake quake = it.next();
            quake.ticksLeft--;
            if (quake.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    public static float getIntensityAt(Vec3d pos) {
        float totalIntensity = 0;
        for (ActiveQuake quake : activeQuakes) {
            double distSq = pos.squaredDistanceTo(quake.x, pos.y, quake.z);
            double maxDist = 64.0; // Portée du séisme : 64 blocs
            double maxDistSq = maxDist * maxDist;

            if (distSq < maxDistSq) {
                float distanceFactor = 1.0f - (float)(Math.sqrt(distSq) / maxDist);
                // Le tremblement est plus fort au début et s'estompe vers la fin de la crevasse
                float lifeFactor = (float)quake.ticksLeft / (float)quake.maxTicks;
                totalIntensity += quake.intensity * distanceFactor * lifeFactor;
            }
        }
        return Math.min(1.0f, totalIntensity);
    }
}
