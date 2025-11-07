package me.libreh.trackercompass.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.util.GenericModInfo;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Optional;

public class TrackerCompassCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("trackercompass")
                .executes(TrackerCompassCommand::about)
                .then(CommandManager.literal("reload")
                        .requires(Permissions.require("manhunt.commands.reload", 4))
                        .executes(TrackerCompassCommand::reloadConfig)
                )
        );
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("trackercompass");
            if (modContainer.isEmpty()) {
                source.sendFeedback(() -> Text.literal("ModContainer trackercompass was not found!"), true);
                return 0;
            }

            GenericModInfo.build(modContainer.get());
        }

        for (var text : source.getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            source.sendFeedback(() -> text, false);
        }
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!ConfigManager.load()) {
            source.sendError(Text.literal("Failed to reload config. Check server logs for details."));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Config reloaded successfully!"), true);
        return 1;
    }
}
