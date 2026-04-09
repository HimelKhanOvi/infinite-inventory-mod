package com.robsterad.infiniteinv.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ExtractItemPayload(ItemStack stackToMatch) implements CustomPayload {

    public static final CustomPayload.Id<ExtractItemPayload> ID = new CustomPayload.Id<>(Identifier.of("infinite-inventory", "extract_item"));

    @Override
    public CustomPayload.Id<ExtractItemPayload> getId() {
        return ID;
    }

    public static final PacketCodec<RegistryByteBuf, ExtractItemPayload> CODEC = PacketCodec.tuple(
            ItemStack.PACKET_CODEC, ExtractItemPayload::stackToMatch,
            ExtractItemPayload::new
    );

}