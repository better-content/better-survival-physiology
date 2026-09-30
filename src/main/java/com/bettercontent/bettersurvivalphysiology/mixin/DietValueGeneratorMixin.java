package com.bettercontent.bettersurvivalphysiology.mixin;

import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.util.DietValueGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Diet normally stops recipe inheritance as soon as an output has one direct group. */
@Mixin(value = DietValueGenerator.class, remap = false)
public abstract class DietValueGeneratorMixin {
    @Shadow @Final private static Map<Item, Set<IDietGroup>> GENERATED;

    @Inject(method = "reload", at = @At("TAIL"))
    private static void betterContent$includeAllIngredients(MinecraftServer server, CallbackInfo ci) {
        Set<IDietGroup> groups = DietGroups.getGroups(server.overworld());
        Map<Item, Set<IDietGroup>> lineage = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            Set<IDietGroup> direct = new HashSet<>();
            ItemStack stack = new ItemStack(item);
            for (IDietGroup group : groups) if (group.contains(stack)) direct.add(group);
            direct.addAll(GENERATED.getOrDefault(item, Set.of()));
            if (!direct.isEmpty()) lineage.put(item, direct);
        }
        List<Recipe<?>> recipes = new ArrayList<>(server.getRecipeManager().getRecipes());
        // A bounded fixed point carries ingredient groups through doughs, sauces, and other
        // non-edible intermediates. Alternatives contribute only groups shared by every option.
        for (int pass = 0; pass < 12; pass++) {
            boolean changed = false;
            for (Recipe<?> recipe : recipes) {
                ItemStack output = recipe.getResultItem(server.registryAccess());
                if (output.isEmpty()) continue;
                Set<IDietGroup> inherited = new HashSet<>();
                for (Ingredient ingredient : recipe.getIngredients()) {
                    ItemStack[] options = ingredient.getItems();
                    if (options.length == 0) continue;
                    Set<IDietGroup> shared = null;
                    for (ItemStack option : options) {
                        Set<IDietGroup> candidate = lineage.getOrDefault(option.getItem(), Set.of());
                        if (shared == null) shared = new HashSet<>(candidate);
                        else shared.retainAll(candidate);
                    }
                    if (shared != null) inherited.addAll(shared);
                }
                if (inherited.isEmpty()) continue;
                if (lineage.computeIfAbsent(output.getItem(), ignored -> new HashSet<>()).addAll(inherited)) changed = true;
            }
            if (!changed) break;
        }
        for (Map.Entry<Item, Set<IDietGroup>> entry : lineage.entrySet()) {
            if (!new ItemStack(entry.getKey()).isEdible()) continue;
            GENERATED.computeIfAbsent(entry.getKey(), ignored -> new HashSet<>()).addAll(entry.getValue());
        }
    }
}
