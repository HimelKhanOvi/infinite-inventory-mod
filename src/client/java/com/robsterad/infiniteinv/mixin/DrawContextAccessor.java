package com.robsterad.infiniteinv.mixin;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DrawContext.class)
public interface DrawContextAccessor {
    @Accessor("tooltipDrawer")
    @Nullable Runnable getTooltipDrawer();

    @Accessor("tooltipDrawer")
    void setTooltipDrawer(@Nullable Runnable drawer);
}