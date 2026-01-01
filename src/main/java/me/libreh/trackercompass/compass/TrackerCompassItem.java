package me.libreh.trackercompass.compass;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TrackerCompassItem {
    public static final String NBT_TRACKER = "trackercompass:tracker";
    public static final String NBT_REMOVE = "trackercompass:remove";
    private static final Style STYLE = Style.EMPTY.withColor(Formatting.LIGHT_PURPLE).withItalic(false);

    public static ItemStack create() {
        ItemStack compass = new ItemStack(Items.COMPASS);
        NbtCompound nbt = new NbtCompound();

        nbt.putBoolean(NBT_REMOVE, true);
        nbt.putBoolean(NBT_TRACKER, true);

        compass.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        compass.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("Tracker Compass").setStyle(STYLE));
        return compass;
    }

    public static boolean isTrackerCompass(ItemStack stack) {
        if (!stack.isOf(Items.COMPASS)) return false;

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return false;

        NbtCompound nbt = customData.copyNbt();
        return nbt.contains(NBT_TRACKER) && nbt.getBoolean(NBT_TRACKER).orElse(false);
    }
}
