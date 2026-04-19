package me.libreh.trackercompass.compass;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class TrackerCompassItem {
    public static final String NBT_TRACKER = "trackercompass:tracker";
    private static final Style STYLE = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(false);

    public static ItemStack create() {
        ItemStack compass = new ItemStack(Items.COMPASS);
        CompoundTag tag = new CompoundTag();

        tag.putBoolean(NBT_TRACKER, true);

        compass.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        compass.set(DataComponents.CUSTOM_NAME,
                Component.literal("Tracker Compass").setStyle(STYLE));
        return compass;
    }

    public static boolean isTrackerCompass(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) return false;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;

        CompoundTag tag = customData.copyTag();
        return tag.contains(NBT_TRACKER) && tag.getBoolean(NBT_TRACKER).orElse(false);
    }
}
