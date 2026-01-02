package me.libreh.trackercompass;

import me.libreh.trackercompass.command.ToggleCommand;
import me.libreh.trackercompass.command.TrackerCompassCommand;
import me.libreh.trackercompass.compass.CompassActionBar;
import me.libreh.trackercompass.compass.CompassManager;
import me.libreh.trackercompass.compass.TrackerCompassItem;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import me.libreh.trackercompass.gui.TrackerCompassGui;
import me.libreh.trackercompass.tracking.PlayerPositionTracker;
import me.libreh.trackercompass.util.GenericModInfo;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackerCompass implements ModInitializer {
	public static final String MOD_ID = "trackercompass";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModContainer CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).get();

    private int ticksSinceLastUpdate = 0;
    private TrackerCompassPersistentState persistentState;

    private PlayerPositionTracker positionTracker;
    private static CompassManager compassManager;
    private CompassActionBar compassActionBar;

	@Override
	public void onInitialize() {
        GenericModInfo.build(CONTAINER, MOD_ID, LOGGER, true, true, 0xFF80EA);

        if (!ConfigManager.load()) {
            LOGGER.warn("Failed to load config, using defaults");
        }

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (compassActionBar != null) {
                compassActionBar.onPlayerDisconnect(handler.player);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TrackerCompassCommand.register(dispatcher);
            ToggleCommand.register(dispatcher);
        });

        UseItemCallback.EVENT.register(this::onItemUse);
	}

    private void onServerStarted(MinecraftServer server) {
        persistentState = TrackerCompassPersistentState.get(server);

        positionTracker = new PlayerPositionTracker(persistentState);
        compassManager = new CompassManager(persistentState);
        compassActionBar = new CompassActionBar(persistentState);

        LOGGER.info("TrackerCompass persistent state loaded");
    }

    private void onServerStopping(MinecraftServer server) {
        if (persistentState != null) {
            var overworld = server.getWorld(World.OVERWORLD);
            if (overworld != null) {
                overworld.getPersistentStateManager().save();
                LOGGER.info("TrackerCompass persistent state saved");
            }
        }
    }

    private void onServerTick(MinecraftServer server) {
        positionTracker.updateAllPositions(server);

        ticksSinceLastUpdate++;
        if (ticksSinceLastUpdate >= ConfigManager.getConfig().compassUpdateTicks) {
            ticksSinceLastUpdate = 0;
            compassManager.updateAllCompasses(server);
            compassActionBar.updateActionBars(server);
        }
    }

    private ActionResult onItemUse(PlayerEntity player, World world, Hand hand) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);

        if (TrackerCompassItem.isTrackerCompass(stack) && ConfigManager.getConfig().enableTrackerGui) {
            TrackerCompassGui gui = new TrackerCompassGui(serverPlayer);
            gui.open();
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    public static void updatePlayerCompassImmediate(ServerPlayerEntity player) {
        if (compassManager != null) {
            compassManager.updatePlayerCompassImmediate(player);
        }
    }
}