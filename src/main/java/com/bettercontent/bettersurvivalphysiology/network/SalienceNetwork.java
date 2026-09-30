package com.bettercontent.bettersurvivalphysiology.network;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.bettercontent.bettersurvivalphysiology.presentation.PresentationSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SalienceNetwork {
    private static final String PROTOCOL = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SystemicSalienceMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private SalienceNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(0, MetabolicSyncPacket.class,
                MetabolicSyncPacket::encode, MetabolicSyncPacket::decode, MetabolicSyncPacket::handle);
        CHANNEL.registerMessage(1, MealFeedbackPacket.class,
                MealFeedbackPacket::encode, MealFeedbackPacket::decode, MealFeedbackPacket::handle);
    }

    public static void sync(ServerPlayer player, NutritionSnapshot nutrition, MetabolicState state) {
        PresentationSnapshot presentation = PresentationSnapshot.create(player, nutrition, state);
        int[] seconds = presentation.nutrientSeconds();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MetabolicSyncPacket(
                nutrition.proteins(), nutrition.grains(), nutrition.fruits(), nutrition.fats(), nutrition.vegetables(), nutrition.dairy(),
                (float) state.sugar, (float) state.alcohol,
                presentation.ordinary(), presentation.prepared(), presentation.feast(), presentation.flags(), presentation.workSequence(),
                seconds[0], seconds[1], seconds[2], seconds[3], seconds[4], seconds[5],
                presentation.sugarSeconds(), presentation.alcoholSeconds()
        ));
    }

    public static void meal(ServerPlayer player, MealFeedbackPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
