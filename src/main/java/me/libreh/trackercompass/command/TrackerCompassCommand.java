package me.libreh.trackercompass.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.util.GenericModInfo;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
