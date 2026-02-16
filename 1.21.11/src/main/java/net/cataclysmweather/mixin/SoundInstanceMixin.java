package net.cataclysmweather.mixin;

import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SoundInstance.class)
public interface SoundInstanceMixin {
    // Le Mixin est vidé pour restaurer tous les sons (pluie comprise).
}
