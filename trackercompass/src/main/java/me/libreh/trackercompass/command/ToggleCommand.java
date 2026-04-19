package me.libreh.trackercompass.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.libreh.trackercompass.TrackerCompass;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

public class ToggleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"tracker", "compass", "hunt"}) {
            dispatcher.register(Commands.literal(name)
                    .requires(source -> Permissions.check(source, "trackercompass.tracker", true))
                    .executes(ToggleCommand::toggleCompass)
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ToggleCommand::trackPlayer)));
        }
    }

    public static int toggleCompass(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TrackerCompassSavedData data = TrackerCompassSavedData.get(source.getServer());
        UUID playerId = player.getUUID();

        boolean enabled = !Objects.requireNonNullElse(data.getPlayerCompassToggle(playerId),
                ConfigManager.config().giveCompassByDefault);

        data.setPlayerCompassToggle(playerId, enabled);
        TrackerCompass.syncCompass(player);

        source.sendSuccess(() -> Component.literal(enabled
                ? "Tracker compass enabled!"
                : "Tracker compass disabled!")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), false);

        return 1;
    }

    public static int trackPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        ServerPlayer target = EntityArgument.getPlayer(context, "player");

        if (target.getUUID().equals(player.getUUID())) {
            source.sendFailure(Component.literal("You cannot track yourself"));
            return 0;
        }

        TrackerCompassSavedData data = TrackerCompassSavedData.get(source.getServer());
        UUID playerId = player.getUUID();

        data.setTargetPlayer(playerId, target.getUUID());

        boolean compassEnabled = Objects.requireNonNullElse(data.getPlayerCompassToggle(playerId),
                ConfigManager.config().giveCompassByDefault);
        if (!compassEnabled) {
            data.setPlayerCompassToggle(playerId, true);
            TrackerCompass.syncCompass(player);
        }

        String targetName = target.getName().getString();
        source.sendSuccess(() -> Component.literal("Tracking: " + targetName).withStyle(ChatFormatting.GOLD), false);

        return 1;
    }
}
