package me.libreh.trackercompass;

import me.libreh.trackercompass.command.ToggleCommand;
import me.libreh.trackercompass.command.TrackerCompassCommand;
import me.libreh.trackercompass.compass.CompassActionBar;
import me.libreh.trackercompass.compass.CompassManager;
import me.libreh.trackercompass.compass.TrackerCompassItem;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.libreh.trackercompass.gui.TrackerCompassGui;
import me.libreh.trackercompass.util.GenericModInfo;
import me.libreh.trackercompass.util.PlayerDimensionUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TrackerCompass implements ModInitializer {
	public static final String MOD_ID = "trackercompass";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModContainer CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).get();
    private int tickCounter = 0;
    private TrackerCompassSavedData data;
    private static CompassManager compassManager;
    private CompassActionBar actionBar;

	@Override
	public void onInitialize() {
        GenericModInfo.build(CONTAINER, MOD_ID, LOGGER, true, true, 0xFF80EA);

        ConfigManager.load();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerWorldEvents.LOAD.register(this::onWorldLoad);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (compassManager != null) {
                compassManager.syncInventory(handler.player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (actionBar != null) {
                actionBar.onPlayerDisconnect(handler.player);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TrackerCompassCommand.register(dispatcher);
            ToggleCommand.register(dispatcher);
        });

        UseItemCallback.EVENT.register(this::onItemUse);
	}

    private void onServerStarted(MinecraftServer server) {
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
        if (world.dimension() == Level.OVERWORLD) {
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

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePosition(player);

            UUID targetUuid = data.getTargetPlayer(player.getUUID());
            BlockPos targetPos = targetUuid != null
                ? PlayerDimensionUtil.findPlayerInDimension(targetUuid, player, server, data)
                : null;

            compassManager.update(player, targetPos);
            actionBar.update(player, server, targetUuid, targetPos);
        }

        data.markDirtyIfNeeded();
    }

    private void updatePosition(ServerPlayer player) {
        if (!player.isAlive()) {
            data.clearPlayerPositions(player.getUUID());
        } else {
            data.updatePlayerPosition(player.getUUID(), player.level().dimension(), player.blockPosition());
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