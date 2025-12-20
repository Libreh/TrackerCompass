package me.libreh.trackercompass.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassPersistentState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrackerCompassGui extends SimpleGui {
    private final ServerPlayerEntity player;

    public TrackerCompassGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X6, player, false);
        this.player = player;
        this.setTitle(Text.literal("Track Player").formatted(Formatting.AQUA));

        populateGui();
    }

    private void populateGui() {
        this.clearGui();

        List<UUID> allPlayerUuids = getAllSelectablePlayers();

        int slot = 0;
        for (UUID uuid : allPlayerUuids) {
            if (slot >= 45) break;
            this.setSlot(slot++, createPlayerItem(uuid));
        }
    }

    private List<UUID> getAllSelectablePlayers() {
        List<UUID> playerUuids = new ArrayList<>();

        for (ServerPlayerEntity onlinePlayer : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            if (!onlinePlayer.getUuid().equals(player.getUuid())) {
                playerUuids.add(onlinePlayer.getUuid());
            }
        }

        if (ConfigManager.getConfig().showOfflinePlayersInGui) {
            TrackerCompassPersistentState persistentState = TrackerCompassPersistentState.get(player.getEntityWorld().getServer());

            for (UUID offlineUuid : persistentState.getPlayerDimensionPositions().keySet()) {
                if (!playerUuids.contains(offlineUuid) && !offlineUuid.equals(player.getUuid())) {
                    playerUuids.add(offlineUuid);
                }
            }
        }

        return playerUuids;
    }

    private ItemStack createPlayerItem(UUID targetUuid) {
        ItemStack item = new ItemStack(Items.PLAYER_HEAD);

        String playerName = getPlayerName(targetUuid);
        item.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(playerName).styled(s -> s.withItalic(false).withFormatting(Formatting.AQUA)));

        List<Text> lore = new ArrayList<>();

        ServerPlayerEntity targetPlayer = this.player.getEntityWorld().getServer().getPlayerManager().getPlayer(targetUuid);
        boolean isOnline = targetPlayer != null;
        Formatting formatting = isOnline ? Formatting.GREEN : Formatting.GRAY;
        lore.add(Text.literal("Status: " + (isOnline ? "Online" : "Offline")).styled(s -> s.withFormatting(formatting).withItalic(false)));

        if (isOnline) {
            String dimension = getDimensionName(targetPlayer);
            lore.add(Text.literal("Dimension: " + dimension).styled(s -> s.withFormatting(Formatting.YELLOW).withItalic(false)));

            if (targetPlayer.getEntityWorld() == this.player.getEntityWorld()) {
                double distance = this.player.getEntityPos().distanceTo(targetPlayer.getEntityPos());
                lore.add(Text.literal("Distance: " + (int)distance + "m").styled(s -> s.withFormatting(Formatting.GOLD).withItalic(false)));
            }
        }

        lore.add(Text.empty());
        lore.add(Text.literal("Click to track this target").styled(s -> s.withFormatting(Formatting.GREEN).withItalic(false)));

        item.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return item;
    }

    private String getPlayerName(UUID uuid) {
        ServerPlayerEntity onlinePlayer = this.player.getEntityWorld().getServer().getPlayerManager().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName().getString();
        }

        return this.player.getEntityWorld().getServer().getApiServices().nameToIdCache()
                .getByUuid(uuid)
                .map(PlayerConfigEntry::name)
                .orElse("Unknown Player");
    }

    private String getDimensionName(ServerPlayerEntity player) {
        if (player.getEntityWorld().getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
            return "Overworld";
        } else if (player.getEntityWorld().getRegistryKey() == net.minecraft.world.World.NETHER) {
            return "Nether";
        } else if (player.getEntityWorld().getRegistryKey() == net.minecraft.world.World.END) {
            return "End";
        }
        return "Unknown";
    }

    @Override
    public boolean onClick(int index, ClickType type, SlotActionType action, GuiElementInterface element) {
        if (index >= 0 && index < 45) {

            List<UUID> selectablePlayers = getAllSelectablePlayers();

            if (index < selectablePlayers.size()) {
                UUID selectedUuid = selectablePlayers.get(index);
                String selectedName = getPlayerName(selectedUuid);

                TrackerCompassPersistentState.get(player.getEntityWorld().getServer())
                        .setTargetPlayer(player.getUuid(), selectedUuid);

                player.sendMessage(Text.literal("Tracking: " + selectedName).formatted(Formatting.GOLD), false);

                this.close();
                return true;
            }
        }

        return super.onClick(index, type, action, element);
    }

    private void clearGui() {
        for (int i = 0; i < this.getSize(); i++) {
            this.clearSlot(i);
        }
    }
}
