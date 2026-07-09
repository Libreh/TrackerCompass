package me.libreh.trackercompass.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.libreh.trackercompass.config.ConfigManager;
import me.libreh.trackercompass.data.TrackerCompassSavedData;
import me.libreh.trackercompass.compass.TrackerPlaceholders;
import me.libreh.trackercompass.api.TrackerText;
import net.minecraft.core.BlockPos;
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
import java.util.Map;
import java.util.UUID;

public class TrackerCompassGui extends SimpleGui {
    private final ServerPlayer player;

    public TrackerCompassGui(ServerPlayer player) {
        super(calculateMenuType(player), player, false);
        this.player = player;

        TrackerText titleTemplate = TrackerText.of(ConfigManager.config().guiTitle);
        Component resolvedTitle = titleTemplate.resolve(Map.of()).copy().withStyle(s -> s.withItalic(false));
        this.setTitle(resolvedTitle);

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

        if (ConfigManager.config().showOfflinePlayers) {
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
        ServerPlayer targetPlayer = player.level().getServer().getPlayerList().getPlayer(targetUuid);
        BlockPos targetPos = targetPlayer != null ? targetPlayer.blockPosition() : null;

        Map<String, Component> placeholders = TrackerPlaceholders.build(player, targetPlayer, targetPos, targetUuid, player.level().getServer());

        TrackerText titleTemplate = TrackerText.of(ConfigManager.config().playerTitle);
        placeholders.put("player", Component.literal(playerName));
        Component title = titleTemplate.resolve(placeholders).copy().withStyle(s -> s.withItalic(false));
        item.set(DataComponents.CUSTOM_NAME, title);

        item.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(playerName));
        item.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.PROFILE, true));

        List<Component> lore = new ArrayList<>();
        for (String loreLine : ConfigManager.config().playerLoreLines) {
            if (loreLine.isEmpty()) continue;

            TrackerText parsed = TrackerText.of(loreLine);
            if (!parsed.isEmpty()) {
                Component resolved = parsed.resolve(placeholders).copy().withStyle(s -> s.withItalic(false));
                if (!resolved.getString().isEmpty()) {
                    lore.add(resolved);
                }
            }
        }

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
    public boolean onClick(int index, ClickType type, net.minecraft.world.inventory.ContainerInput action, GuiElement element) {
        if (index >= 0 && index < this.getSize()) {

            List<UUID> selectablePlayers = getAllSelectablePlayers(player);

            if (index < selectablePlayers.size()) {
                UUID selectedUuid = selectablePlayers.get(index);
                String selectedName = getPlayerName(selectedUuid);

                TrackerCompassSavedData.get(player.level().getServer())
                        .setTargetPlayer(player.getUUID(), selectedUuid);

                player.sendSystemMessage(Component.literal("Tracking: " + selectedName).withStyle(net.minecraft.ChatFormatting.GOLD), false);

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
