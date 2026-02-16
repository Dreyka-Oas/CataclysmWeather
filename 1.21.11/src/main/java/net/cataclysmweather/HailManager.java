package net.cataclysmweather;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.sound.SoundEvents;

public class HailManager {
    public static class HailStone {
        public Vec3d pos;
        public Vec3d prevPos;
        public Vec3d velocity;
        public float rotationX, rotationY, rotationZ;
        public float prevRotationX, prevRotationY, prevRotationZ;
        public float rotSpeedX, rotSpeedY, rotSpeedZ;
        public float size;
        public int age;
        public boolean onGround = false;
        public int groundTicks = 0;
        public final int maxGroundTicks = 60; 
        public double groundY;
        public boolean soundPlayed = false;

        public HailStone(Vec3d pos, Vec3d velocity, float size, java.util.Random random, double groundY) {
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
            this.rotSpeedX = (random.nextFloat() - 0.5f) * 25;
            this.rotSpeedY = (random.nextFloat() - 0.5f) * 25;
            this.rotSpeedZ = (random.nextFloat() - 0.5f) * 25;
            this.age = 0;
        }

        public void simulate() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return;

            if (onGround) {
                this.prevPos = this.pos;
                this.prevRotationX = this.rotationX;
                this.prevRotationY = this.rotationY;
                this.prevRotationZ = this.rotationZ;
                groundTicks++;
                return;
            }

            this.prevPos = this.pos;
            double nextY = this.pos.y + this.velocity.y;
            int ix = MathHelper.floor(this.pos.x);
            int iz = MathHelper.floor(this.pos.z);
            
            // On scanne vers le bas pour trouver le premier bloc solide
            for (int y = MathHelper.floor(this.pos.y); y >= MathHelper.floor(nextY) - 1; y--) {
                BlockPos checkPos = BlockPos.ofFloored(ix, y, iz);
                if (!client.world.getBlockState(checkPos).isAir() && !client.world.getFluidState(checkPos).isStill()) {
                    this.groundY = y + 1.0; 
                    this.pos = new Vec3d(this.pos.x, this.groundY, this.pos.z);
                    this.onGround = true;
                    this.velocity = Vec3d.ZERO;
                    triggerImpactEffects(client);
                    break;
                }
            }

            if (!onGround) {
                this.pos = this.pos.add(this.velocity);
                if (this.pos.y <= this.groundY) {
                    this.pos = new Vec3d(this.pos.x, this.groundY, this.pos.z);
                    this.onGround = true;
                    this.velocity = Vec3d.ZERO;
                    triggerImpactEffects(client);
                }
            }

            this.rotationX += this.rotSpeedX;
            this.rotationY += this.rotSpeedY;
            this.rotationZ += this.rotSpeedZ;
            this.age++;
        }

        private void triggerImpactEffects(MinecraftClient client) {
            if (client.player != null) {
                double dx = client.player.getX() - this.pos.x;
                double dy = client.player.getY() - this.pos.y;
                double dz = client.player.getZ() - this.pos.z;
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                
                if (dist < 32 && CataclysmWeatherClient.clientHailLevel >= 10) {
                    float shakeAmt = (float)( (1.0 - (dist / 32.0)) * (this.size / 10.0) );
                    ScreenShakeManager.addShake(shakeAmt);
                }
            }
        }
    }

    public static final List<HailStone> stones = new CopyOnWriteArrayList<>();
    private static long lastTickTime = System.currentTimeMillis();
    private static final java.util.Random SPAWN_RAND = new java.util.Random();
    private static int spawnTimer = 0;

    public static float getCustomTickDelta() {
        long now = System.currentTimeMillis();
        float delta = (now - lastTickTime) / 50.0f;
        return Math.min(1.0f, Math.max(0.0f, delta));
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        lastTickTime = System.currentTimeMillis();
        int level = CataclysmWeatherClient.clientHailLevel;

        int totalStones = stones.size();
        client.player.sendMessage(net.minecraft.text.Text.literal("§bGrêlons : " + totalStones + " (Niveau " + level + ")"), true);

        if (level > 0) spawnStones(client, level);
        for (HailStone stone : stones) {
            stone.simulate();
            if (stone.groundTicks > stone.maxGroundTicks || (stone.age > 400 && !stone.onGround)) {
                stones.remove(stone);
            }
            
            if (stone.onGround && !stone.soundPlayed) {
                stone.soundPlayed = true;
                double dx = client.player.getX() - stone.pos.x;
                double dz = client.player.getZ() - stone.pos.z;
                double distSq = dx*dx + dz*dz;
                if (distSq < 2500) {
                    float distFactor = (float)(1.0 - Math.sqrt(distSq) / 50.0);
                    float baseVol = Math.min(1.0f, 0.2f + (stone.size * 0.5f));
                    float volume = baseVol * distFactor;
                    float pitch = Math.max(0.5f, 2.0f - (stone.size * 0.7f)) + (float)Math.random() * 0.1f;
                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_GLASS_BREAK, pitch, volume));
                    if (stone.size > 1.0f) {
                        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_STONE_HIT, pitch * 0.5f, volume * 0.6f));
                    }
                }
            }
        }
    }

    private static void spawnStones(MinecraftClient client, int level) {
        // NOUVEAUX QUOTAS
        int maxStones = 150;
        if (level >= 15) maxStones = 50;
        else if (level == 14) maxStones = 80;
        else if (level == 13) maxStones = 120;
        
        if (stones.size() >= maxStones) return;

        spawnTimer++;
        // INTERVALLES AJUSTÉS : Plus c'est petit, plus ça tombe vite pour remplir le quota
        int interval = (level >= 14) ? 1 : 0; // 0 = spawn à chaque tick, voire plusieurs si on voulait
        if (spawnTimer < interval) return;
        spawnTimer = 0;

        // Pour les niveaux 1-13, on permet d'en spawn 2 par tick pour atteindre les 150
        int spawnCount = (level <= 13) ? 2 : 1;
        for (int j = 0; j < spawnCount; j++) {
            if (stones.size() >= maxStones) break;
            
            double radius = Math.min(client.options.getViewDistance().getValue(), 12) * 16.0;
            double x = client.player.getX() + (SPAWN_RAND.nextDouble() - 0.5) * (radius * 2.0);
            double z = client.player.getZ() + (SPAWN_RAND.nextDouble() - 0.5) * (radius * 2.0);
            double y = client.player.getY() + 100 + SPAWN_RAND.nextDouble() * 30.0; 
            float size = calculateSize(level, SPAWN_RAND);
            
            int ix = MathHelper.floor(x);
            int iz = MathHelper.floor(z);
            double groundY = client.player.getY() - 64; 
            try {
                groundY = client.world.getTopY(Heightmap.Type.OCEAN_FLOOR, ix, iz);
            } catch (Exception e) {}
            
            stones.add(new HailStone(new Vec3d(x, y, z), new Vec3d(0, -5.0, 0), size, SPAWN_RAND, groundY));
        }
    }

    private static float calculateSize(int level, java.util.Random random) {
        float roll = random.nextFloat();
        float dangerChance = Math.min(0.95f, level / 15.0f);
        if (roll < dangerChance) {
            if (level >= 15) return 10.0f + random.nextFloat() * 10.0f;
            if (level == 14) return 5.0f + random.nextFloat() * 5.0f;
            if (level == 13) return 2.0f + random.nextFloat() * 5.0f;
            if (level >= 10) return 0.8f + random.nextFloat() * 2.0f;
            if (level >= 7)  return 0.4f + random.nextFloat() * 1.0f;
            return 0.25f + random.nextFloat() * 0.35f;
        } else return 0.1f + random.nextFloat() * 0.15f;
    }
}
