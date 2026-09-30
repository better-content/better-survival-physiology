package com.bettercontent.bettersurvivalphysiology.network;

import com.bettercontent.bettersurvivalphysiology.client.MealRecap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MealFeedbackPacket(int changedMask, float[] nutrients, int[] seconds,
                                 boolean sugarChanged, boolean alcoholChanged,
                                 float sugar, float debt, float alcohol,
                                 int sugarSeconds, int debtSeconds, int alcoholSeconds) {
    public static void encode(MealFeedbackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.changedMask);
        for (float value : packet.nutrients) buffer.writeFloat(value);
        for (int value : packet.seconds) buffer.writeVarInt(value);
        buffer.writeBoolean(packet.sugarChanged); buffer.writeBoolean(packet.alcoholChanged);
        buffer.writeFloat(packet.sugar); buffer.writeFloat(packet.debt); buffer.writeFloat(packet.alcohol);
        buffer.writeVarInt(packet.sugarSeconds); buffer.writeVarInt(packet.debtSeconds); buffer.writeVarInt(packet.alcoholSeconds);
    }

    public static MealFeedbackPacket decode(FriendlyByteBuf buffer) {
        int mask = buffer.readVarInt(); float[] nutrients = new float[6]; int[] seconds = new int[6];
        for (int index = 0; index < 6; index++) nutrients[index] = buffer.readFloat();
        for (int index = 0; index < 6; index++) seconds[index] = buffer.readVarInt();
        return new MealFeedbackPacket(mask, nutrients, seconds, buffer.readBoolean(), buffer.readBoolean(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(MealFeedbackPacket packet, Supplier<NetworkEvent.Context> contexts) {
        NetworkEvent.Context context = contexts.get();
        context.enqueueWork(() -> MealRecap.accept(packet));
        context.setPacketHandled(true);
    }
}
