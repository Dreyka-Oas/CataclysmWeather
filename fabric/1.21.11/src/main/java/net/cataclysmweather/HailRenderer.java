package net.cataclysmweather;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class HailRenderer {
    public static void spawnHailParticles(MinecraftClient client) {
        if (CataclysmWeatherClient.clientHailLevel <= 0) return;

        ClientWorld world = client.world;
        if (world == null || client.player == null) return;

        Random random = world.getRandom();
        BlockPos playerPos = client.player.getBlockPos();
        int radius = 24;
        int level = CataclysmWeatherClient.clientHailLevel;

        int count = switch (level) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 8;
            case 5 -> 15;
            case 6 -> 25;
            case 7 -> 40;
            case 8 -> 60;
            case 9 -> 90;
            case 10 -> 120;
            case 11 -> 150;
            default -> 0;
        };

        for (int i = 0; i < count; i++) {
            double xOff = (random.nextDouble() * 2.0 - 1.0) * radius;
            double zOff = (random.nextDouble() * 2.0 - 1.0) * radius;
            
            double x = client.player.getX() + xOff;
            double z = client.player.getZ() + zOff;
            double y = client.player.getY() + 15 + random.nextDouble() * 10;

            client.particleManager.addParticle(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState()),
                x, y, z,
                random.nextGaussian() * 0.1,
                -1.5 - (level * 0.3), // Chute beaucoup plus rapide
                random.nextGaussian() * 0.1
            );

            if (random.nextFloat() < 0.02 * level) {
                BlockPos impactPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(x, y, z));
                world.playSound(client.player, x, (double)impactPos.getY(), z, 
                    SoundEvents.BLOCK_GLASS_BREAK, 
                    SoundCategory.WEATHER, 
                    0.05f * level, 2.0f);
            }
        }
    }
}
