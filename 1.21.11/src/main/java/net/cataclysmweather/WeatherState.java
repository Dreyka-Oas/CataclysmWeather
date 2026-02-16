package net.cataclysmweather;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherState {
    private static final Random SERVER_RAND = new Random();
    private static final Map<BlockPos, Float> blockDamage = new ConcurrentHashMap<>();
    private static final Map<Long, List<Impact>> pendingImpacts = new ConcurrentHashMap<>();
    private static final Map<Long, List<Impact>> pendingMeteors = new ConcurrentHashMap<>();
    private static final List<Fracture> activeFractures = Collections.synchronizedList(new ArrayList<>());
    private static ServerWorld lastWorld;

    // Variables client
    private static int clientHailLevel = 0;
    private static int clientLightningLevel = 0;
    private static int clientMeteorLevel = 0;
    private static int clientQuakeLevel = 0;

    static class Fracture {
        double curX, curZ, dx, dz;
        int length, currentStep;
        float intensity;
        boolean spawnedBranches = false;
        Fracture(double x, double z, double dx, double dz, int length, float intensity) {
            this.curX = x; this.curZ = z; this.dx = dx; this.dz = dz; this.length = length; this.intensity = intensity;
            this.currentStep = 0;
        }
    }

    record Impact(BlockPos pos, float size) {}
    record PlayerData(double x, double y, double z, int id, int viewDistance) {}

    public static void setClientState(int hail, int lightning, int meteor, int quake) {
        clientHailLevel = hail;
        clientLightningLevel = lightning;
        clientMeteorLevel = meteor;
        clientQuakeLevel = quake;
        
        if (hail == 0) HailManager.stones.clear();
        if (meteor == 0) MeteorManager.meteors.clear();
    }

    public static void startHail(ServerWorld world, int level, int ticks) {
        WeatherPersistence.Data data = WeatherPersistence.getData();
        data.hailLevel = level; data.hailTicksLeft = ticks;
        WeatherPersistence.save(world);
        syncToAll(world);
    }

    public static void startLightning(ServerWorld world, int level, int ticks) {
        WeatherPersistence.Data data = WeatherPersistence.getData();
        data.lightningLevel = level; data.lightningTicksLeft = ticks;
        WeatherPersistence.save(world);
        syncToAll(world);
    }

    public static void startMeteor(ServerWorld world, int level, int ticks) {
        WeatherPersistence.Data data = WeatherPersistence.getData();
        data.meteorLevel = level; data.meteorTicksLeft = ticks;
        WeatherPersistence.save(world);
        syncToAll(world);
    }

    public static void startQuake(ServerWorld world, int level, int ticks) {
        WeatherPersistence.Data data = WeatherPersistence.getData();
        data.quakeLevel = level; data.quakeTicksLeft = ticks;
        data.hasSpawnedCrevice = false;
        WeatherPersistence.save(world);
        syncToAll(world);
    }

    public static void clearCustomWeather(ServerWorld world) {
        WeatherPersistence.Data data = WeatherPersistence.getData();
        data.hailLevel = 0; data.lightningLevel = 0; data.meteorLevel = 0; data.quakeLevel = 0;
        data.hailTicksLeft = 0; data.lightningTicksLeft = 0; data.meteorTicksLeft = 0; data.quakeTicksLeft = 0;
        data.hasSpawnedCrevice = false;
        WeatherPersistence.save(world);
        pendingImpacts.clear(); pendingMeteors.clear(); blockDamage.clear();
        syncToAll(world);
    }

    public static void stopAll(ServerWorld world) {
        clearCustomWeather(world);
        world.setWeather(6000, 0, false, false);
    }

    public static int getHailLevel() { return (lastWorld != null) ? WeatherPersistence.getData().hailLevel : clientHailLevel; }
    public static int getLightningLevel() { return (lastWorld != null) ? WeatherPersistence.getData().lightningLevel : clientLightningLevel; }
    public static int getMeteorLevel() { return (lastWorld != null) ? WeatherPersistence.getData().meteorLevel : clientMeteorLevel; }
    public static int getQuakeLevel() { return (lastWorld != null) ? WeatherPersistence.getData().quakeLevel : clientQuakeLevel; }

    private static void syncToAll(ServerWorld world) {
        lastWorld = world;
        WeatherPersistence.Data data = WeatherPersistence.getData();
        HailNetworking.HailPayload payload = new HailNetworking.HailPayload(data.hailLevel, data.lightningLevel, data.meteorLevel, data.quakeLevel, false);
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void serverTick(ServerWorld world) {
        if (lastWorld == null) WeatherPersistence.load(world);
        lastWorld = world;
        WeatherPersistence.Data data = WeatherPersistence.getData();
        boolean changed = false;

        if (data.hailTicksLeft > 0) { data.hailTicksLeft--; if (data.hailTicksLeft == 0) { data.hailLevel = 0; pendingImpacts.clear(); changed = true; } }
        if (data.lightningTicksLeft > 0) { data.lightningTicksLeft--; if (data.lightningTicksLeft == 0) { data.lightningLevel = 0; changed = true; } }
        if (data.meteorTicksLeft > 0) { data.meteorTicksLeft--; if (data.meteorTicksLeft == 0) { data.meteorLevel = 0; pendingMeteors.clear(); changed = true; } }
        if (data.quakeTicksLeft > 0) { data.quakeTicksLeft--; if (data.quakeTicksLeft == 0) { data.quakeLevel = 0; changed = true; } }

        if (world.getTime() % 100 == 0) {
            changed = true;
        }

        if (changed) { WeatherPersistence.save(world); syncToAll(world); }

        if (data.hailLevel == 0 && data.lightningLevel == 0 && data.meteorLevel == 0 && data.quakeLevel == 0 && pendingImpacts.isEmpty() && pendingMeteors.isEmpty()) return;
        
        List<PlayerData> players = new ArrayList<>();
        for (ServerPlayerEntity p : world.getPlayers()) players.add(new PlayerData(p.getX(), p.getY(), p.getZ(), p.getId(), p.getViewDistance()));
        
        long time = world.getTime();
        WeatherSimulationThread.execute(() -> {
            if (data.hailLevel > 0) planServerImpacts(world, players, time, data.hailLevel);
            if (data.lightningLevel > 0) spawnLightning(world, players, data.lightningLevel, time);
            if (data.meteorLevel > 0) planMeteorImpacts(world, players, time, data.meteorLevel);
            if (data.quakeLevel > 0) processQuakeLogic(world, players, data.quakeLevel);
        });
        
        processPendingImpacts(world);
        processPendingMeteors(world);
        updateFractures(world);
    }

    private static void updateFractures(ServerWorld world) {
        synchronized (activeFractures) {
            if (world.getTime() % 3 != 0) return;

            List<Fracture> newBranches = new ArrayList<>();
            Iterator<Fracture> it = activeFractures.iterator();
            while (it.hasNext()) {
                Fracture f = it.next();
                
                f.curX += f.dx;
                f.curZ += f.dz;
                
                f.dx += (SERVER_RAND.nextDouble() - 0.5) * 0.2;
                f.dz += (SERVER_RAND.nextDouble() - 0.5) * 0.2;

                double mag = Math.sqrt(f.dx * f.dx + f.dz * f.dz);
                if (mag > 0) {
                    f.dx /= mag;
                    f.dz /= mag;
                }

                if (!f.spawnedBranches && f.currentStep > 15 && f.currentStep < f.length - 20 && SERVER_RAND.nextFloat() < (f.intensity * 0.05f)) {
                    double branchAngle = (SERVER_RAND.nextDouble() - 0.5) * Math.PI * 0.5;
                    double newDx = f.dx * Math.cos(branchAngle) - f.dz * Math.sin(branchAngle);
                    double newDz = f.dx * Math.sin(branchAngle) + f.dz * Math.cos(branchAngle);
                    newBranches.add(new Fracture(f.curX, f.curZ, newDx, newDz, f.length / 2, f.intensity));
                    f.spawnedBranches = true;
                }

                int ix = (int) Math.round(f.curX);
                int iz = (int) Math.round(f.curZ);

                if (f.currentStep % 4 == 0) {
                    float vol = 1.0f + f.intensity * 3.0f;
                    world.playSound(null, ix, world.getTopY(Heightmap.Type.MOTION_BLOCKING, ix, iz), iz, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.BLOCKS, vol, 0.5f + SERVER_RAND.nextFloat() * 0.2f);
                }

                int radius = Math.max(0, (int) (f.intensity * 3.5f));
                int rSq = radius * radius;

                for (int ox = -radius; ox <= radius; ox++) {
                    for (int oz = -radius; oz <= radius; oz++) {
                        if (ox * ox + oz * oz > rSq) continue;
                        
                        int curX = ix + ox;
                        int curZ = iz + oz;
                        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, curX, curZ);
                        int depth = (int) (4 + (f.intensity * 18) + SERVER_RAND.nextInt(3));
                        
                        for (int d = -1; d < depth; d++) {
                            BlockPos deepPos = new BlockPos(curX, topY - d, curZ);
                            BlockState state = world.getBlockState(deepPos);
                            if (!state.isAir() && !state.isOf(Blocks.BEDROCK)) {
                                world.setBlockState(deepPos, Blocks.AIR.getDefaultState());
                            }
                        }
                    }
                }

                f.currentStep++;
                if (f.currentStep >= f.length) {
                    it.remove();
                }
            }
            activeFractures.addAll(newBranches);
        }
    }

    private static void processQuakeLogic(ServerWorld world, List<PlayerData> players, int level) {
        synchronized (activeFractures) {
            for (ServerPlayerEntity p : world.getPlayers()) {
                if (!p.isCreative() && !p.isSpectator()) {
                    boolean nearFracture = false;
                    for (Fracture f : activeFractures) {
                        double distSq = p.squaredDistanceTo(f.curX, p.getY(), f.curZ);
                        if (distSq < 30 * 30) { 
                            nearFracture = true;
                            break;
                        }
                    }

                    if (nearFracture && SERVER_RAND.nextFloat() < (level * 0.1f)) {
                        p.takeKnockback(level * 0.03f, SERVER_RAND.nextFloat() - 0.5f, SERVER_RAND.nextFloat() - 0.5f);
                        if (level >= 10 && SERVER_RAND.nextFloat() < 0.1f) {
                            p.damage(world, world.getDamageSources().magic(), level / 7.0f);
                        }
                    }
                }
            }
        }

        WeatherPersistence.Data data = WeatherPersistence.getData();
        if (level >= 1 && activeFractures.isEmpty() && !data.hasSpawnedCrevice) {
            if (!players.isEmpty()) {
                if (SERVER_RAND.nextFloat() < 0.2f) { 
                    PlayerData player = players.get(SERVER_RAND.nextInt(players.size()));
                    double angleSpawn = SERVER_RAND.nextDouble() * Math.PI * 2.0;
                    double distSpawn = 8.0 + SERVER_RAND.nextDouble() * 10.0;
                    double rx = player.x + Math.cos(angleSpawn) * distSpawn;
                    double rz = player.z + Math.sin(angleSpawn) * distSpawn;
                    
                    int baseLen = 15 + level * 10;
                    int length = baseLen + SERVER_RAND.nextInt(baseLen / 2);
                    
                    double angleDir = SERVER_RAND.nextDouble() * Math.PI * 2.0;
                    double dx = Math.cos(angleDir);
                    double dz = Math.sin(angleDir);
                    
                    float intensity = level / 15.0f;
                    activeFractures.add(new Fracture(rx, rz, dx, dz, length, intensity));
                    data.hasSpawnedCrevice = true;
                    WeatherPersistence.save(world);
                    
                    HailNetworking.QuakeImpactPayload payload = new HailNetworking.QuakeImpactPayload(rx, rz, intensity, length);
                    for (ServerPlayerEntity p : world.getPlayers()) {
                        if (p.squaredDistanceTo(rx, p.getY(), rz) < 128 * 128) {
                            ServerPlayNetworking.send(p, payload);
                        }
                    }
                }
            }
        }
    }

    private static void planMeteorImpacts(ServerWorld world, List<PlayerData> players, long time, int level) {
        for (PlayerData player : players) {
            if (SERVER_RAND.nextFloat() > (level / 100.0f)) continue; 
            double radius = 64.0;
            double x = player.x + (SERVER_RAND.nextDouble() - 0.5) * radius * 2.0;
            double z = player.z + (SERVER_RAND.nextDouble() - 0.5) * radius * 2.0;
            BlockPos topPos = world.getTopPosition(Heightmap.Type.OCEAN_FLOOR, BlockPos.ofFloored(x, 0, z));
            float size = 2.0f + (SERVER_RAND.nextFloat() * level);
            long impactTime = time + 70; 
            pendingMeteors.computeIfAbsent(impactTime, k -> Collections.synchronizedList(new ArrayList<>())).add(new Impact(topPos, size));
        }
    }

    private static void processPendingMeteors(ServerWorld world) {
        long time = world.getTime();
        List<Impact> impacts = pendingMeteors.remove(time);
        if (impacts != null) {
            for (Impact impact : impacts) {
                float power = impact.size * 0.6f;
                world.createExplosion(null, impact.pos.getX(), impact.pos.getY(), impact.pos.getZ(), power, true, World.ExplosionSourceType.TNT);
                if (world.getBlockState(impact.pos.up()).isAir()) {
                    world.setBlockState(impact.pos.up(), Blocks.FIRE.getDefaultState());
                }
            }
        }
    }

    private static void planServerImpacts(ServerWorld world, List<PlayerData> players, long time, int level) {
        for (PlayerData player : players) {
            int count = (level <= 13) ? 2 : 1; 
            for (int i = 0; i < count; i++) {
                if (SERVER_RAND.nextFloat() > (level / 15.0f)) continue;
                double radius = Math.min(player.viewDistance, 12) * 16.0;
                double x = player.x + (SERVER_RAND.nextDouble() - 0.5) * (radius * 2.0);
                double z = player.z + (SERVER_RAND.nextDouble() - 0.5) * (radius * 2.0);
                BlockPos topPos = world.getTopPosition(Heightmap.Type.OCEAN_FLOOR, BlockPos.ofFloored(x, 0, z));
                float size = calculateSize(level, SERVER_RAND);
                long impactTime = time + 15;
                pendingImpacts.computeIfAbsent(impactTime, k -> Collections.synchronizedList(new ArrayList<>())).add(new Impact(topPos, size));
            }
        }
    }

    private static float calculateSize(int level, Random random) {
        float roll = random.nextFloat();
        float dangerChance = Math.min(0.95f, level / 15.0f);
        if (roll < dangerChance) {
            if (level >= 15) return 10.0f + random.nextFloat() * 10.0f;
            if (level == 14) return 5.0f + random.nextFloat() * 5.0f;  
            return 0.25f + random.nextFloat() * 0.35f; 
        } else return 0.1f + random.nextFloat() * 0.15f; 
    }

    private static void processPendingImpacts(ServerWorld world) {
        long time = world.getTime();
        List<Impact> impacts = pendingImpacts.remove(time);
        if (impacts != null) {
            for (Impact impact : impacts) {
                float radius = impact.size / 2.0f;
                Box damageBox = new Box(impact.pos).expand(radius, 5.0, radius);
                List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, damageBox, e -> true);
                for (LivingEntity target : targets) applyDamageToEntity(world, target, impact.size);
                float blockRadius = Math.min(4.5f, radius);
                int r = (int) Math.ceil(blockRadius);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dz * dz <= blockRadius * blockRadius) {
                                applyDamageToBlock(world, impact.pos.add(dx, dy - 1, dz), impact.size);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void applyDamageToEntity(ServerWorld world, LivingEntity entity, float size) {
        if (size < 0.25f) return;
        float baseDamage = (size < 1.0f ? 6.0f : 20.0f);
        if (size > 5.0f) baseDamage = 200.0f;
        if (baseDamage >= 1.0f) {
            entity.damage(world, size > 5.0f ? world.getDamageSources().outOfWorld() : world.getDamageSources().generic(), baseDamage);
            entity.takeKnockback(size * 0.2f, 0, 0);
        }
    }

    private static void applyDamageToBlock(ServerWorld world, BlockPos pos, float size) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;
        float hardness = state.getHardness(world, pos);
        if (hardness < 0 || state.isOf(Blocks.OBSIDIAN)) return;
        float damage = (size > 5.0f) ? 100.0f : size * 0.5f; 
        float current = blockDamage.getOrDefault(pos, 0.0f) + damage;
        if (current >= hardness) {
            world.breakBlock(pos, false);
            blockDamage.remove(pos);
        } else {
            blockDamage.put(pos, current);
            world.setBlockBreakingInfo(pos.hashCode(), pos, (int)((current / hardness) * 10.0f));
        }
    }

    private static void spawnLightning(ServerWorld world, List<PlayerData> players, int level, long time) {
        float chance = (level * 1.4f / 500.0f); if (level >= 15) chance = 0.8f;
        for (PlayerData player : players) {
            if (SERVER_RAND.nextFloat() < chance) {
                double radius = Math.min(player.viewDistance, 12) * 16.0;
                double x = player.x + (SERVER_RAND.nextDouble() - 0.5) * (radius * 2.0);
                double dz = player.z + (SERVER_RAND.nextDouble() - 0.5) * (radius * 2.0);
                BlockPos lPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(x, 0, dz));
                if (world.isSkyVisible(lPos)) {
                    world.getServer().execute(() -> {
                        boolean canExplode = (level < 15) || (time % 10 == 0);
                        spawnBolt(world, lPos, level, canExplode);
                    });
                }
            }
        }
    }

    private static void spawnBolt(ServerWorld world, BlockPos pos, int level, boolean canExplode) {
        LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        bolt.refreshPositionAfterTeleport(pos.getX(), pos.getY(), pos.getZ());
        world.spawnEntity(bolt);
        if (canExplode) {
            float power = 0;
            if (level == 11) power = 1.0f; if (level == 12) power = 2.0f; if (level == 13) power = 3.5f; if (level == 14) power = 5.0f; if (level >= 15) power = 8.0f;
            if (power > 0) world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), power, World.ExplosionSourceType.TNT);
        }
        if (world.getBlockState(pos).isAir()) world.setBlockState(pos, Blocks.FIRE.getDefaultState());
    }
}
