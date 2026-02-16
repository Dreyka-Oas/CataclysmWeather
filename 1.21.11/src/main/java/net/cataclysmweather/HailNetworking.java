package net.cataclysmweather;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class HailNetworking {
    public static final Identifier HAIL_SYNC_ID = Identifier.of("cataclysmweather", "hail_sync");
    public static final CustomPayload.Id<HailPayload> HAIL_PAYLOAD_ID = new CustomPayload.Id<>(HAIL_SYNC_ID);

    public record HailPayload(int level, int lightningLevel, int meteorLevel, int quakeLevel, boolean dry) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, HailPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.level());
                buf.writeInt(value.lightningLevel());
                buf.writeInt(value.meteorLevel());
                buf.writeInt(value.quakeLevel());
                buf.writeBoolean(value.dry());
            },
            buf -> new HailPayload(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return HAIL_PAYLOAD_ID;
        }
    }

    public record QuakeImpactPayload(double x, double z, float intensity, int duration) implements CustomPayload {
        public static final Identifier ID = Identifier.of("cataclysmweather", "quake_impact");
        public static final CustomPayload.Id<QuakeImpactPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);
        public static final PacketCodec<PacketByteBuf, QuakeImpactPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeDouble(value.x());
                buf.writeDouble(value.z());
                buf.writeFloat(value.intensity());
                buf.writeInt(value.duration());
            },
            buf -> new QuakeImpactPayload(buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return PAYLOAD_ID;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(HAIL_PAYLOAD_ID, HailPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(QuakeImpactPayload.PAYLOAD_ID, QuakeImpactPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(HAIL_PAYLOAD_ID, (payload, context) -> {
            context.client().execute(() -> {
                WeatherState.setClientState(payload.level(), payload.lightningLevel(), payload.meteorLevel(), payload.quakeLevel());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(QuakeImpactPayload.PAYLOAD_ID, (payload, context) -> {
            context.client().execute(() -> {
                QuakeManager.addQuake(payload.x(), payload.z(), payload.intensity(), payload.duration());
            });
        });
    }
}
