package net.cataclysmweather.mixin;

import net.cataclysmweather.WeatherState;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        WeatherState.serverTick((ServerWorld)(Object)this);
        // La grêle visuelle est gérée par WorldRendererMixin sur le client
    }
}
