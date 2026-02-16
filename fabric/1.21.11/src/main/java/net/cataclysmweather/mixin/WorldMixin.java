package net.cataclysmweather.mixin;

import net.cataclysmweather.WeatherState;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    // On ne bloque plus rien ici pour laisser la pluie vanilla fonctionner
    // quand l'utilisateur le demande.
}
