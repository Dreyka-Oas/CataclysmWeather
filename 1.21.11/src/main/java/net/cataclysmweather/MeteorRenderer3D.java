package net.cataclysmweather;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;

public class MeteorRenderer3D {
    public static void render(MatrixStack matrices, VertexConsumerProvider consumers, double camX, double camY, double camZ, float tickDelta) {
        BlockRenderManager blockRenderer = MinecraftClient.getInstance().getBlockRenderManager();
        
        for (MeteorManager.Meteor m : MeteorManager.meteors) {
            matrices.push();
            
            double x = MathHelper.lerp(tickDelta, m.prevPos.x, m.pos.x) - camX;
            double y = MathHelper.lerp(tickDelta, m.prevPos.y, m.pos.y) - camY;
            double z = MathHelper.lerp(tickDelta, m.prevPos.z, m.pos.z) - camZ;
            
            matrices.translate(x, y, z);
            
            // Rotation interpolée pour la fluidité
            float rotX = MathHelper.lerpAngleDegrees(tickDelta, m.prevRotationX, m.rotationX);
            float rotY = MathHelper.lerpAngleDegrees(tickDelta, m.prevRotationY, m.rotationY);
            float rotZ = MathHelper.lerpAngleDegrees(tickDelta, m.prevRotationZ, m.rotationZ);
            
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
            
            matrices.scale(m.size, m.size, m.size);
            matrices.translate(-0.5, -0.5, -0.5);
            
            blockRenderer.renderBlockAsEntity(Blocks.MAGMA_BLOCK.getDefaultState(), matrices, consumers, 15728880, OverlayTexture.DEFAULT_UV);
            
            matrices.pop();
        }
    }
}
