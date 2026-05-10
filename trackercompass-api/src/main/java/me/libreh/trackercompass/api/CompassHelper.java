package me.libreh.trackercompass.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CompassHelper {
    public static void pointToTarget(ServerPlayer player, BlockPos targetPos, Predicate<ItemStack> isCompass) {
        pointToTarget(player, DimensionUtil.resolve(player), targetPos, isCompass);
    }

    // Pass explicit dimension when client-visible dimension differs from server's
    // (e.g. dimensions spoofing), else LodestoneTracker spins.
    public static void pointToTarget(ServerPlayer player, ResourceKey<Level> dimension, BlockPos targetPos, Predicate<ItemStack> isCompass) {
        GlobalPos globalPos = GlobalPos.of(dimension, targetPos);
        LodestoneTracker tracker = new LodestoneTracker(Optional.of(globalPos), false);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isCompass.test(stack)) {
                stack.set(DataComponents.LODESTONE_TRACKER, tracker);
            }
        }
    }

    public static void giveCompass(ServerPlayer player, Supplier<ItemStack> compassFactory, Predicate<ItemStack> isCompass) {
        boolean carriedIsCompass = isCompass.test(player.containerMenu.getCarried());

        // Keep the first tracker compass and drop any extras.
        int found = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isCompass.test(stack)) {
                if (found == -1) {
                    found = i;
                } else {
                    player.getInventory().removeItemNoUpdate(i);
                }
            }
        }

        if (found == -1 && !carriedIsCompass) {
            player.getInventory().add(compassFactory.get());
        }
    }

    public static void removeCompasses(ServerPlayer player, Predicate<ItemStack> isCompass) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isCompass.test(stack)) {
                player.getInventory().removeItemNoUpdate(i);
            }
        }
    }

    public static boolean isHoldingCompass(ServerPlayer player, Predicate<ItemStack> isCompass) {
        return isCompass.test(player.getMainHandItem()) || isCompass.test(player.getOffhandItem());
    }

    private CompassHelper() {}
}
