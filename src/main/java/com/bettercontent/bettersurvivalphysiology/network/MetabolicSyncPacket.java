package com.bettercontent.bettersurvivalphysiology.network;

import com.bettercontent.bettersurvivalphysiology.client.ClientMetabolicState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MetabolicSyncPacket(
        float proteins, float grains, float fruits, float fats, float vegetables, float dairy,
        float sugar, float alcohol, float ordinary, float prepared, float feast,
        int flags, int workSequence,
        int proteinSeconds, int grainSeconds, int fruitSeconds, int fatSeconds, int vegetableSeconds, int dairySeconds,
        int sugarSeconds, int alcoholSeconds
) {
    public static final MetabolicSyncPacket EMPTY = new MetabolicSyncPacket(
            0, 0, 0, 0, 0, 0, 0, 0, .2f, .4f, .8f, 0, 0,
            -1, -1, -1, -1, -1, -1, -1, -1);

    public static void encode(MetabolicSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.proteins); buffer.writeFloat(packet.grains); buffer.writeFloat(packet.fruits);
        buffer.writeFloat(packet.fats); buffer.writeFloat(packet.vegetables); buffer.writeFloat(packet.dairy);
        buffer.writeFloat(packet.sugar); buffer.writeFloat(packet.alcohol);
        buffer.writeFloat(packet.ordinary); buffer.writeFloat(packet.prepared); buffer.writeFloat(packet.feast);
        buffer.writeVarInt(packet.flags); buffer.writeVarInt(packet.workSequence);
        buffer.writeVarInt(packet.proteinSeconds); buffer.writeVarInt(packet.grainSeconds); buffer.writeVarInt(packet.fruitSeconds);
        buffer.writeVarInt(packet.fatSeconds); buffer.writeVarInt(packet.vegetableSeconds); buffer.writeVarInt(packet.dairySeconds);
        buffer.writeVarInt(packet.sugarSeconds); buffer.writeVarInt(packet.alcoholSeconds);
    }

    public static MetabolicSyncPacket decode(FriendlyByteBuf buffer) {
        return new MetabolicSyncPacket(
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(MetabolicSyncPacket packet, Supplier<NetworkEvent.Context> contexts) {
        NetworkEvent.Context context = contexts.get();
        context.enqueueWork(() -> ClientMetabolicState.accept(packet));
        context.setPacketHandled(true);
    }

    public float nutrient(int index) { return new float[]{proteins, grains, fruits, fats, vegetables, dairy}[index]; }
    public int nutrientSeconds(int index) { return new int[]{proteinSeconds, grainSeconds, fruitSeconds, fatSeconds, vegetableSeconds, dairySeconds}[index]; }
}
