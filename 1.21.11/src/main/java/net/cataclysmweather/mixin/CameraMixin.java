package net.cataclysmweather.mixin;

import net.cataclysmweather.QuakeManager;
import net.cataclysmweather.ScreenShakeManager;
import net.cataclysmweather.WeatherState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private Vec3d pos;

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdateTail(World world, Entity focusedEntity, boolean thirdPerson, boolean frontal, float tickDelta, CallbackInfo ci) {
        float impactShake = ScreenShakeManager.getIntensity();
        // Utilise désormais les secousses localisées
        float localQuake = QuakeManager.getIntensityAt(this.pos);
        
        if (localQuake <= 0 && impactShake <= 0.01f) return;

        // Modulation temporelle pour le jitter
        long time = world.getTime();
        float timeF = time + tickDelta;
        
        // Intensité totale
        float totalIntensity = localQuake + impactShake;

        if (totalIntensity > 0.01f) {
            double multiplier = 1.8;
            
            // Mélange de mouvement fluide (sinus) et de jitter aléatoire
            double ox = (Math.sin(timeF * 1.5) * 0.4 + (Math.random() - 0.5) * 0.8) * totalIntensity * multiplier;
            double oy = (Math.cos(timeF * 1.2) * 0.4 + (Math.random() - 0.5) * 0.8) * totalIntensity * multiplier;
            double oz = (Math.sin(timeF * 1.8) * 0.4 + (Math.random() - 0.5) * 0.8) * totalIntensity * multiplier;
            
            this.pos = this.pos.add(ox, oy, oz);
        }
    }
}
