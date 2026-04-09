package com.robsterad.infiniteinv.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record InsertItemPayload(int slotId) implements CustomPayload {
    public static final CustomPayload.Id<InsertItemPayload> ID = new CustomPayload.Id<>(Identifier.of("infinite-inventory", "insert_item"));

    @Override
    public CustomPayload.Id<InsertItemPayload> getId() {
        return ID;
    }


    public static final PacketCodec<RegistryByteBuf, InsertItemPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, InsertItemPayload::slotId,
            InsertItemPayload::new
    );


}