package me.libreh.trackercompass.mixin;

import me.libreh.trackercompass.api.TrackerCompassRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Allay.class)
public class AllayMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void blockTrackerCompassFromAllay(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (TrackerCompassRegistry.isAny(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
