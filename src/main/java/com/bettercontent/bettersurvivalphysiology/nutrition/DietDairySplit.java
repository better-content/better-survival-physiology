package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietResult;
import com.illusivesoulworks.diet.common.util.DietResult;
import com.illusivesoulworks.diet.common.util.DietValueGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Native Diet calculates a two-group serving for dairy. Repartition that *same total* only
 * when its direct tag or entire native recipe trail identifies dairy exclusively. Mixed recipes
 * have set membership, not ingredient weights; retain native shares rather than invent weights. */
public final class DietDairySplit {
    private static final TagKey<Item> DAIRY = source("dairy");
    private static final List<TagKey<Item>> OTHER_SOURCES = List.of(
            source("grains"), source("fruits"), source("sugars"), source("vegetables"));

    // Native DietGroups.sync transmits GENERATED group names, but not DietValueGenerator.TRAILS.
    // The server transmits only this derived yes/no classification; no gains or ingredient weights.
    private static volatile Set<ResourceLocation> clientDerivedDairy = Set.of();
    private static volatile Set<ResourceLocation> serverDerivedDairy = Set.of();
    private static volatile boolean clientReady;

    private DietDairySplit() {}

    /** Rebuild once per native generator reload, from its chosen recipe's GENERATED groups and
     * complete contributing ingredient TRAIL. Other recipe alternatives have no native weights;
     * only the actual native-selected dairy-exclusive trail is eligible. */
    public static Set<ResourceLocation> refreshServer() {
        Set<ResourceLocation> result = DietValueGenerator.getAll().entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(g -> g.getName().equals("proteins"))
                        && entry.getValue().stream().anyMatch(g -> g.getName().equals("carbs")))
                .map(Map.Entry::getKey)
                .filter(item -> !item.getDefaultInstance().is(DAIRY))
                .filter(DietDairySplit::dairyTrail)
                .map(BuiltInRegistries.ITEM::getKey)
                .collect(Collectors.toUnmodifiableSet());
        serverDerivedDairy = result;
        return result;
    }

    public static Set<ResourceLocation> serverDerivedDairy() { return serverDerivedDairy; }

    private static boolean dairyTrail(Item item) {
        List<Item> trail = DietValueGenerator.getTrail(item);
        return trail != null && !trail.isEmpty() && trail.stream().allMatch(sourceItem -> {
            ItemStack source = new ItemStack(sourceItem);
            return source.is(DAIRY) && OTHER_SOURCES.stream().noneMatch(source::is);
        });
    }

    public static void acceptClient(Set<ResourceLocation> ids) {
        Set<ResourceLocation> checked = Set.copyOf(ids);
        for (ResourceLocation id : checked)
            if (BuiltInRegistries.ITEM.getOptional(id).isEmpty())
                throw new IllegalArgumentException("Unknown native dairy-derived item " + id);
        clientDerivedDairy = checked;
        clientReady = true;
    }

    public static void clearClient() {
        clientReady = false;
        clientDerivedDairy = Set.of();
    }

    public static boolean clientReady() { return clientReady; }

    public static IDietResult repartition(Player player, List<ItemStack> ingredients, IDietResult nativeResult) {
        return repartition(ingredients, nativeResult, player.level().isClientSide());
    }

    /** Explicit client-side switch is also used by nonshipping native packet-boundary coverage. */
    public static IDietResult repartition(List<ItemStack> ingredients, IDietResult nativeResult, boolean clientSide) {
        return repartition(ingredients, nativeResult, clientSide,
                SalienceConfig.value(SalienceConfig.DAIRY_PROTEIN_SHARE, .80));
    }

    /** Explicit share is a pure nonshipping assertion seam for alternate server configuration. */
    public static IDietResult repartition(List<ItemStack> ingredients, IDietResult nativeResult,
                                          boolean clientSide, double share) {
        if (!Double.isFinite(share) || share < 0 || share > 1)
            throw new IllegalArgumentException("Invalid native dairy share");
        if (ingredients.isEmpty() || !ingredients.stream().allMatch(stack -> onlyDairy(stack, clientSide)))
            return nativeResult;
        Map<IDietGroup, Float> nativeGains = nativeResult.get();
        IDietGroup protein = null, carbs = null;
        for (IDietGroup group : nativeGains.keySet()) {
            if (group.getName().equals("proteins")) protein = group;
            if (group.getName().equals("carbs")) carbs = group;
        }
        if (protein == null || carbs == null) return nativeResult;
        float oldProtein = nativeGains.get(protein), oldCarbs = nativeGains.get(carbs);
        if (!Float.isFinite(oldProtein) || !Float.isFinite(oldCarbs) || oldProtein < 0 || oldCarbs < 0)
            throw new IllegalStateException("Invalid native dairy dose");
        double total = (double) oldProtein + oldCarbs;
        Map<IDietGroup, Float> adjusted = new HashMap<>(nativeGains);
        float proteinDose = (float) (total * share);
        adjusted.put(protein, proteinDose);
        // Assign the remainder, rather than rounding each contribution up independently.
        adjusted.put(carbs, (float) (total - proteinDose));
        return new DietResult(adjusted);
    }

    private static boolean onlyDairy(ItemStack stack, boolean clientSide) {
        if (stack.isEmpty()) return false;
        if (stack.is(DAIRY)) return OTHER_SOURCES.stream().noneMatch(stack::is);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return clientSide ? clientReady && clientDerivedDairy.contains(id) : serverDerivedDairy.contains(id);
    }

    private static TagKey<Item> source(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("diet", name));
    }
}
