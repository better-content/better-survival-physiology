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
        DietBridge.tracker(player).orElseThrow().setValue("carbs", .8f);
        source(player, new ItemStack(Items.MILK_BUCKET));
        sip(player, 1);
        CompoundTag reserve = reserve(player);
        int hydration = reserve.getInt("thirst");
        helper.assertTrue(hydration > 1, "Real milk purchase must retain a fractional reserve");
        var profile = reserve.getCompound("beverage").getCompound("nutrition").getCompound("diet");
        helper.assertTrue(profile.getAllKeys().size() == 2, "Milk has exactly native Protein and Carbs");
        near(helper, profile.getDouble("proteins"), .048, "Whole milk Protein 80%");
        near(helper, profile.getDouble("carbs"), .012, "Whole milk Carbs 20%");
        near(helper, DietBridge.snapshot(player).proteins(), .048 / hydration, "Partial milk Protein without duplicate native award");
        near(helper, DietBridge.snapshot(player).carbs(), .8 + .012 / hydration, "Partial milk Carb despite loaded tracker");
        // Persisted reserve, not a fresh source lookup, drives every subsequent natural sip.
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted.copy());
        for (int index = 1; index < hydration; index++) sip(player, 1);
        near(helper, DietBridge.snapshot(player).proteins(), .048, "Partitioned complete milk Protein");
        near(helper, DietBridge.snapshot(player).carbs(), .812, "Partitioned complete milk Carbs");
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
            var nativeGains = ConsumableProfiles.dietGains(player, stack).orElseThrow();
            double carbs = nativeGains.getOrDefault("carbs", 0.0), alcohol = ConsumableProfiles.alcohol(stack);
            helper.assertTrue(carbs > 0 || alcohol > 0, "Real source must exercise native Carbs or paid Alcohol: " + id);
            source(player, stack);
            player.getFoodData().setFoodLevel(10);
            sip(player, 1);
            var reserve = reserve(player);
            int hydration = reserve.getInt("thirst");
            near(helper, DietBridge.snapshot(player).carbs(), carbs / hydration, "Partial native Carbs " + id);
            near(helper, DietBridge.snapshot(player).alcohol(), alcohol / hydration, "Partial paid Alcohol " + id);
            var nutrition = reserve.getCompound("beverage").getCompound("nutrition").copy();
            // Explicit native NBT save/read boundary, leaving the source slot empty after upfront payment.
            var encoded = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, encoded.copy());
            for (int index = 1; index < hydration; index++) sip(player, 1);
            near(helper, DietBridge.snapshot(player).carbs(), carbs, "Whole native Carbs exactly once " + id);
            near(helper, DietBridge.snapshot(player).alcohol(), alcohol, "Whole paid Alcohol exactly once " + id);
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
            double expected = ConsumableProfiles.dietGains(player, stack).orElseThrow().getOrDefault("carbs", 0.0);
            var profile = new CompoundTag();
            MinecraftForge.EVENT_BUS.post(new BeverageEvents.Profile(player, stack, profile));
            near(helper, profile.getDouble("sugar"), 0, "No mirrored sugar in paid drink " + id);
            near(helper, profile.getCompound("diet").getDouble("carbs"), expected, "Captured native Carb entitlement " + id);
            for (var group : NutritionSnapshot.Group.values())
                DietBridge.tracker(player).orElseThrow().setValue(group.id, .999f);
            var full = new CompoundTag();
            MinecraftForge.EVENT_BUS.post(new BeverageEvents.Profile(player, stack, full));
            near(helper, full.getCompound("diet").getDouble("carbs"), expected,
                    "Full tracker cannot reprice paid potential Carb gain " + id);
        }
        // Saved paid canonical payload, never a fresh lookup or mirrored scalar.
        var player = player(helper);
        var paid = new CompoundTag();
        paid.putDouble("sugar", 0);
        var diet = new CompoundTag(); diet.putDouble("carbs", .22);
        paid.put("diet", diet); paid.putString("item", "minecraft:sweet_berries");
        CompoundTag persisted = paid.copy();
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, persisted, 0, .25));
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, persisted, .25, 1));
        near(helper, DietBridge.snapshot(player).carbs(), .22, "Saved paid Carbs released once over two sips");
        helper.assertTrue(paid.equals(persisted), "Paid payload remains unchanged");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void edibleServingRecoversOnceOnlyAtCompletion(GameTestHelper helper) {
        var player = player(helper);
        DietBridge.tracker(player).orElseThrow().setValue("vegetables", .5f);
        var state = MetabolicStateStore.get(player);
        DietBridge.tracker(player).orElseThrow().setValue("carbs", .2f);
        player.setHealth(4);
        source(player, item("minecraft:honey_bottle"));
        sip(player, 1);
        var reserve = reserve(player);
        helper.assertTrue(reserve.getCompound("beverage").getCompound("nutrition").getBoolean("edible"),
                "Native honey bottle edible context retained");
        near(helper, player.getHealth(), 4, "Partial sip is not a whole Cream meal");
        helper.assertTrue(state.mealRecoveryCooldown == 0, "Partial sip cannot consume meal cooldown");
        for (int index = 1; index < reserve.getInt("thirst"); index++) sip(player, 1);
        near(helper, player.getHealth(), 4 + 2 * MetabolicMath.amplification(DietBridge.snapshot(player).carbs()),
                "One completed edible Vegetable meal recovery");
        helper.assertTrue(state.mealRecoveryCooldown > 0, "Completion starts existing recovery cooldown");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void blackoutAndMalformedFractionsReleaseNoPhysiologyEntitlement(GameTestHelper helper) {
        var player = player(helper);
        var state = MetabolicStateStore.get(player);
        source(player, new ItemStack(Items.MILK_BUCKET));
        DietBridge.tracker(player).orElseThrow().setValue("alcohol", 1);
        state.blackout = true;
        sip(player, 1);
        helper.assertTrue(source(player).is(Items.MILK_BUCKET) && reserve(player).isEmpty(),
                "Real physiology Check cancels prepayment without consuming source");
        DietBridge.tracker(player).orElseThrow().setValue("alcohol", 0); state.blackout = false;
        sip(player, 1);
        var nutrition = reserve(player).getCompound("beverage").getCompound("nutrition");
        var paid = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).copy();
        double proteins = DietBridge.snapshot(player).proteins();
        DietBridge.tracker(player).orElseThrow().setValue("alcohol", 1);
        state.blackout = true;
        sip(player, 1);
        helper.assertTrue(paid.equals(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)),
                "Real physiology Check preserves already-paid reserve bytes");
        MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, nutrition, 0, .5));
        near(helper, DietBridge.snapshot(player).proteins(), proteins, "Defensive blackout Sip guard");
        DietBridge.tracker(player).orElseThrow().setValue("alcohol", 0); state.blackout = false;
        for (double[] range : new double[][]{{0, 0}, {.5, .4}, {-.1, .5}, {0, 1.1}, {0, Double.NaN}})
            MinecraftForge.EVENT_BUS.post(new BeverageEvents.Sip(player, nutrition, range[0], range[1]));
        near(helper, DietBridge.snapshot(player).proteins(), proteins, "Invalid intervals cannot award native Diet");
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
        for (double carbs : new double[]{0, .25, .5, 1}) {
            DietBridge.tracker(player).orElseThrow().setValue("carbs", (float) carbs);
            thirst.setExhaustion(0);
            thirst.addExhaustion(player, input);
            near(helper, thirst.getExhaustion(), baseline * Math.pow(8, carbs),
                    "Native thirst hook applies native Carb surcharge at load " + carbs);
        }
        // Spice and its Deep Reserve reduce ordinary costs, not Carb expenditure.
        DietBridge.tracker(player).orElseThrow().setValue("spice", 1);
        DietBridge.tracker(player).orElseThrow().setValue("carbs", 1);
        state.enduranceReserveTicks = 200;
        thirst.setExhaustion(0);
        thirst.addExhaustion(player, input);
        near(helper, thirst.getExhaustion(), baseline * 7, "Deep Reserve cannot erase Carb thirst surcharge");
        DietBridge.tracker(player).orElseThrow().setValue("spice", 0);
        state.enduranceReserveTicks = 0;
        int pulses = (int) Math.floor(4 / (baseline * 8)) + 1;
        helper.assertTrue(pulses > 0 && pulses < 1000, "Bounded native exhaustion-threshold workload");
        for (double carbs : new double[]{0, 1}) {
            DietBridge.tracker(player).orElseThrow().setValue("carbs", (float) carbs);
            thirst.setExhaustion(0);
            thirst.setThirst(20);
            thirst.setQuenched(0);
            for (int i = 0; i < pulses; i++) {
                thirst.addExhaustion(player, input);
                thirst.tick(player);
            }
            helper.assertTrue(carbs == 0 ? thirst.getThirst() == 20 : thirst.getThirst() < 20,
                    "Identical native cost pulses must consume hydration sooner with Carbs, load=" + carbs);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void plantJuiceKeepsAuthoredNutritionAndCappedNectarAcrossPaidSips(GameTestHelper helper) {
        var player = player(helper);
        source(player, item("better_drinking_water:apple_juice"));
        sip(player, 1);
        var paid = reserve(player);
        helper.assertTrue(paid.getInt("thirst") == 3, "Native 250 mB juice serving buys three hydration");
        var nutrition = paid.getCompound("beverage").getCompound("nutrition");
        near(helper, nutrition.getCompound("diet").getDouble("carbs"), .025, "One authored plant Carb entitlement");
        near(helper, nutrition.getDouble("sugar"), 0, "No separate sugar mirror in paid juice");
        near(helper, DietBridge.snapshot(player).carbs(), .025 / 3, "One sip is not a whole Carb award");
        // Altering current capacity cannot reprice prepaid remaining entitlement.
        DietBridge.tracker(player).orElseThrow().setValue("carbs", .5f);
        for (int index = 1; index < 3; index++) sip(player, 1);
        near(helper, DietBridge.snapshot(player).carbs(), .5 + .025 * 2 / 3, "Only remaining paid Carb dose released");
        helper.assertTrue(reserve(player).isEmpty(), "Completed plant serving cannot replay nutrition");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void nativeManualPlantFinishDoesNotDoubleAwardThroughMealEvents(GameTestHelper helper) {
        var player = player(helper);
        var stack = item("better_drinking_water:apple_juice");
        var original = stack.copy();
        var thirst = player.getCapability(ModCapabilities.PLAYER_THIRST).resolve().orElseThrow();
        thirst.setThirst(10);
        player.getFoodData().setFoodLevel(10); player.getFoodData().setSaturation(0);
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(player, original, 32));
        var returned = stack.getItem().finishUsingItem(stack, player.level(), player);
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(player, original, 32, returned));
        near(helper, DietBridge.snapshot(player).carbs(), .025, "Manual finish plus native finish awards Carbs once");
        helper.assertTrue(player.getFoodData().getFoodLevel() == 11 && thirst.getThirst() == 13,
                "Manual explicit hunger/three hydration once");
        helper.assertTrue(returned.is(Items.GLASS_BOTTLE) && stack.isEmpty(), "One manual plant bottle yields one vessel");
        MetabolicStateStore.get(player).blackout = true;
        thirst.setThirst(10);
        var denied = item("better_drinking_water:apple_juice");
        denied.getItem().finishUsingItem(denied, player.level(), player);
        helper.assertTrue(denied.getCount() == 1 && thirst.getThirst() == 10, "Real physiology blackout veto protects manual plant purchase");
        near(helper, DietBridge.snapshot(player).carbs(), .025, "Denied manual finish adds no second Carb award");
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
