package com.robsterad.infiniteinv.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

public record SyncInventoryPayload(List<NetworkItemData> items) implements CustomPayload {

    public static final CustomPayload.Id<SyncInventoryPayload> ID = new CustomPayload.Id<>(Identifier.of("infinite-inventory", "sync_inventory"));

    public record NetworkItemData(ItemStack stack, long count, long timestamp) {
        public static final PacketCodec<RegistryByteBuf, NetworkItemData> CODEC = PacketCodec.tuple(
                ItemStack.PACKET_CODEC, NetworkItemData::stack,
                PacketCodecs.LONG, NetworkItemData::count,
                PacketCodecs.LONG, NetworkItemData::timestamp,
                NetworkItemData::new
        );
    }

    public static final PacketCodec<RegistryByteBuf, SyncInventoryPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, NetworkItemData.CODEC), SyncInventoryPayload::items,
            SyncInventoryPayload::new
    );


    @Override
    public CustomPayload.Id<SyncInventoryPayload> getId() {
        return ID;
    }
}