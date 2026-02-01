package me.libreh.trackercompass.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.libreh.trackercompass.util.PlayerDimensionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrackerCompassGui extends SimpleGui {
    private final ServerPlayer player;

    public TrackerCompassGui(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.player = player;
        this.setTitle(Component.literal("Track Player").withStyle(ChatFormatting.AQUA));

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

        for (ServerPlayer onlinePlayer : player.level().getServer().getPlayerList().getPlayers()) {
            if (!onlinePlayer.getUUID().equals(player.getUUID())) {
                playerUuids.add(onlinePlayer.getUUID());
            }
        }

        if (ConfigManager.getConfig().showOfflinePlayersInGui) {
            TrackerCompassSavedData persistentState = TrackerCompassSavedData.get(player.level().getServer());

            for (UUID offlineUuid : persistentState.getPlayerDimensionPositions().keySet()) {
                if (!playerUuids.contains(offlineUuid) && !offlineUuid.equals(player.getUUID())) {
                    playerUuids.add(offlineUuid);
                }
            }
        }

        return playerUuids;
    }

    private ItemStack createPlayerItem(UUID targetUuid) {
        ItemStack item = new ItemStack(Items.PLAYER_HEAD);

        String playerName = getPlayerName(targetUuid);
        item.set(DataComponents.CUSTOM_NAME,
                Component.literal(playerName).withStyle(s -> s.withItalic(false).withColor(ChatFormatting.AQUA)));

        item.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(playerName));
        item.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.PROFILE, true));

        List<Component> lore = new ArrayList<>();

        ServerPlayer targetPlayer = player.level().getServer().getPlayerList().getPlayer(targetUuid);
        boolean isOnline = targetPlayer != null;

        if (ConfigManager.getConfig().showOfflinePlayersInGui) {
            ChatFormatting formatting = isOnline ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            lore.add(Component.literal("Status: " + (isOnline ? "Online" : "Offline")).withStyle(s -> s.withColor(formatting).withItalic(false)));
        }

        if (isOnline) {
            if (ConfigManager.getConfig().showDimension) {
                String dimension = PlayerDimensionUtil.getName(targetPlayer);
                lore.add(Component.literal("Dimension: " + dimension).withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false)));
            }

            if (ConfigManager.getConfig().showDistance && targetPlayer.level() == this.player.level()) {
                double distance = this.player.position().distanceTo(targetPlayer.position());
                lore.add(Component.literal("Distance: " + (int)distance + "m").withStyle(s -> s.withColor(ChatFormatting.GOLD).withItalic(false)));
            }
        }

        lore.add(Component.empty());
        lore.add(Component.literal("Click to track this target").withStyle(s -> s.withColor(ChatFormatting.GREEN).withItalic(false)));

        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }

    private String getPlayerName(UUID uuid) {
        ServerPlayer onlinePlayer = this.player.level().getServer().getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName().getString();
        }

        return this.player.level().getServer().services().nameToIdCache()
                .get(uuid)
                .map(NameAndId::name)
                .orElse("Unknown Player");
    }

    @Override
    public boolean onClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action, GuiElementInterface element) {
        if (index >= 0 && index < 45) {

            List<UUID> selectablePlayers = getAllSelectablePlayers();

            if (index < selectablePlayers.size()) {
                UUID selectedUuid = selectablePlayers.get(index);
                String selectedName = getPlayerName(selectedUuid);

                TrackerCompassSavedData.get(player.level().getServer())
                        .setTargetPlayer(player.getUUID(), selectedUuid);

                player.sendSystemMessage(Component.literal("Tracking: " + selectedName).withStyle(ChatFormatting.GOLD), false);

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
