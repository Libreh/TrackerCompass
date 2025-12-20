package me.libreh.trackercompass;

import me.libreh.trackercompass.command.ToggleCommand;
import me.libreh.trackercompass.command.TrackerCompassCommand;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import me.libreh.trackercompass.gui.TrackerCompassGui;
import me.libreh.trackercompass.tracking.PlayerDimensionPositions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TrackerCompass implements ModInitializer {
	public static final String MOD_ID = "trackercompass";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int ticksSinceLastUpdate = 0;
    private static TrackerCompassPersistentState persistentState;
    private final Map<UUID, Boolean> wasHoldingCompass = new HashMap<>();

	@Override
	public void onInitialize() {
        if (!ConfigManager.load()) {
            LOGGER.warn("Failed to load config, using defaults");
        }

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> wasHoldingCompass.remove(handler.player.getUuid()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TrackerCompassCommand.register(dispatcher);
            ToggleCommand.register(dispatcher);
        });

        UseItemCallback.EVENT.register(this::onItemUse);
	}

    private void onServerStarted(MinecraftServer server) {
        persistentState = TrackerCompassPersistentState.get(server);
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
        updatePositions(server);

        ticksSinceLastUpdate++;
        if (ticksSinceLastUpdate >= ConfigManager.getConfig().compassUpdateTicks) {
            ticksSinceLastUpdate = 0;
            updateAllTrackerCompasses(server);
        }
    }

    private ActionResult onItemUse(PlayerEntity player, World world, Hand hand) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);

        if (isTrackerCompass(stack) && ConfigManager.getConfig().enableTrackerGui) {
            TrackerCompassGui gui = new TrackerCompassGui(serverPlayer);
            gui.open();
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private void updatePositions(MinecraftServer server) {
        if (persistentState == null) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isDead()) {
                clearPlayerPosition(player.getUuid());
                continue;
            }

            UUID playerId = player.getUuid();
            RegistryKey<World> dimension = player.getEntityWorld().getRegistryKey();
            BlockPos position = player.getBlockPos();

            persistentState.updatePlayerPosition(playerId, dimension, position);
        }
    }

    private void clearPlayerPosition(UUID playerId) {
        if (persistentState != null) {
            persistentState.clearPlayerPositions(playerId);
        }
    }

    private void updateAllTrackerCompasses(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (shouldPlayerHaveCompass(player)) {
                ensurePlayerHasOneTrackerCompass(player);
            } else {
                removeAllTrackerCompasses(player);
            }
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            updatePlayerCompass(player, server);
        }

        updateActionBars(server);
    }

    public static void updatePlayerCompassImmediate(ServerPlayerEntity player) {
        if (shouldPlayerHaveCompass(player)) {
            ensurePlayerHasOneTrackerCompass(player);
        } else {
            removeAllTrackerCompasses(player);
        }
    }

    private static boolean shouldPlayerHaveCompass(ServerPlayerEntity player) {
        if (persistentState == null) {
            return ConfigManager.getConfig().giveCompassByDefault;
        }

        Boolean playerToggle = persistentState.getPlayerCompassToggle(player.getUuid());
        if (playerToggle != null) {
            return playerToggle;
        }

        return ConfigManager.getConfig().giveCompassByDefault;
    }

    private static void removeAllTrackerCompasses(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isTrackerCompass(stack)) {
                player.getInventory().removeStack(i);
            }
        }
    }

    private static void ensurePlayerHasOneTrackerCompass(ServerPlayerEntity player) {
        int trackerSlot = -1;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isTrackerCompass(stack)) {
                if (trackerSlot == -1) {
                    trackerSlot = i;
                } else {
                    player.getInventory().removeStack(i);
                }
            }
        }

        if (trackerSlot == -1) {
            ItemStack trackerCompass = createTrackerCompass();
            player.getInventory().insertStack(trackerCompass);
        }
    }

    private static void updatePlayerCompass(ServerPlayerEntity player, MinecraftServer server) {
        UUID targetUuid = persistentState.getTargetPlayer(player.getUuid());
        if (targetUuid == null) {
            return;
        }

        BlockPos targetPos = findSpecificPlayerInObserverDimension(targetUuid, player, server);
        if (targetPos == null) {
            return;
        }

        RegistryKey<World> observerDimension = player.getEntityWorld().getRegistryKey();
        updateTrackerCompassesInInventory(player, targetPos, observerDimension);
    }

    private static BlockPos findSpecificPlayerInObserverDimension(UUID targetUuid, ServerPlayerEntity observer, MinecraftServer server) {
        if (persistentState == null) {
            return null;
        }

        RegistryKey<World> observerDimension = observer.getEntityWorld().getRegistryKey();

        ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.getEntityWorld().getRegistryKey().equals(observerDimension)) {
            return targetPlayer.getBlockPos();
        }

        PlayerDimensionPositions dimPositions = persistentState.getPlayerDimensionPositions().get(targetUuid);
        if (dimPositions == null) {
            return null;
        }

        return dimPositions.getPosition(observerDimension);
    }

    private static void updateTrackerCompassesInInventory(ServerPlayerEntity player, BlockPos targetPos, RegistryKey<World> dimension) {
        GlobalPos globalPos = GlobalPos.create(dimension, targetPos);
        LodestoneTrackerComponent tracker = new LodestoneTrackerComponent(Optional.of(globalPos), false);

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isTrackerCompass(stack)) {
                stack.set(DataComponentTypes.LODESTONE_TRACKER, tracker);
            }
        }
    }

    private static boolean isTrackerCompass(ItemStack stack) {
        if (!stack.isOf(Items.COMPASS)) return false;

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return false;

        NbtCompound nbt = customData.copyNbt();
        return nbt.contains("Tracker") && nbt.getBoolean("Tracker").orElse(false);
    }

    private boolean isPlayerHoldingTrackerCompass(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        return isTrackerCompass(mainHand) || isTrackerCompass(offHand);
    }

    private static ItemStack createTrackerCompass() {
        ItemStack compass = new ItemStack(Items.COMPASS);
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("Remove", true);
        nbt.putBoolean("Tracker", true);
        compass.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return compass;
    }

    private void updateActionBars(MinecraftServer server) {
        if (!ConfigManager.getConfig().actionBarInfo) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            boolean currentlyHolding = isPlayerHoldingTrackerCompass(player);
            Boolean wasHolding = wasHoldingCompass.get(playerId);

            wasHoldingCompass.put(playerId, currentlyHolding);

            if (ConfigManager.getConfig().onlyShowWhenHoldingCompass) {
                if (wasHolding != null && wasHolding && !currentlyHolding) {
                    player.sendMessage(Text.empty(), true);
                    continue;
                }
            }

            if (ConfigManager.getConfig().onlyShowWhenHoldingCompass) {
                if (!currentlyHolding) {
                    continue;
                }
            }

            UUID targetUuid = persistentState.getTargetPlayer(player.getUuid());
            if (targetUuid == null) {
                continue;
            }

            Text actionBarText = getActionBarText(player, targetUuid, server);
            if (actionBarText != null) {
                player.sendMessage(actionBarText, true);
            }
        }
    }

    private Text getActionBarText(ServerPlayerEntity player, UUID targetUuid, MinecraftServer server) {
        BlockPos targetPos = findSpecificPlayerInObserverDimension(targetUuid, player, server);
        if (targetPos == null) {
            return null;
        }

        double distance = Math.sqrt(player.getBlockPos().getSquaredDistance(targetPos));

        String arrow = "";
        if (ConfigManager.getConfig().showDirectionArrow) {
            arrow = getDirectionArrow(player, targetPos) + " ";
        }

        String targetName;
        ServerPlayerEntity targetPlayerEntity = server.getPlayerManager().getPlayer(targetUuid);

        if (targetPlayerEntity != null) {
            targetName = targetPlayerEntity.getName().getString();

            if (ConfigManager.getConfig().showStatusIndicators &&
                !targetPlayerEntity.getEntityWorld().getRegistryKey().equals(player.getEntityWorld().getRegistryKey())) {
                targetName += " (Portal)";
            }
        } else {
            Optional<PlayerConfigEntry> playerConfigEntry = server.getApiServices().nameToIdCache().getByUuid(targetUuid);

            if (playerConfigEntry.isPresent()) {
                targetName = playerConfigEntry.get().name();
            } else {
                targetName = "Player";
            }

            if (ConfigManager.getConfig().showStatusIndicators) {
                targetName += " (Offline)";
            }
        }

        String actionBarFormat;
        if (ConfigManager.getConfig().showDistance) {
            actionBarFormat = String.format("%s%dm [%s]", arrow, (int)distance, targetName);
        } else {
            actionBarFormat = String.format("%s[%s]", arrow, targetName);
        }

        return Text.literal(actionBarFormat).formatted(Formatting.AQUA);
    }

    private String getDirectionArrow(ServerPlayerEntity player, BlockPos target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - player.getYaw() - 90;

        angle = (angle + 360) % 360;

        if (angle >= 337.5 || angle < 22.5) return "↑";
        if (angle >= 22.5 && angle < 67.5) return "↗";
        if (angle >= 67.5 && angle < 112.5) return "→";
        if (angle >= 112.5 && angle < 157.5) return "↘";
        if (angle >= 157.5 && angle < 202.5) return "↓";
        if (angle >= 202.5 && angle < 247.5) return "↙";
        if (angle >= 247.5 && angle < 292.5) return "←";
        return "↖";
    }
}