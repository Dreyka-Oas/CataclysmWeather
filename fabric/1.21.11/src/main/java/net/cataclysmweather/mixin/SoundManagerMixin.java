package net.cataclysmweather.mixin;

import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    // Le Mixin est vidé pour restaurer tous les sons (pluie comprise).
}
