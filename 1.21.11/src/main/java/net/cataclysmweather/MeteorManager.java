package net.cataclysmweather;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.particle.ParticleTypes;

public class MeteorManager {
    public static class Meteor {
        public Vec3d pos;
        public Vec3d prevPos;
        public Vec3d velocity;
        public float rotationX, rotationY, rotationZ;
        public float prevRotationX, prevRotationY, prevRotationZ;
        public float rotSpeedX, rotSpeedY, rotSpeedZ;
        public float size;
        public final double groundY;
        public boolean onGround = false;
        public int groundTicks = 0;
        public final int maxGroundTicks = 60; 

        public Meteor(Vec3d pos, Vec3d velocity, float size, double groundY, java.util.Random random) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.size = size;
            this.groundY = groundY;
            
            this.rotationX = random.nextFloat() * 360;
            this.rotationY = random.nextFloat() * 360;
            this.rotationZ = random.nextFloat() * 360;
            this.prevRotationX = this.rotationX;
            this.prevRotationY = this.rotationY;
            this.prevRotationZ = this.rotationZ;
            
            this.rotSpeedX = (random.nextFloat() - 0.5f) * 60;
            this.rotSpeedY = (random.nextFloat() - 0.5f) * 60;
            this.rotSpeedZ = (random.nextFloat() - 0.5f) * 60;
        }

        public void simulate() {
            if (onGround) {
                this.prevPos = this.pos;
                this.prevRotationX = this.rotationX;
                this.prevRotationY = this.rotationY;
                this.prevRotationZ = this.rotationZ;
                groundTicks++;
                return;
            }
            
            this.prevPos = this.pos;
            this.pos = this.pos.add(this.velocity);
            
            this.prevRotationX = this.rotationX;
            this.prevRotationY = this.rotationY;
            this.prevRotationZ = this.rotationZ;
            
            this.rotationX += this.rotSpeedX;
            this.rotationY += this.rotSpeedY;
            this.rotationZ += this.rotSpeedZ;
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.particleManager != null) {
                client.particleManager.addParticle(ParticleTypes.FLAME, this.pos.x, this.pos.y, this.pos.z, 0.0, 0.1, 0.0);
                if (Math.random() < 0.3) client.particleManager.addParticle(ParticleTypes.LARGE_SMOKE, this.pos.x, this.pos.y, this.pos.z, 0.0, 0.0, 0.0);
            }

            if (this.pos.y <= this.groundY) {
                this.pos = new Vec3d(this.pos.x, this.groundY, this.pos.z);
                this.onGround = true;
                this.velocity = Vec3d.ZERO;

                // Tremblement lors de l'impact
                if (client.player != null) {
                    double dx = client.player.getX() - this.pos.x;
                    double dy = client.player.getY() - this.pos.y;
                    double dz = client.player.getZ() - this.pos.z;
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    if (dist < 128) {
                        float shakeAmt = (float)((1.0 - (dist / 128.0)) * (this.size * 0.15f));
                        ScreenShakeManager.addShake(shakeAmt);
                    }
                }
            }
        }
    }

    public static final List<Meteor> meteors = new CopyOnWriteArrayList<>();
    private static final java.util.Random RAND = new java.util.Random();
    private static int spawnTimer = 0;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        int level = WeatherState.getMeteorLevel();

        if (level > 0) {
            spawnMeteors(client, level);
        } else {
            if (!meteors.isEmpty()) meteors.clear();
            return;
        }

        for (Meteor m : meteors) {
            m.simulate();
            if (m.groundTicks > m.maxGroundTicks) {
                meteors.remove(m);
            }
        }
    }

    private static void spawnMeteors(MinecraftClient client, int level) {
        // DÉBIT CONSTANT AU LIEU DE QUOTA
        spawnTimer++;
        // Niveau 15: 1 tous les 5 ticks | Niveau 1: 1 tous les 40 ticks
        int interval = Math.max(5, 45 - (level * 2));
        if (spawnTimer < interval) return;
        spawnTimer = 0;

        double radius = 100.0; 
        double x = client.player.getX() + (RAND.nextDouble() - 0.5) * radius * 2.0;
        double z = client.player.getZ() + (RAND.nextDouble() - 0.5) * radius * 2.0;
        double y = 350.0;
        
        float size = 1.0f + (RAND.nextFloat() * level * 0.5f);
        double groundY = client.world.getTopY(Heightmap.Type.OCEAN_FLOOR, MathHelper.floor(x), MathHelper.floor(z));
        
        meteors.add(new Meteor(new Vec3d(x, y, z), new Vec3d(1.5, -5.0, 0.8), size, groundY, RAND));
    }
}
