package com.robsterad.infiniteinv.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Identifier;
import net.minecraft.world.item.ItemStack;

public record DepositItemPayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<DepositItemPayload> ID = new Type<>(Identifier.of("infiniteinv", "deposit_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositItemPayload> CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC, DepositItemPayload::stack,
        DepositItemPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
