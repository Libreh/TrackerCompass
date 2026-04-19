package me.libreh.trackercompass.mixin;

import me.libreh.trackercompass.compass.TrackerCompassItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class SlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void blockTrackerCompassInContainers(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!TrackerCompassItem.isTrackerCompass(stack)) return;
        Slot self = (Slot) (Object) this;
        if (!(self.container instanceof Inventory)) {
            cir.setReturnValue(false);
        }
    }
}
