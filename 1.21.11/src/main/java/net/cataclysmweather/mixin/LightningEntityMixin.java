package net.cataclysmweather.mixin;

import net.cataclysmweather.CataclysmWeatherClient;
import net.cataclysmweather.ScreenShakeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LightningEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningEntity.class)
public abstract class LightningEntityMixin {
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        // Au lieu de modifier la variable qui crash, on laisse Minecraft faire.
        // Mais on déclenche notre tremblement client ici de manière sécurisée.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            LightningEntity self = (LightningEntity)(Object)this;
            double dx = client.player.getX() - self.getX();
            double dy = client.player.getY() - self.getY();
            double dz = client.player.getZ() - self.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            if (dist < 64 && CataclysmWeatherClient.clientLightningLevel >= 8) {
                float shakeAmt = (float)( (1.0 - (dist / 64.0)) * 0.8 );
                ScreenShakeManager.addShake(shakeAmt);
            }
        }
    }
}
