package me.libreh.trackercompass.mixin;

import me.libreh.trackercompass.api.TrackerCompassRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public class ItemFrameMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void blockTrackerCompassInItemFrame(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (TrackerCompassRegistry.isAny(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
