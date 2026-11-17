package com.bettercontent.bettersurvivalphysiology.network;

import com.bettercontent.bettersurvivalphysiology.client.ClientMetabolicState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Read-only presentation of five native tracker values, not a second authoritative balance. */
public record MetabolicSyncPacket(
        float proteins, float carbs, float spice, float vegetables, float alcohol,
        float ordinary, float prepared, float feast, int flags, int workSequence,
        int proteinSeconds, int carbSeconds, int spiceSeconds, int vegetableSeconds, int alcoholSeconds
) {
    public static final MetabolicSyncPacket EMPTY = new MetabolicSyncPacket(
            0, 0, 0, 0, 0, .2f, .4f, .8f, 0, 0, -1, -1, -1, -1, -1);

    public static void encode(MetabolicSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.proteins); buffer.writeFloat(packet.carbs); buffer.writeFloat(packet.spice);
        buffer.writeFloat(packet.vegetables); buffer.writeFloat(packet.alcohol);
        buffer.writeFloat(packet.ordinary); buffer.writeFloat(packet.prepared); buffer.writeFloat(packet.feast);
        buffer.writeVarInt(packet.flags); buffer.writeVarInt(packet.workSequence);
        buffer.writeVarInt(packet.proteinSeconds); buffer.writeVarInt(packet.carbSeconds);
        buffer.writeVarInt(packet.spiceSeconds); buffer.writeVarInt(packet.vegetableSeconds);
        buffer.writeVarInt(packet.alcoholSeconds);
    }

    public static MetabolicSyncPacket decode(FriendlyByteBuf buffer) {
        return new MetabolicSyncPacket(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(MetabolicSyncPacket packet, Supplier<NetworkEvent.Context> contexts) {
        NetworkEvent.Context context = contexts.get();
        context.enqueueWork(() -> ClientMetabolicState.accept(packet));
        context.setPacketHandled(true);
    }

    public float nutrient(int index) { return new float[]{proteins, carbs, spice, vegetables, alcohol}[index]; }
    public int nutrientSeconds(int index) { return new int[]{proteinSeconds, carbSeconds, spiceSeconds, vegetableSeconds, alcoholSeconds}[index]; }
}
