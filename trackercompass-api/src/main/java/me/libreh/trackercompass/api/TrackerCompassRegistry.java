package me.libreh.trackercompass.api;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class TrackerCompassRegistry {
    private static final List<Predicate<ItemStack>> PREDICATES = new CopyOnWriteArrayList<>();

    private TrackerCompassRegistry() {}

    public static void register(Predicate<ItemStack> predicate) {
        PREDICATES.add(predicate);
    }

    public static boolean isAny(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Predicate<ItemStack> p : PREDICATES) {
            if (p.test(stack)) return true;
        }
        return false;
    }
}
