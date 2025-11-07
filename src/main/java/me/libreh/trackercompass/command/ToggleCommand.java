package me.libreh.trackercompass.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.libreh.trackercompass.TrackerCompass;
import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ToggleCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> trackerCommandNode = dispatcher.register(CommandManager.literal("tracker")
                .requires(source -> Permissions.check(source, "manhunt.commands.tracker", true))
                .executes(ToggleCommand::toggleOwnCompass)
        );
        dispatcher.register(CommandManager.literal("compass").redirect(trackerCommandNode));
        dispatcher.register(CommandManager.literal("hunt").redirect(trackerCommandNode));
    }

    private static int toggleOwnCompass(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("This command can only be used by players"));
            return 0;
        }

        TrackerCompassPersistentState persistentState = TrackerCompassPersistentState.get(source.getServer());
        UUID playerId = player.getUuid();

        Boolean currentToggle = persistentState.getPlayerCompassToggle(playerId);
        boolean newState = currentToggle == null || !currentToggle;

        persistentState.setPlayerCompassToggle(playerId, newState);

        TrackerCompass.updatePlayerCompassImmediate(player);

        if (newState) {
            source.sendFeedback(() -> Text.literal("Tracker compass enabled!")
                    .formatted(Formatting.GREEN), false);
        } else {
            source.sendFeedback(() -> Text.literal("Tracker compass disabled!")
                    .formatted(Formatting.RED), false);
        }

        return 1;
    }
}
