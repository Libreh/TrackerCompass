package me.libreh.trackercompass.mixin;

import me.libreh.trackercompass.api.TrackerCompassRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCost.class)
public class ItemCostMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void blockTrackerCompassAsTradePayment(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (TrackerCompassRegistry.isAny(itemStack)) {
            cir.setReturnValue(false);
        }
    }
}
