package net.cataclysmweather.mixin;

import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    // Le Mixin est vidé pour restaurer toutes les particules (pluie et splash compris).
}
