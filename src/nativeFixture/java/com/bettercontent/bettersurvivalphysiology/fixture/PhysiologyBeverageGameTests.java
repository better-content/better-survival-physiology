package com.bettercontent.bettersurvivalphysiology.fixture;

import com.bettercontent.betterdrinkingwater.WaterBottleCurio;
import com.bettercontent.betterdrinkingwater.api.BeverageEvents;
import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.illusivesoulworks.diet.api.DietApi;
import com.mojang.authlib.GameProfile;
import dev.ghen.thirst.foundation.common.capability.ModCapabilities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/** Nonshipping real Curios debit/release tests; no copied engine or synthetic whole-item finish. */
@GameTestHolder(SystemicSalienceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhysiologyBeverageGameTests {
    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void milkBucketAwardsOnlyItsFractionDespiteNectar(GameTestHelper helper) {
        var player = player(helper);
        MetabolicStateStore.get(player).sugar = .8;
        source(player, new ItemStack(Items.MILK_BUCKET));
        sip(player, 1);
        CompoundTag reserve = reserve(player);
        int hydration = reserve.getInt("thirst");
        helper.assertTrue(hydration > 1, "Real milk purchase must retain a fractional reserve");
        var profile = reserve.getCompound("beverage").getCompound("nutrition").getCompound("diet");
        helper.assertTrue(profile.getAllKeys().size() == 2, "Milk replaces lookup with exactly dairy and fats");
        near(helper, profile.getDouble("dairy"), .06, "Whole milk Cream profile");
        near(helper, DietBridge.snapshot(player).dairy(), .06 / hydration, "Partial milk Cream without whole/native double award");
        near(helper, DietBridge.snapshot(player).fats(), .06 / hydration, "Partial milk Tallow not Nectar-amplified");
        // Persisted reserve, not a fresh source lookup, drives every subsequent natural sip.
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted.copy());
        for (int index = 1; index < hydration; index++) sip(player, 1);
        near(helper, DietBridge.snapshot(player).dairy(), .06, "Partitioned complete milk Cream");
        near(helper, DietBridge.snapshot(player).fats(), .06, "Partitioned complete milk Tallow");
        helper.assertTrue(reserve(player).isEmpty(), "Completed paid serving leaves no reserve");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void milkBottleUsesNativeDietServingRatherThanBucketOverride(GameTestHelper helper) {
        var player = player(helper);
        var stack = item("farmersdelight:milk_bottle");
        var nativeGains = DietApi.getInstance().get(player, stack).get();
        helper.assertTrue(!nativeGains.isEmpty(), "Native milk bottle must have real Diet gains");
        source(player, stack);
        sip(player, 1);
        var reserve = reserve(player);
        var profile = reserve.getCompound("beverage").getCompound("nutrition").getCompound("diet");
        for (var group : NutritionSnapshot.Group.values()) {
            double expected = nativeGains.entrySet().stream().filter(e -> e.getKey().getName().equals(group.id))
                    .mapToDouble(e -> e.getValue().doubleValue()).sum();
            near(helper, profile.getDouble(group.id), expected, "Bottle equals native Diet lookup for " + group.id);
            near(helper, DietBridge.snapshot(player).actual(group), expected / reserve.getInt("thirst"),
                    "Bottle fractional " + group.id);
        }
        for (int index = 1; index < reserve.getInt("thirst"); index++) sip(player, 1);
        helper.assertTrue(DietBridge.tracker(player).orElseThrow().getEaten().contains(stack.getItem()),
                "Native Diet discovery recorded at complete milk bottle, even though it is not edible");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void realHoneyAndBeerScaleExistingLoadsOnceAcrossPersistence(GameTestHelper helper) {
        for (String id : new String[]{"minecraft:honey_bottle", "brewinandchewin:beer"}) {
            var player = player(helper);
            var stack = item(id);
            var state = MetabolicStateStore.get(player);
            double sugar = ConsumableProfiles.servingSugar(player, stack), alcohol = ConsumableProfiles.alcohol(stack);
            helper.assertTrue(sugar > 0 || alcohol > 0, "Real source must exercise metabolic dose: " + id);
            source(player, stack);
            player.getFoodData().setFoodLevel(10);
            sip(player, 1);
            var reserve = reserve(player);
            int hydration = reserve.getInt("thirst");
            near(helper, state.sugar, sugar / hydration, "Partial Nectar " + id);
            near(helper, state.alcohol, alcohol / hydration, "Partial Booze " + id);
            var nutrition = reserve.getCompound("beverage").getCompound("nutrition").copy();
            // Explicit native NBT save/read boundary, leaving the source slot empty after upfront payment.
            var encoded = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, encoded.copy());
            for (int index = 1; index < hydration; index++) sip(player, 1);
            near(helper, state.sugar, sugar, "Whole Nectar exactly once " + id);
            near(helper, state.alcohol, alcohol, "Whole Booze exactly once " + id);
            for (var group : NutritionSnapshot.Group.values())
                near(helper, DietBridge.snapshot(player).actual(group), nutrition.getCompound("diet")
                        .getDouble(group.id), "Whole native Diet entitlement exactly once " + id + "/" + group.id);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void nectarProfilesUsePotentialNativeGainsAndPreservePaidEntitlements(GameTestHelper helper) {
        for (String id : new String[]{"minecraft:sweet_berries", "farmersdelight:chocolate_pie_slice",
                "minecraft:honey_bottle", "brewinandchewin:beer"}) {
            var player = player(helper);
            var stack = item(id);
            var nativeGains = ConsumableProfiles.dietGains(player, stack).orElseThrow();
            double smallest = Double.POSITIVE_INFINITY;
            for (var group : NutritionSnapshot.Group.values()) {
                double gain = nativeGains.getOrDefault(group.id, 0.0);
                if (Double.isFinite(gain) && gain > 0) smallest = Math.min(smallest, gain);
            }
            double expected = Math.min(ConsumableProfiles.baseSugar(stack), smallest * .5);
            var nutrition = new CompoundTag();
            MinecraftForge.EVENT_BUS.post(new BeverageEvents.Profile(player, stack, nutrition));
            near(helper, nutrition.getDouble("sugar"), expected, "Captured capped whole-serving Nectar " + id);
            near(helper, ConsumableProfiles.servingSugar(player, stack), expected, "Direct resolver agrees " + id);
            for (var group : NutritionSnapshot.Group.values())
                DietBridge.tracker(player).orElseThrow().setValue(group.id, .999f);
            near(helper, ConsumableProfiles.servingSugar(player, stack), expected, "Current capacity cannot change dose " + id);
            var full = new CompoundTag();
            MinecraftForge.EVENT_BUS.post(new BeverageEvents.Profile(player, stack, full));
            near(helper, full.getDouble("sugar"), expected, "Full tracker profile uses potential gains " + id);
        }
        // A legacy paid payload is an entitlement, not a fresh dose lookup.
        var player = player(helper);
        var paid = new CompoundTag();
        paid.putDouble("sugar", .22);
        paid.putString("item", "minecraft:sweet_berries");
        CompoundTag persisted = paid.copy();
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, persisted, 0, .25));
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, persisted, .25, 1));
        near(helper, MetabolicStateStore.get(player).sugar, .22, "Stored legacy dose is not recapped per sip");
        helper.assertTrue(paid.equals(persisted), "Paid payload remains unchanged");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void edibleServingRecoversOnceOnlyAtCompletion(GameTestHelper helper) {
        var player = player(helper);
        DietBridge.tracker(player).orElseThrow().setValue("dairy", .5f);
        var state = MetabolicStateStore.get(player);
        state.sugar = .2;
        player.setHealth(4);
        source(player, item("minecraft:honey_bottle"));
        sip(player, 1);
        var reserve = reserve(player);
        helper.assertTrue(reserve.getCompound("beverage").getCompound("nutrition").getBoolean("edible"),
                "Native honey bottle edible context retained");
        near(helper, player.getHealth(), 4, "Partial sip is not a whole Cream meal");
        helper.assertTrue(state.mealRecoveryCooldown == 0, "Partial sip cannot consume meal cooldown");
        for (int index = 1; index < reserve.getInt("thirst"); index++) sip(player, 1);
        near(helper, player.getHealth(), 4 + 2 * MetabolicMath.amplification(state.sugar), "One completed edible meal recovery");
        helper.assertTrue(state.mealRecoveryCooldown > 0, "Completion starts existing recovery cooldown");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void blackoutAndMalformedFractionsReleaseNoPhysiologyEntitlement(GameTestHelper helper) {
        var player = player(helper);
        var state = MetabolicStateStore.get(player);
        source(player, new ItemStack(Items.MILK_BUCKET));
        state.addAlcohol(1);
        sip(player, 1);
        helper.assertTrue(source(player).is(Items.MILK_BUCKET) && reserve(player).isEmpty(),
                "Real physiology Check cancels prepayment without consuming source");
        state.alcohol = 0; state.blackout = false;
        sip(player, 1);
        var nutrition = reserve(player).getCompound("beverage").getCompound("nutrition");
        var paid = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
        double dairy = DietBridge.snapshot(player).dairy();
        state.addAlcohol(1);
        sip(player, 1);
        helper.assertTrue(paid.equals(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)),
                "Real physiology Check preserves already-paid reserve bytes");
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, nutrition, 0, .5));
        near(helper, DietBridge.snapshot(player).dairy(), dairy, "Defensive blackout Sip guard");
        state.alcohol = 0; state.blackout = false;
        for (double[] range : new double[][]{{0, 0}, {.5, .4}, {-.1, .5}, {0, 1.1}, {0, Double.NaN}})
            MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, nutrition, range[0], range[1]));
        near(helper, DietBridge.snapshot(player).dairy(), dairy, "Invalid intervals cannot award native Diet");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void nectarAcceleratesNativeThirstExhaustionAndDepletion(GameTestHelper helper) {
        var player = player(helper);
        var state = MetabolicStateStore.get(player);
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        float input = .1f;
        thirst.setExhaustion(0);
        thirst.addExhaustion(player, input);
        float baseline = thirst.getExhaustion();
        helper.assertTrue(baseline > 0 && Float.isFinite(baseline), "Native fixture must admit ordinary thirst exhaustion");
        for (double nectar : new double[]{0, .25, .5, 1}) {
            state.sugar = nectar;
            thirst.setExhaustion(0);
            thirst.addExhaustion(player, input);
            near(helper, thirst.getExhaustion(), baseline * Math.pow(8, nectar),
                    "Native thirst hook applies Nectar surcharge at load " + nectar);
        }
        // Tallow and its emergency reserve reduce ordinary costs, not Nectar's extra cost.
        DietBridge.tracker(player).orElseThrow().setValue("fats", 1);
        state.sugar = 1;
        state.enduranceReserveTicks = 200;
        thirst.setExhaustion(0);
        thirst.addExhaustion(player, input);
        near(helper, thirst.getExhaustion(), baseline * 7, "Emergency reserve cannot erase Nectar thirst surcharge");
        DietBridge.tracker(player).orElseThrow().setValue("fats", 0);
        state.enduranceReserveTicks = 0;
        int pulses = (int) Math.floor(4 / (baseline * 8)) + 1;
        helper.assertTrue(pulses > 0 && pulses < 1000, "Bounded native exhaustion-threshold workload");
        for (double nectar : new double[]{0, 1}) {
            state.sugar = nectar;
            thirst.setExhaustion(0);
            thirst.setThirst(20);
            thirst.setQuenched(0);
            for (int i = 0; i < pulses; i++) {
                thirst.addExhaustion(player, input);
                thirst.tick(player);
            }
            helper.assertTrue(nectar == 0 ? thirst.getThirst() == 20 : thirst.getThirst() < 20,
                    "Identical native cost pulses must consume hydration sooner with Nectar, load=" + nectar);
        }
        helper.succeed();
    }

    private static void source(ServerPlayer player, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).resolve().orElseThrow().getStacksHandler("water").orElseThrow()
                .getStacks().setStackInSlot(0, stack.copy());
    }

    private static ItemStack source(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).resolve().orElseThrow().getStacksHandler("water").orElseThrow()
                .getStacks().getStackInSlot(0);
    }

    private static CompoundTag reserve(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("better_drinking_water:reserve");
    }

    private static void sip(ServerPlayer player, int missing) {
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        thirst.setThirst(20 - missing);
        thirst.setQuenched(0);
        player.tickCount += 10 - player.tickCount % 10;
        WaterBottleCurio.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "physiology-test"));
        var diet = DietBridge.tracker(player).orElseThrow();
        diet.initSuite();
        for (var group : NutritionSnapshot.Group.values()) diet.setValue(group.id, 0);
        if (!diet.isActive()) throw new AssertionError("Native Diet fixture tracker must be active");
        return player;
    }

    private static ItemStack item(String id) {
        var key = new ResourceLocation(id);
        if (!ForgeRegistries.ITEMS.containsKey(key)) throw new AssertionError("Missing required native item " + id);
        return new ItemStack(ForgeRegistries.ITEMS.getValue(key));
    }

    private static void near(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.000002, message + ": expected " + expected + ", got " + actual);
    }
}
