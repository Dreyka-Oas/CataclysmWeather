package net.cataclysmweather.mixin;

import net.cataclysmweather.CataclysmWeatherClient;
import net.cataclysmweather.HailManager;
import net.cataclysmweather.HailRenderer3D;
import net.cataclysmweather.MeteorManager;
import net.cataclysmweather.MeteorRenderer3D;
import net.cataclysmweather.QuakeManager;
import net.cataclysmweather.WeatherState;
import net.cataclysmweather.ScreenShakeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        CataclysmWeatherClient.tick();
        HailManager.tick(this.client);
        MeteorManager.tick(this.client);
        QuakeManager.tick();
        ScreenShakeManager.tick();
    }

    @Inject(method = "renderWeather", at = @At("TAIL"))
    private void onRenderWeatherTail(net.minecraft.client.render.FrameGraphBuilder frameGraphBuilder, com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice, CallbackInfo ci) {
        renderCustomWeather();
    }

    private void renderCustomWeather() {
        if (this.client.player == null) return;
        MatrixStack matrices = new MatrixStack();
        Camera camera = this.client.gameRenderer.getCamera();
        Vec3d cameraPos = ((CameraAccessor)camera).getPos();
        VertexConsumerProvider.Immediate consumers = this.client.getBufferBuilders().getEntityVertexConsumers();
        float tickDelta = HailManager.getCustomTickDelta();

        if (!HailManager.stones.isEmpty()) HailRenderer3D.render(matrices, consumers, cameraPos.x, cameraPos.y, cameraPos.z, tickDelta);
        if (!MeteorManager.meteors.isEmpty()) MeteorRenderer3D.render(matrices, consumers, cameraPos.x, cameraPos.y, cameraPos.z, tickDelta);
    }
}
