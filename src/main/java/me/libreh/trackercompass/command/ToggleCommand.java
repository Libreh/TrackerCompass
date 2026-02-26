package me.libreh.trackercompass.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.libreh.trackercompass.TrackerCompass;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ToggleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> trackerCommandNode = dispatcher.register(Commands.literal("tracker")
                .requires(source -> Permissions.check(source, "trackercompass.tracker", true))
                .executes(ToggleCommand::toggleCompass)
        );
        dispatcher.register(Commands.literal("compass").redirect(trackerCommandNode));
        dispatcher.register(Commands.literal("hunt").redirect(trackerCommandNode));
    }

    public static int toggleCompass(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }

        TrackerCompassSavedData data = TrackerCompassSavedData.get(source.getServer());
        UUID playerId = player.getUUID();

        Boolean current = data.getPlayerCompassToggle(playerId);
        boolean enabled = current == null || !current;

        data.setPlayerCompassToggle(playerId, enabled);
        TrackerCompass.syncCompass(player);

        source.sendSuccess(() -> Component.literal(enabled
                ? "Tracker compass enabled!"
                : "Tracker compass disabled!")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), false);

        return 1;
    }
}
