package com.bettercontent.bettersurvivalphysiology.fixture;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.betterdrinkingwater.api.BeverageEvents;
import net.minecraftforge.common.MinecraftForge;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietAwardGuard;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietDairySplit;
import com.bettercontent.bettersurvivalphysiology.network.DietDairySyncPacket;
import com.bettercontent.bettersurvivalphysiology.nutrition.NativeDietRoster;
import com.bettercontent.bettersurvivalphysiology.nutrition.ServingVectors;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietResult;
import com.illusivesoulworks.diet.common.util.DietResult;
import com.illusivesoulworks.diet.common.capability.DietCapabilityEventsListener;
import com.illusivesoulworks.diet.common.util.DietValueGenerator;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Nonshipping native Diet 4813904 registry, API, recipe-classification and persistence checks.
 * Authored for review; NOT executed under the present no-test-run approval. */
@GameTestHolder(SystemicSalienceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FiveGroupDietGameTests {
    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void actualRegistrySuiteTrackerAndSavedValuesAreExactlyFive(GameTestHelper helper) {
        var player = player(helper);
        verify(helper, player);
        var diet = DietBridge.tracker(player).orElseThrow();
        diet.setValue("carbs", .41f);
        CompoundTag saved = new CompoundTag();
        diet.save(saved);
        helper.assertTrue(saved.getAllKeys().containsAll(NativeDietRoster.GROUPS), "Native save must contain five authoritative values");
        for (String retired : NativeDietRoster.RETIRED)
            helper.assertTrue(!saved.contains(retired), "No persisted retired value: " + retired);
        diet.setValue("carbs", 0);
        diet.load(saved);
        near(helper, diet.getValue("carbs"), .41, "Native Diet save/load maintains Carbs");
        verify(helper, player);
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void nativeIngredientClassificationAndUnambiguousDairySplit(GameTestHelper helper) {
        var player = player(helper);
        var api = DietApi.getInstance();
        var beef = new ItemStack(Items.BEEF);
        var bread = new ItemStack(Items.BREAD);
        var fruit = new ItemStack(Items.APPLE);
        var sweet = new ItemStack(Items.HONEY_BOTTLE);
        var vegetable = new ItemStack(Items.CARROT);
        for (var stack : List.of(bread, fruit, sweet)) {
            var groups = api.getGroups(player, stack).stream().map(g -> g.getName()).collect(Collectors.toSet());
            helper.assertTrue(groups.contains("carbs"), "Native source resolves Carbs " + stack);
            helper.assertTrue(groups.stream().noneMatch(NativeDietRoster.RETIRED::contains), "No retired native group " + stack);
        }
        helper.assertTrue(names(api.get(player, beef)).contains("proteins"), "Native Protein source");
        helper.assertTrue(names(api.get(player, vegetable)).contains("vegetables"), "Native Vegetable source");
        var overlapping = item("farmersdelight:honey_glazed_ham");
        helper.assertTrue(api.getGroups(player, overlapping).stream().filter(g -> g.getName().equals("carbs")).count() == 1,
                "One ingredient with native fruit/grain/sugar tags has only one Carbs membership");
        helper.assertTrue(api.get(player, overlapping).get().keySet().stream()
                .filter(g -> g.getName().equals("carbs")).count() == 1,
                "Overlapping source tags cannot generate two native Carbs awards");
        var cheese = item("brewinandchewin:flaxen_cheese_wedge");
        for (var result : List.of(api.get(player, cheese), api.get(player, cheese, 6, .6f),
                api.get(player, List.of(cheese), 6, .6f))) {
            var canonical = ServingVectors.CanonicalServing.fromNative(result).gains();
            double protein = canonical.getOrDefault("proteins", 0.0), carbs = canonical.getOrDefault("carbs", 0.0);
            helper.assertTrue(protein + carbs > 0, "Real dairy must have a positive native serving");
            near(helper, protein / (protein + carbs), .8, "Dairy native 80/20 across all three API overloads");
            helper.assertTrue(canonical.keySet().stream().noneMatch(NativeDietRoster.RETIRED::contains),
                    "Native result must already be canonical");
        }
        Map<String, Double> single = ServingVectors.CanonicalServing.fromNative(api.get(player, cheese, 6, .6f)).gains();
        Map<String, Double> composite = ServingVectors.CanonicalServing.fromNative(
                api.get(player, List.of(cheese), 6, .6f)).gains();
        helper.assertTrue(single.equals(composite), "Delegated stack/int/float path cannot split twice");
        var mixed = ServingVectors.CanonicalServing.fromNative(
                api.get(player, List.of(cheese, beef), 6, .6f)).gains();
        near(helper, mixed.get("proteins"), mixed.get("carbs"),
                "Mixed native group membership retains set shares without invented ingredient weights");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void actualUntaggedRecipesAndSyncedDerivedDairyAgree(GameTestHelper helper) {
        var player = player(helper);
        var dairy = item(DietDerivedFixtureItems.DAIRY.toString());
        var mixed = item(DietDerivedFixtureItems.MIXED.toString());
        for (ItemStack output : List.of(dairy, mixed)) {
            helper.assertTrue(DietGroups.getGroups(player.level()).stream().noneMatch(group -> group.contains(output)),
                    "Fixture output must have NO explicit native group tags: " + output);
            helper.assertTrue(DietValueGenerator.getAll().containsKey(output.getItem()),
                    "Pinned native recipe generator must classify actual untagged fixture food: " + output);
        }
        helper.assertTrue(DietValueGenerator.getTrail(dairy.getItem()).contains(item("brewinandchewin:flaxen_cheese_wedge").getItem()),
                "Native selected recipe trail must inherit actual cheese");
        helper.assertTrue(DietValueGenerator.getTrail(mixed.getItem()).contains(Items.BEEF),
                "Native mixed recipe trail must include the non-dairy ingredient");
        var ids = DietDairySplit.serverDerivedDairy();
        helper.assertTrue(ids.contains(DietDerivedFixtureItems.DAIRY) && !ids.contains(DietDerivedFixtureItems.MIXED),
                "Native GENERATED/TRAILS select only unambiguous dairy output for sync");
        var serverDairy = DietApi.getInstance().get(player, dairy);
        var serverMixed = DietApi.getInstance().get(player, mixed);
        var dairyGains = ServingVectors.CanonicalServing.fromNative(serverDairy).gains();
        double total = dairyGains.get("proteins") + dairyGains.get("carbs");
        helper.assertTrue(total > 0, "Native generated edible dairy recipe has a real dose");
        near(helper, dairyGains.get("proteins") / total, .8, "Real native derived dairy dose");
        var mixedGains = ServingVectors.CanonicalServing.fromNative(serverMixed).gains();
        near(helper, mixedGains.get("proteins"), mixedGains.get("carbs"),
                "Mixed derived recipe retains native equal group shares without invented weights");

        // Native group sync transmits membership but NOT recipe TRAILS. Roundtrip only our
        // missing classification bitset, then exercise the client-specific split branch.
        DietDairySplit.clearClient();
        var bytes = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DietDairySyncPacket.encode(new DietDairySyncPacket(ids), bytes);
            DietDairySplit.acceptClient(DietDairySyncPacket.decode(bytes).itemIds());
            var unsplit = new HashMap<>(serverDairy.get());
            unsplit.replaceAll((group, gain) -> group.getName().equals("proteins")
                    || group.getName().equals("carbs") ? (float) (total / 2) : gain);
            var clientDairy = ServingVectors.CanonicalServing.fromNative(
                    DietDairySplit.repartition(List.of(dairy), new DietResult(unsplit), true)).gains();
            near(helper, clientDairy.get("proteins"), dairyGains.get("proteins"), "Client synced native-derived Protein");
            near(helper, clientDairy.get("carbs"), dairyGains.get("carbs"), "Client synced native-derived Carbs");
            // Set the actual loaded server config so the milk, beverage profile and
            // native-result routes must read the SAME setting; restore even on failure.
            double originalShare = SalienceConfig.DAIRY_PROTEIN_SHARE.get();
            try {
                SalienceConfig.DAIRY_PROTEIN_SHARE.set(.60);
                var milk60 = ServingVectors.milk().gains();
                near(helper, milk60.get("proteins"), .036, "Configured paid milk Protein at 60/40");
                near(helper, milk60.get("carbs"), .024, "Configured paid milk Carbs at 60/40");
                CompoundTag profile = new CompoundTag();
                MinecraftForge.EVENT_BUS.post(new BeverageEvents.Profile(player, new ItemStack(Items.MILK_BUCKET), profile));
                near(helper, profile.getCompound("diet").getDouble("proteins"), milk60.get("proteins"),
                        "Actual beverage profile listener uses shared configured milk Protein");
                near(helper, profile.getCompound("diet").getDouble("carbs"), milk60.get("carbs"),
                        "Actual beverage profile listener uses shared configured milk Carbs");
                var configuredNative = ServingVectors.CanonicalServing.fromNative(DietDairySplit.repartition(
                        List.of(dairy), new DietResult(unsplit), true)).gains();
                near(helper, configuredNative.get("proteins") / (configuredNative.get("proteins") + configuredNative.get("carbs")),
                        .60, "Client native recipe classification reads same loaded config");
            } finally {
                SalienceConfig.DAIRY_PROTEIN_SHARE.set(originalShare);
            }
            for (ItemStack source : List.of(dairy, item("brewinandchewin:flaxen_cheese_wedge"))) {
                var existing = DietApi.getInstance().get(player, source).get();
                var raw = new HashMap<>(existing);
                double dose = raw.entrySet().stream().filter(entry -> entry.getKey().getName().equals("proteins")
                        || entry.getKey().getName().equals("carbs")).mapToDouble(entry -> entry.getValue()).sum();
                raw.replaceAll((group, gain) -> group.getName().equals("proteins")
                        || group.getName().equals("carbs") ? (float) (dose / 2) : gain);
                for (boolean client : new boolean[]{false, true}) {
                    var alternate = ServingVectors.CanonicalServing.fromNative(DietDairySplit.repartition(
                            List.of(source), new DietResult(raw), client, .60)).gains();
                    near(helper, alternate.get("proteins") / (alternate.get("proteins") + alternate.get("carbs")),
                            .60, "Actual native source/dairy-derived trail at configured 60/40");
                    near(helper, alternate.get("proteins") + alternate.get("carbs"), dose,
                            "Alternative config conserves real native dose without extra gain");
                }
            }
            helper.assertTrue(!DietDairySplit.repartition(List.of(mixed), serverMixed, true).get().isEmpty(),
                    "Synced client mixed recipe is still classified, not rejected");
            try {
                DietDairySplit.acceptClient(Set.of(new ResourceLocation("missing:unknown_dairy_item")));
                helper.fail("Unknown client classification ID must be a configuration error");
            } catch (IllegalArgumentException expected) {
                helper.assertTrue(DietDairySplit.clientReady(), "Rejected unknown ID cannot corrupt prior valid client snapshot");
            }
        } finally {
            bytes.release();
            DietDairySplit.clearClient();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void nativeNondeathClonePreservesAllFiveBalances(GameTestHelper helper) {
        nativeClone(helper, false);
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void nativeFinalDeathCloneResetsAllFiveBalances(GameTestHelper helper) {
        nativeClone(helper, true);
    }

    private static void nativeClone(GameTestHelper helper, boolean death) {
        var original = player(helper);
        var prior = DietBridge.tracker(original).orElseThrow();
        int index = 0;
        for (String group : NativeDietRoster.GROUPS) prior.setValue(group, .30f + .10f * index++);
        Map<String, Float> before = Map.copyOf(prior.getValues());
        helper.assertTrue(before.keySet().equals(NativeDietRoster.GROUPS), "Original owns precisely five native values");
        var replacement = player(helper); // distinct real native capability; no handwritten tracker clone
        var next = DietBridge.tracker(replacement).orElseThrow();
        helper.assertTrue(next != prior, "Distinct original and replacement capabilities");
        // The native fixture's pinned server config uses AMOUNT, 100% loss and 0
        // minimum; the expected zero below intentionally fails on config drift.
        // Invoke the original Diet Forge clone handler: it revives the old capability,
        // delegates to IDietTracker.copy(original, wasDeath), then invalidates old caps.
        DietCapabilityEventsListener.playerClone(new PlayerEvent.Clone(replacement, original, death));
        verify(helper, replacement);
        for (String group : NativeDietRoster.GROUPS)
            near(helper, next.getValue(group), death ? 0 : before.get(group),
                    "Native " + (death ? "final-death reset" : "nondeath clone") + " " + group);
        CompoundTag saved = new CompoundTag(); next.save(saved);
        helper.assertTrue(saved.getAllKeys().containsAll(NativeDietRoster.GROUPS),
                "Cloned native persisted value keys remain five");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty")
    public static void twoNativePlayersCannotShareFiveBalances(GameTestHelper helper) {
        var first = player(helper); var second = player(helper);
        var left = DietBridge.tracker(first).orElseThrow();
        var right = DietBridge.tracker(second).orElseThrow();
        helper.assertTrue(!first.getUUID().equals(second.getUUID()) && left != right,
                "Two unique native players require distinct live Diet capabilities");
        Map<String, Float> beforeSecond = Map.copyOf(right.getValues());
        int index = 0;
        for (String group : NativeDietRoster.GROUPS) left.setValue(group, .25f + .12f * index++);
        verify(helper, first); verify(helper, second);
        helper.assertTrue(right.getValues().equals(beforeSecond),
                "Writing first native tracker must not mutate second player or expose shared map");
        left.setValue("carbs", .13f);
        near(helper, DietBridge.snapshot(first).carbs(), .13, "First player owns own timed Carb value");
        near(helper, DietBridge.snapshot(second).carbs(), beforeSecond.get("carbs"),
                "Second player retains independent Carb value");
        helper.succeed();
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty", timeoutTicks = 900)
    public static void realReloadCannotResurrectBundledGroups(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = player(helper);
        var tracker = DietBridge.tracker(player).orElseThrow();
        tracker.setValue("carbs", .37f);
        var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
        server.reloadResources(selected).thenRunAsync(() -> {
            tracker.initSuite(); // the same existing fake player, not a freshly created tracker
            verify(helper, player);
            near(helper, tracker.getValue("carbs"), .37, "Existing native player value survives group/suite reload");
            helper.assertTrue(DietGroups.getGroup(player.level(), "fruits").isEmpty(),
                    "Pinned bundled diet:fruits must remain retired after real reload");
            helper.assertTrue(DietGroups.getGroup(player.level(), "sugars").isEmpty(),
                    "Pinned bundled diet:sugars must remain retired after real reload");
            helper.assertTrue(DietDairySplit.serverDerivedDairy().contains(DietDerivedFixtureItems.DAIRY),
                    "Fresh native reload reclassifies dairy-only recipe before next sync");
            helper.succeed();
        }, server).exceptionally(failure -> {
            helper.fail("Native reload failed: " + failure);
            return null;
        });
    }

    private static void verify(GameTestHelper helper, ServerPlayer player) {
        DietAwardGuard.validate(helper.getLevel().getServer());
        var groups = DietGroups.getGroups(player.level()).stream().map(g -> g.getName()).collect(Collectors.toSet());
        var suite = DietSuites.getSuite(player.level(), "builtin").orElseThrow();
        var tracker = DietBridge.tracker(player).orElseThrow();
        helper.assertTrue(groups.equals(NativeDietRoster.GROUPS), "Actual native registry has exactly five groups: " + groups);
        helper.assertTrue(suite.getGroups().stream().map(g -> g.getName()).collect(Collectors.toSet())
                .equals(NativeDietRoster.GROUPS), "Actual builtin suite has exactly five groups");
        helper.assertTrue(tracker.getValues().keySet().equals(NativeDietRoster.GROUPS),
                "Actual persistent native tracker has exactly five values: " + tracker.getValues().keySet());
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "five-diet-test"));
        DietBridge.tracker(player).orElseThrow().initSuite();
        return player;
    }

    private static Set<String> names(IDietResult result) {
        return result.get().keySet().stream().map(group -> group.getName()).collect(Collectors.toSet());
    }

    private static ItemStack item(String id) {
        var key = new ResourceLocation(id);
        if (!ForgeRegistries.ITEMS.containsKey(key)) throw new AssertionError("Missing required fixture item " + id);
        return new ItemStack(ForgeRegistries.ITEMS.getValue(key));
    }

    private static void near(GameTestHelper helper, double actual, double expected, String label) {
        helper.assertTrue(Math.abs(actual - expected) < .000002,
                label + ": expected " + expected + ", got " + actual);
    }
}
