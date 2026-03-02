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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class TrackerCompassGui extends SimpleGui {
    private final ServerPlayer player;

    public TrackerCompassGui(ServerPlayer player) {
        super(calculateMenuType(player), player, false);
        this.player = player;
        this.setTitle(Component.literal("Track Player"));

        populateGui();
    }

    private static MenuType<?> calculateMenuType(ServerPlayer player) {
        int count = getAllSelectablePlayers(player).size();
        int rows = Math.min(6, Math.max(1, (count + 8) / 9));

        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private void populateGui() {
        this.clearGui();

        List<UUID> allPlayerUuids = getAllSelectablePlayers(player);

        int slot = 0;
        int maxSlots = this.getSize();
        for (UUID uuid : allPlayerUuids) {
            if (slot >= maxSlots) break;
            this.setSlot(slot++, createPlayerItem(uuid));
        }
    }

    private static List<UUID> getAllSelectablePlayers(ServerPlayer player) {
        LinkedHashSet<UUID> uuids = new LinkedHashSet<>();

        for (ServerPlayer onlinePlayer : player.level().getServer().getPlayerList().getPlayers()) {
            if (!onlinePlayer.getUUID().equals(player.getUUID())) {
                uuids.add(onlinePlayer.getUUID());
            }
        }

        if (ConfigManager.config().showOfflinePlayersInGui) {
            for (UUID offlineUuid : TrackerCompassSavedData.get(player.level().getServer()).getPlayerDimensionPositions().keySet()) {
                if (!offlineUuid.equals(player.getUUID())) {
                    uuids.add(offlineUuid);
                }
            }
        }

        List<UUID> sorted = new ArrayList<>(uuids);
        sorted.sort(Comparator.comparing(uuid -> getPlayerName(player, uuid)));
        return sorted;
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

        if (ConfigManager.config().showOfflinePlayersInGui) {
            ChatFormatting formatting = isOnline ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            lore.add(Component.literal("Status: " + (isOnline ? "Online" : "Offline")).withStyle(s -> s.withColor(formatting).withItalic(false)));
        }

        if (isOnline) {
            if (ConfigManager.config().showDimension) {
                String dimension = PlayerDimensionUtil.getName(targetPlayer);
                lore.add(Component.literal("Dimension: " + dimension).withStyle(s -> s.withColor(ChatFormatting.YELLOW).withItalic(false)));
            }

            if (ConfigManager.config().showDistance && targetPlayer.level() == this.player.level()) {
                double distance = this.player.position().distanceTo(targetPlayer.position());
                lore.add(Component.literal("Distance: " + (int)distance + "m").withStyle(s -> s.withColor(ChatFormatting.GOLD).withItalic(false)));
            }
        }

        lore.add(Component.literal("Click to track").withStyle(s -> s.withColor(ChatFormatting.BLUE).withItalic(false)));

        item.set(DataComponents.LORE, new ItemLore(lore));
        return item;
    }

    private String getPlayerName(UUID uuid) {
        return getPlayerName(player, uuid);
    }

    private static String getPlayerName(ServerPlayer player, UUID uuid) {
        ServerPlayer onlinePlayer = player.level().getServer().getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName().getString();
        }

        return player.level().getServer().services().nameToIdCache()
                .get(uuid)
                .map(NameAndId::name)
                .orElse("Unknown Player");
    }

    @Override
    public boolean onClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action, GuiElementInterface element) {
        if (index >= 0 && index < this.getSize()) {

            List<UUID> selectablePlayers = getAllSelectablePlayers(player);

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
