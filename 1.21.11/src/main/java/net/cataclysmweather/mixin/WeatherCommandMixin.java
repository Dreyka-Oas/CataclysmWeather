package net.cataclysmweather.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.cataclysmweather.WeatherState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.WeatherCommand;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherCommand.class)
public class WeatherCommandMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void onRegisterHead(CommandDispatcher<ServerCommandSource> dispatcher, CallbackInfo ci) {
        ci.cancel();

        dispatcher.register(CommandManager.literal("weather")
            .then(CommandManager.literal("clear")
                .executes(context -> executeClear(context.getSource(), 6000))
                .then(CommandManager.argument("duration", IntegerArgumentType.integer(0, 1000000))
                    .executes(context -> executeClear(context.getSource(), IntegerArgumentType.getInteger(context, "duration")))
                )
            )
            .then(CommandManager.literal("rain")
                .executes(context -> executeRain(context.getSource(), 6000))
                .then(CommandManager.argument("duration", IntegerArgumentType.integer(0, 1000000))
                    .executes(context -> executeRain(context.getSource(), IntegerArgumentType.getInteger(context, "duration")))
                )
            )
            .then(CommandManager.literal("thunder")
                .executes(context -> executeLightning(context.getSource(), 4, 6000))
                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 15))
                    .executes(context -> executeLightning(context.getSource(), IntegerArgumentType.getInteger(context, "level"), 6000))
                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(context -> executeLightning(context.getSource(), IntegerArgumentType.getInteger(context, "level"), IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
            .then(CommandManager.literal("grele")
                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 15))
                    .executes(context -> executeHail(context.getSource(), IntegerArgumentType.getInteger(context, "level"), 6000))
                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(context -> executeHail(context.getSource(), IntegerArgumentType.getInteger(context, "level"), IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
            .then(CommandManager.literal("meteor")
                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 15))
                    .executes(context -> executeMeteor(context.getSource(), IntegerArgumentType.getInteger(context, "level"), 6000))
                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(context -> executeMeteor(context.getSource(), IntegerArgumentType.getInteger(context, "level"), IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
            .then(CommandManager.literal("quake")
                .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 15))
                    .executes(context -> executeQuake(context.getSource(), IntegerArgumentType.getInteger(context, "level"), 6000))
                    .then(CommandManager.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(context -> executeQuake(context.getSource(), IntegerArgumentType.getInteger(context, "level"), IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
        );
    }

    private static int executeClear(ServerCommandSource source, int duration) {
        WeatherState.stopAll(source.getWorld());
        source.sendFeedback(() -> Text.translatable("commands.weather.set.clear"), true);
        return duration;
    }

    private static int executeRain(ServerCommandSource source, int duration) {
        WeatherState.clearCustomWeather(source.getWorld());
        source.getWorld().setWeather(0, duration, true, false);
        source.sendFeedback(() -> Text.translatable("commands.weather.set.rain"), true);
        return duration;
    }

    private static int executeHail(ServerCommandSource source, int level, int duration) {
        WeatherState.startHail(source.getWorld(), level, duration);
        String desc = getDesc(level);
        source.sendFeedback(() -> Text.literal("§bGrêle " + desc + " (Niveau " + level + ") activée"), true);
        return 1;
    }

    private static int executeMeteor(ServerCommandSource source, int level, int duration) {
        WeatherState.startMeteor(source.getWorld(), level, duration);
        String desc = getDesc(level);
        source.sendFeedback(() -> Text.literal("§6Météores " + desc + " (Niveau " + level + ") activés"), true);
        return 1;
    }

    private static int executeQuake(ServerCommandSource source, int level, int duration) {
        WeatherState.startQuake(source.getWorld(), level, duration);
        String desc = getDesc(level);
        source.sendFeedback(() -> Text.literal("§2Séisme " + desc + " (Niveau " + level + ") activé"), true);
        return 1;
    }

    private static int executeLightning(ServerCommandSource source, int level, int duration) {
        WeatherState.startLightning(source.getWorld(), level, duration);
        String desc = getDesc(level);
        source.sendFeedback(() -> Text.literal("§eFoudre " + desc + " (Niveau " + level + ") activée"), true);
        return 1;
    }

    private static String getDesc(int level) {
        return switch(level) {
            case 1, 2 -> "légère";
            case 3, 4 -> "modérée";
            case 5, 6 -> "forte";
            case 7, 8 -> "violente";
            case 9, 10 -> "cataclysmique";
            case 11 -> "SURNATURELLE";
            case 12 -> "APOCALYPTIQUE";
            case 13 -> "FIN DU MONDE";
            case 14 -> "COLLAPSUS";
            case 15 -> "SINGULARITÉ";
            default -> "inconnue";
        };
    }
}
