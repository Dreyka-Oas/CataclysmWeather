package net.cataclysmweather.mixin;

import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.client.render.VertexConsumers$Dual")
public abstract class VertexConsumerMixin {
    // On vide le Mixin pour éviter tout conflit avec les shaders Photon sur les éclairs.
}
