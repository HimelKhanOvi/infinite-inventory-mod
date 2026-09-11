package com.robsterad.infiniteinv.mixin;

import com.robsterad.infiniteinv.gui.InfiniteInventoryOverlay;
import com.robsterad.infiniteinv.network.DepositItemPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ScreenMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        InfiniteInventoryOverlay.renderOverlay(screen, guiGraphics, mouseX, mouseY, delta, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void onSlotClicked(Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        // Send packet to deposit item on shift-click
        if (type == ClickType.QUICK_MOVE && slot != null && !slot.getItem().isEmpty()) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && slot.container == client.player.getInventory()) {
                ItemStack stack = slot.getItem().copy();
                if (!stack.isEmpty()) {
                    ClientPlayNetworking.send(new DepositItemPayload(stack));
                }
            }
        }
    }
}
