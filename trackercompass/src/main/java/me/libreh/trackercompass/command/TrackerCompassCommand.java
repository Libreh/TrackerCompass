package me.libreh.trackercompass.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.libreh.trackercompass.config.ConfigManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;

public class TrackerCompassCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trackercompass")
                .requires(source -> Permissions.check(source, "trackercompass.tracker", true))
                .executes(ToggleCommand::toggleCompass)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ToggleCommand::trackPlayer))
                .then(Commands.literal("reload")
                        .requires(Permissions.require("trackercompass.reload", PermissionLevel.ADMINS))
                        .executes(TrackerCompassCommand::reloadConfig)
                )
        );
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
