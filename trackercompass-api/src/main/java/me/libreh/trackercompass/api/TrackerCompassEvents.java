package me.libreh.trackercompass.api;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class TrackerCompassEvents {
    private static final List<Consumer<ServerPlayer>> HOTBAR_SWITCH = new CopyOnWriteArrayList<>();

    private TrackerCompassEvents() {}

    public static void onHotbarSwitch(Consumer<ServerPlayer> listener) {
        HOTBAR_SWITCH.add(listener);
    }

    public static void fireHotbarSwitch(ServerPlayer player) {
        for (Consumer<ServerPlayer> l : HOTBAR_SWITCH) l.accept(player);
    }
}
