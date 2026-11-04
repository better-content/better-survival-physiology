package com.bettercontent.bettersurvivalphysiology.presentation;

import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Diet owns the visible roster, translated name, order, colour and actual player values. */
public final class DietPresentation {
    private DietPresentation() {}

    public record Group(String id, Component name, float value, int color, AspectIdentity aspect) {}

    /** Empty until both Diet's suite data and its player capability have been synced. */
    public static Optional<List<Group>> groups(Player player) {
        if (player == null) return Optional.empty();
        return DietBridge.tracker(player).flatMap(tracker ->
                DietSuites.getSuite(player.level(), tracker.getSuite()).flatMap(suite -> {
                    var values = tracker.getValues();
                    if (suite.getGroups().isEmpty() || suite.getGroups().stream().anyMatch(g -> !values.containsKey(g.getName())))
                        return Optional.empty();
                    return Optional.of(suite.getGroups().stream()
                            .sorted(Comparator.comparingInt(com.illusivesoulworks.diet.api.type.IDietGroup::getOrder)
                                    .thenComparing(com.illusivesoulworks.diet.api.type.IDietGroup::getName))
                            .map(group -> new Group(group.getName(),
                                    Component.translatable("groups.diet." + group.getName() + ".name"),
                                    values.get(group.getName()), group.getColor().getRGB(),
                                    AspectIdentity.fromGroupName(group.getName())))
                            .toList());
                }));
    }
}
