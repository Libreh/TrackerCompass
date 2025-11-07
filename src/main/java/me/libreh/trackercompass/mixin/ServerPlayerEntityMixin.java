package me.libreh.trackercompass.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void dropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> ci) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return;
        var nbtData = customData.copyNbt();
        if (nbtData.getBoolean("Remove").isEmpty()) return;

        if (nbtData.getBoolean("Remove").get()) {
            ci.cancel();
        }
    }
}