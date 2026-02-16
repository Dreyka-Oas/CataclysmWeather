package net.cataclysmweather;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;

public class HailRenderer3D {
    public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ, float tickDelta) {
        if (HailManager.stones.isEmpty()) return;

        BlockRenderManager blockRenderer = MinecraftClient.getInstance().getBlockRenderManager();
        BlockState iceState = Blocks.BLUE_ICE.getDefaultState();
        
        for (HailManager.HailStone stone : HailManager.stones) {
            matrices.push();
            
            // Interpolation de la position
            double x = stone.prevPos.x + (stone.pos.x - stone.prevPos.x) * tickDelta;
            double y = stone.prevPos.y + (stone.pos.y - stone.prevPos.y) * tickDelta;
            double z = stone.prevPos.z + (stone.pos.z - stone.prevPos.z) * tickDelta;
            
            matrices.translate(x - cameraX, y - cameraY, z - cameraZ);
            
            // Interpolation de la rotation
            float rotX = stone.prevRotationX + (stone.rotationX - stone.prevRotationX) * tickDelta;
            float rotY = stone.prevRotationY + (stone.rotationY - stone.prevRotationY) * tickDelta;
            float rotZ = stone.prevRotationZ + (stone.rotationZ - stone.prevRotationZ) * tickDelta;
            
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));

            float s = stone.size * 2.0f; 
            matrices.scale(s, s, s);
            
            matrices.translate(-0.5, -0.5, -0.5);
            
            blockRenderer.renderBlockAsEntity(iceState, matrices, vertexConsumers, 15728880, OverlayTexture.DEFAULT_UV);
            
            matrices.pop();
        }
    }
}
