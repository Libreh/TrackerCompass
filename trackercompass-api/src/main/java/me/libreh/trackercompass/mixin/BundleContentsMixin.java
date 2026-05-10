package me.libreh.trackercompass.mixin;

import me.libreh.trackercompass.api.TrackerCompassRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleContents.class)
public class BundleContentsMixin {
    @Inject(method = "canItemBeInBundle", at = @At("RETURN"), cancellable = true)
    private static void blockTrackerCompassInBundle(ItemStack itemToAdd, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && TrackerCompassRegistry.isAny(itemToAdd)) {
            cir.setReturnValue(false);
        }
    }
}
