package pl.autotpa;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AutoTpaClient implements ClientModInitializer {
    private static final int MIN_SECONDS = 10;

    private static String target = null;
    private static int intervalTicks = 0;
    private static int counter = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (target == null) return;
            if (mc.player == null || mc.player.connection == null) {
                target = null; // rozłączono z serwerem
                return;
            }
            if (--counter <= 0) {
                mc.player.connection.sendCommand("tpa " + target);
                counter = intervalTicks;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("autotpa")
                .then(ClientCommandManager.literal("start")
                    .then(ClientCommandManager.argument("gracz", StringArgumentType.word())
                        .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "gracz"), 30))
                        .then(ClientCommandManager.argument("sekundy", IntegerArgumentType.integer(MIN_SECONDS, 3600))
                            .executes(ctx -> start(ctx.getSource(),
                                StringArgumentType.getString(ctx, "gracz"),
                                IntegerArgumentType.getInteger(ctx, "sekundy"))))))
                .then(ClientCommandManager.literal("stop").executes(ctx -> {
                    target = null;
                    ctx.getSource().sendFeedback(Component.literal("[AutoTPA] Zatrzymano."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("status").executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal(target == null
                        ? "[AutoTPA] Wyłączony."
                        : "[AutoTPA] Wysyłam /tpa " + target + " co " + (intervalTicks / 20) + " s."));
                    return 1;
                }))));
    }

    private static int start(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src, String player, int seconds) {
        target = player;
        intervalTicks = seconds * 20;
        counter = 1; // pierwsza wiadomość od razu
        src.sendFeedback(Component.literal("[AutoTPA] Start: /tpa " + player + " co " + seconds + " s. Zatrzymaj: /autotpa stop"));
        return 1;
    }
}
