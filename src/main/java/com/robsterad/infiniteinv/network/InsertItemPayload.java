package com.robsterad.infiniteinv.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record InsertItemPayload(ItemStack stack, int slotId, boolean single) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InsertItemPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("infinite-inventory", "insert_item"));

    @Override
    public CustomPacketPayload.Type<InsertItemPayload> type() {
        return ID;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, InsertItemPayload> CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, InsertItemPayload::stack,
            ByteBufCodecs.VAR_INT, InsertItemPayload::slotId,
            ByteBufCodecs.BOOL, InsertItemPayload::single,
            InsertItemPayload::new
    );
}
