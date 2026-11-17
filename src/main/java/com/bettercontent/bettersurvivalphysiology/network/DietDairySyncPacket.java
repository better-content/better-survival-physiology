package com.bettercontent.bettersurvivalphysiology.network;

import com.bettercontent.bettersurvivalphysiology.nutrition.DietDairySplit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Minimal native-derived classification missing from DietGroups.sync: item IDs whose entire
 * native recipe trail is dairy. No food gains, parallel nutrition catalogue or recipe weights. */
public record DietDairySyncPacket(Set<ResourceLocation> itemIds) {
    private static final int MAX_IDS = 4096;

    public DietDairySyncPacket {
        itemIds = Set.copyOf(itemIds);
        if (itemIds.size() > MAX_IDS) throw new IllegalStateException("Too many native dairy-only recipes to sync");
    }

    public static DietDairySyncPacket fromNative() {
        return new DietDairySyncPacket(DietDairySplit.refreshServer());
    }

    public static void encode(DietDairySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.itemIds.size());
        packet.itemIds.stream().sorted().forEach(buffer::writeResourceLocation);
    }

    public static DietDairySyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_IDS) throw new IllegalArgumentException("Invalid native dairy classification count");
        Set<ResourceLocation> ids = new HashSet<>();
        for (int i = 0; i < size; i++)
            if (!ids.add(buffer.readResourceLocation())) throw new IllegalArgumentException("Duplicate native dairy item");
        return new DietDairySyncPacket(ids);
    }

    public static void handle(DietDairySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        if (context.getSender() != null) throw new IllegalArgumentException("Client cannot set native dairy classification");
        context.enqueueWork(() -> DietDairySplit.acceptClient(packet.itemIds()));
        context.setPacketHandled(true);
    }
}
