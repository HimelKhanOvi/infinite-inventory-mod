package com.robsterad.infiniteinv.inventory;

import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public record ItemKey(Item item, ComponentChanges components) {

    public static ItemKey of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return new ItemKey(stack.getItem(), stack.getComponentChanges());
    }

    public ItemStack toStack(int count) {
        ItemStack stack = new ItemStack(this.item, count);
        stack.applyChanges(this.components);
        return stack;
    }
}