package me.libreh.trackercompass.mixin;

import me.libreh.trackercompass.compass.TrackerCompassItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void dropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> ci) {
        if (!TrackerCompassItem.isTrackerCompass(stack)) return;
        ServerPlayer self = (ServerPlayer) (Object) this;
        ci.cancel();
        Inventory inv = self.getInventory();
        // Vanilla already cleared the selected slot, so put the compass back there.
        // Fall back to any free slot if something else took it.
        int slot = inv.getItem(inv.getSelectedSlot()).isEmpty() ? inv.getSelectedSlot() : inv.getFreeSlot();
        if (slot != -1) {
            inv.setItem(slot, stack.copy());
            self.connection.send(inv.createInventoryUpdatePacket(slot));
        }
    }
}