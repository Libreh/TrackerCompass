package me.libreh.trackercompass.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.util.GenericModInfo;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

public class TrackerCompassCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trackercompass")
                .executes(TrackerCompassCommand::about)
                .then(Commands.literal("reload")
                        .requires(Permissions.require("manhunt.commands.reload", PermissionLevel.ADMINS))
                        .executes(TrackerCompassCommand::reloadConfig)
                )
        );
    }

    private static int about(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        for (var text : source.getEntity() instanceof ServerPlayer ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            source.sendSuccess(() -> text, false);
        }

        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!ConfigManager.load()) {
            source.sendFailure(Component.literal("Failed to reload config. Check server logs for details."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Config reloaded successfully!"), true);
        return 1;
    }
}
