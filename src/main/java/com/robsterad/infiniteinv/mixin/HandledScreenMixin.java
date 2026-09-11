package com.robsterad.infiniteinv.mixin;

import com.robsterad.infiniteinv.gui.InfiniteInventoryOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class, priority = 999)
public class HandledScreenMixin {

    @Inject(method = "render", at = @At("TAIL"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            InfiniteInventoryOverlay.renderOverlay(screen, guiGraphics, mouseX, mouseY, delta);
        } catch (Throwable t) {
            // Defensive execution to prevent game crashes
        }
    }
}
