package com.robsterad.infiniteinv.mixin;

import com.robsterad.infiniteinv.gui.InfiniteInventoryOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, priority = 1000)
public abstract class HandledScreenMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet.minecraft.client.gui.screens.inventory.AbstractContainerScreen;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            shift = At.Shift.BEFORE
        )
    )
    private void onRenderOverlayBeforeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        InfiniteInventoryOverlay.renderOverlay(screen, guiGraphics, mouseX, mouseY, delta, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }
}
