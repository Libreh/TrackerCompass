package me.libreh.trackercompass;

import me.libreh.trackercompass.api.DimensionUtil;
import me.libreh.trackercompass.api.PlayerDimensionPositions;
import me.libreh.trackercompass.api.TargetPlaceholders;
import me.libreh.trackercompass.api.TrackerCompassEvents;
import me.libreh.trackercompass.api.TrackerCompassRegistry;
import me.libreh.trackercompass.api.TrackerCondition;
import me.libreh.trackercompass.api.TrackerTargetResolver;
import me.libreh.trackercompass.api.TrackerText;
import me.libreh.trackercompass.command.ToggleCommand;
import me.libreh.trackercompass.command.TrackerCompassCommand;
import me.libreh.trackercompass.compass.CompassActionBar;
import me.libreh.trackercompass.compass.CompassManager;
import me.libreh.trackercompass.compass.TrackerCompassItem;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.libreh.worldreset.api.PlayerResetEvents;
import me.libreh.trackercompass.gui.TrackerCompassGui;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrackerCompass implements ModInitializer {
	public static final String MOD_ID = "trackercompass";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private int tickCounter = 0;
    private static TrackerCompassSavedData data;
    private static CompassManager compassManager;
    private static CompassActionBar actionBar;
    private static final Map<UUID, TrackerCondition> activeConditions = new HashMap<>();

	@Override
	public void onInitialize() {
        TrackerPredicates.register();
        TargetPlaceholders.register();

        TrackerCompassRegistry.register(TrackerCompassItem::isTrackerCompass);
        TrackerCompassEvents.onHotbarSwitch(TrackerCompass::refreshActionBar);

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerLevelEvents.LOAD.register(this::onWorldLoad);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (compassManager != null) {
                compassManager.syncInventory(handler.player);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (compassManager != null && !alive) {
                compassManager.syncInventory(newPlayer);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (actionBar != null) {
                actionBar.onPlayerDisconnect(handler.player);
            }
            activeConditions.remove(handler.player.getUUID());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TrackerCompassCommand.register(dispatcher);
            ToggleCommand.register(dispatcher);
        });

        UseItemCallback.EVENT.register(this::onItemUse);

        PlayerResetEvents.onAfterPlayerReset(TrackerCompass::syncCompass);
	}

    private void onServerStarted(MinecraftServer server) {
        ConfigManager.setLookup(server.registryAccess());
        ConfigManager.load();

        data = TrackerCompassSavedData.get(server);
        compassManager = new CompassManager(data);
        actionBar = new CompassActionBar();

        LOGGER.info("TrackerCompass data loaded");
    }

    private void onServerStopping(MinecraftServer server) {
        if (data != null) {
            var overworld = server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                overworld.getDataStorage().saveAndJoin();
                LOGGER.info("TrackerCompass data saved");
            }
        }
    }

    private void onWorldLoad(MinecraftServer server, ServerLevel world) {
        if (DimensionUtil.resolve(world) == Level.OVERWORLD) {
            data = TrackerCompassSavedData.get(server);
            compassManager = new CompassManager(data);
            LOGGER.info("TrackerCompass data reloaded after world load");
        }
    }

    private void onServerTick(MinecraftServer server) {
        if (++tickCounter < ConfigManager.config().compassUpdateTicks) {
            return;
        }
        tickCounter = 0;

        List<TrackerCondition> conditions = ConfigManager.config().trackerConditions;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePosition(player);

            var resolveResult = TrackerTargetResolver.resolve(
                player,
                conditions,
                server.getPlayerList().getPlayers(),
                buildPositionLookup(server),
                data::getTargetPlayer
            );

            if (resolveResult.isPresent()) {
                var result = resolveResult.get();
                ServerPlayer target = result.target();
                TrackerCondition condition = result.activeCondition();
                activeConditions.put(player.getUUID(), condition);

                BlockPos targetPos = findTargetPos(target.getUUID(), player, server);
                compassManager.update(player, targetPos);
                actionBar.update(player, server, target.getUUID(), targetPos, condition.actionbarFormat());
            } else {
                UUID storedTarget = data.getTargetPlayer(player.getUUID());
                if (storedTarget != null) {
                    BlockPos targetPos = findTargetPos(storedTarget, player, server);
                    if (targetPos != null) {
                        TrackerCondition condition = conditions.stream()
                            .filter(c -> c.id().equals("selected_target") && c.enabled())
                            .findFirst()
                            .orElse(null);
                        if (condition != null) {
                            compassManager.update(player, targetPos);
                            actionBar.update(player, server, storedTarget, targetPos, condition.actionbarFormat());
                            activeConditions.put(player.getUUID(), condition);
                            continue;
                        }
                    }
                }
                activeConditions.remove(player.getUUID());
                compassManager.update(player, null);
                actionBar.update(player, server, null, null, null);
            }
        }

        data.markDirtyIfNeeded();
    }

    private Map<UUID, java.util.function.Supplier<BlockPos>> buildPositionLookup(MinecraftServer server) {
        Map<UUID, java.util.function.Supplier<BlockPos>> lookup = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            lookup.put(uuid, player::blockPosition);
        }
        return lookup;
    }

    public static void refreshActionBar(ServerPlayer player) {
        if (actionBar == null || data == null) return;
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        TrackerCondition condition = activeConditions.get(player.getUUID());
        UUID targetUuid = data.getTargetPlayer(player.getUUID());
        BlockPos targetPos = targetUuid != null ? findTargetPos(targetUuid, player, server) : null;
        actionBar.update(player, server, targetUuid, targetPos, condition != null ? condition.actionbarFormat() : null);
    }

    private static BlockPos findTargetPos(UUID targetUuid, ServerPlayer observer, MinecraftServer server) {
        ResourceKey<Level> dimension = DimensionUtil.resolve(observer);

        ServerPlayer target = server.getPlayerList().getPlayer(targetUuid);
        if (target != null && DimensionUtil.resolve(target).equals(dimension)) {
            return target.blockPosition();
        }

        PlayerDimensionPositions positions = data.getPlayerDimensionPositions().get(targetUuid);
        return positions != null ? positions.getPosition(dimension) : null;
    }

    private void updatePosition(ServerPlayer player) {
        if (!player.isAlive()) {
            data.clearPlayerPositions(player.getUUID());
        } else {
            data.updatePlayerPosition(player.getUUID(), DimensionUtil.resolve(player), player.blockPosition());
        }
    }

    private InteractionResult onItemUse(Player player, Level level, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);

        if (TrackerCompassItem.isTrackerCompass(stack) && ConfigManager.config().enableTrackerGui) {
            TrackerCompassGui gui = new TrackerCompassGui(serverPlayer);
            gui.open();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static void syncCompass(ServerPlayer player) {
        if (compassManager != null) {
            compassManager.syncInventory(player);
        }
    }
}
