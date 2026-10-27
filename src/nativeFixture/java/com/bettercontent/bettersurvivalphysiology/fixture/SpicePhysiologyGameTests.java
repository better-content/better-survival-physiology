package com.bettercontent.bettersurvivalphysiology.fixture;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.bettercontent.betterspicyfarmers.cultivation.CultivationContent;
import com.bettercontent.betterspicyfarmers.runtime.SpiceRuntime;
import com.bettercontent.betterspicyfarmers.runtime.SpiceRuntimeConfig;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Nonshipping integration with actual Diet, physiology and registered survival players.
 * Native connections tick both spawn protection and item use. No damage overrides, synthetic
 * tick/Finish/Sip events, use-item advancement, or tick-counter edits are used.
 *
 * Boundary ordering is pinned, not guessed: Forge 47.4.22 userdev selects EventBus 6.2.33.
 * Its ListenerListInst.register appends per-priority listeners; buildCache preserves that order.
 * Diet's native Finish handler is LOW, SpiceRuntime's mod-init handler is LOWEST, and this
 * fixture registers its LOWEST Finish observer later, at test execution. Its HP/Diet snapshot
 * therefore follows both production handlers, before subsequent natural tick maintenance.
 * Typed BeverageCompat.Sip is NORMAL; the nested typed fixture observes it at LOWEST.
 * The qualification receipt must retain the pinned userdev/ListenerList/Diet javap evidence.
 * These fixtures are NOT a Death's Door, real-client, or accepted legacy-Water qualification. */
@GameTestHolder(SystemicSalienceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpicePhysiologyGameTests {
    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty", timeoutTicks = 400)
    public static void legacyEarthDoesNotBecomeSpiceAndOtherNutrientsPersist(GameTestHelper h) {
        withPlayer(h, 380, f -> {
            var p = f.player;
            var diet = DietBridge.tracker(p).orElseThrow();
            // Actual old player payload: Diet saves group keys directly, not positional indexes.
            CompoundTag old = new CompoundTag();
            old.putFloat("vegetables", .95f); old.putFloat("proteins", .31f);
            old.putFloat("grains", .42f); old.putFloat("fruits", .53f);
            old.putFloat("fats", .64f); old.putFloat("dairy", .75f);
            diet.load(old);
            var n = DietBridge.snapshot(p);
            near(h, n.spice(), 0, "Old Earth payload never creates Spice");
            near(h, n.proteins(), .31, "Flesh survives native migration");
            near(h, n.grains(), .42, "Grain survives native migration");
            near(h, n.fruits(), .53, "Fruit survives native migration");
            near(h, n.fats(), .64, "Tallow survives native migration");
            near(h, n.dairy(), .75, "Cream survives native migration");
            diet.setValue("spice", .36f);
            CompoundTag saved = new CompoundTag(); diet.save(saved); diet.load(saved);
            near(h, DietBridge.snapshot(p).spice(), .36, "New Spice persists in its own key");
            f.succeed();
        });
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty", timeoutTicks = 400)
    public static void oneNativeDietIntakeFullGroupStillPaysAndBoozeDoesNotReduceCost(GameTestHelper h) {
        withPlayer(h, 380, f -> {
            var p = f.player;
            var diet = DietBridge.tracker(p).orElseThrow();
            MetabolicStateStore.get(p).alcohol = .9; // Original physiological setup, not a damage mock.
            finish(f, paid -> {
                near(h, paid.spice(), .20, "Canonical native Diet awards precisely .20 for one chilli");
                paidCost(f, paid, 0, 4, "Booze does not reduce native serving cost");
                h.assertTrue(paid.alcohol() > .8, "Actual paid boundary still has strong physiological alcohol load");
                diet.setValue("spice", 1); // Original full-group setup; never rewrite it at completion.
                p.getFoodData().setFoodLevel(20); p.getFoodData().setSaturation(20);
                finish(f, full -> {
                    near(h, full.spice(), 1, "Full group remains capped at the actual paid boundary");
                    paidCost(f, full, paid.hits(), 4, "Full group and full food still pay serving HP");
                    // Native healing during the second use and clock drain remain enabled/observable.
                    h.assertTrue(DietBridge.drainUpperBand(p, .8, .5), "Native upper-band drain acts on full Spice");
                    h.assertTrue(DietBridge.snapshot(p).spice() < 1, "Spice participates in ordinary nutrient drain");
                    f.succeed();
                });
            });
        });
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty", timeoutTicks = 400)
    public static void creativeCompletionAndAbortedUseHaveNoSpiceAward(GameTestHelper h) {
        withPlayer(h, 380, f -> {
            var p = f.player;
            float before = p.getHealth();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CultivationContent.item("chilli")));
            p.getMainHandItem().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(p.isUsingItem(), "Aborted check starts actual native item use");
            f.after(3, () -> {
                p.releaseUsingItem();
                near(h, DietBridge.snapshot(p).spice(), 0, "Aborted native use has no nutrition award");
                h.assertTrue(f.spy.hits.isEmpty() && f.spy.finishes.isEmpty(), "Aborted use never settles or hurts");
                h.assertTrue(p.getHealth() >= before, "Aborted use has no HP decline");
                p.setGameMode(GameType.CREATIVE);
                h.assertTrue(p.isCreative(), "Native creative mode is actually active");
                float beforeCreative = p.getHealth();
                finish(f, creative -> {
                    near(h, creative.spice(), 0, "Creative native completed use has no Spice award");
                    h.assertTrue(creative.hits() == 0, "Creative completion has no Spice hurt");
                    h.assertTrue(creative.health() >= beforeCreative, "Creative use has no HP cost; ordinary healing remains allowed");
                    near(h, p.getHealth(), p.getMaxHealth(), "Creative/aborted healthy fixture retains full HP");
                    f.succeed();
                });
            });
        });
    }

    @GameTest(templateNamespace = SystemicSalienceMod.MOD_ID, template = "empty", timeoutTicks = 480)
    public static void typedPaidPortionsConserveSpiceUnitsAndApplyPortionCaps(GameTestHelper h) {
        h.assertTrue(com.bettercontent.bettersurvivalphysiology.compat.BeverageAvailability.typed(),
                "This fixture explicitly requires the separately qualified typed Water API; never upgrade legacy implicitly");
        // Do not initialize the typed listener (or any player) before the explicit hard ABI gate.
        withPlayer(h, 460, TypedFixture::start);
    }

    private static void withPlayer(GameTestHelper h, int deadline, Consumer<Fixture> checks) {
        var f = new Fixture(h);
        f.guard(() -> {
            f.join();
            h.runAtTickTime(deadline, () -> f.guard(() -> h.fail("Bounded native fixture deadline without completion")));
            f.after(65, () -> {
                var p = f.player;
                h.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && !p.isCreative() && !p.isSpectator(),
                        "Real registered fixture is in actual survival mode");
                var source = new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(SpiceRuntime.CONSUMPTION));
                h.assertTrue(!p.isInvulnerableTo(source), "Natural spawn protection expired for the actual Spice damage source");
                var diet = DietBridge.tracker(p).orElseThrow(); diet.initSuite();
                for (var group : NutritionSnapshot.Group.values()) diet.setValue(group.id, 0);
                h.assertTrue(diet.isActive(), "Actual Diet tracker is required and active");
                near(h, p.getHealth(), p.getMaxHealth(), "Settled native fixture starts healthy");
                near(h, SpiceRuntimeConfig.hpCost(1), 4, "Qualification profile uses four HP per unit");
                near(h, SpiceRuntimeConfig.hpCost(4), 8, "Qualification profile uses eight HP per completed-serving cap");
                f.observe(f.spy); // All mod-init listeners already exist; pinned same-priority append order.
                checks.accept(f);
            });
        });
    }
    private static void finish(Fixture f, Consumer<Finish> completed) {
        var p = f.player;
        int count = f.spy.finishes.size();
        var stack = new ItemStack(CultivationContent.item("chilli"));
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.use(f.helper.getLevel(), p, InteractionHand.MAIN_HAND);
        f.helper.assertTrue(p.isUsingItem(), "Native spicy item use starts");
        f.after(stack.getUseDuration() + 2L, () -> {
            f.helper.assertTrue(!p.isUsingItem(), "Native connection ticks complete actual item use");
            f.helper.assertTrue(f.spy.finishes.size() == count + 1, "Exactly one real native Finish boundary observed");
            completed.accept(f.spy.finishes.get(count));
        });
    }
    private static void paidCost(Fixture f, Finish paid, int priorHits, double cost, String message) {
        f.helper.assertTrue(paid.hits() == priorHits + 1, "Exactly one distinct native Spice hurt per completed paid serving");
        var hit = f.spy.hits.get(priorHits);
        f.helper.assertTrue(hit.attackerless() && hit.paying(), "Spice hurt is scoped and has no causing/direct entity");
        near(f.helper, hit.amount(), cost, message + " (actual native hurt amount)");
        near(f.helper, hit.health() - paid.health(), cost, message + " (actual paid HP delta)");
        System.out.println("BSP_SPICE_PAID beforeHP=" + hit.health() + " afterHP=" + paid.health()
                + " nativeAmount=" + hit.amount() + " spice=" + paid.spice() + " alcohol=" + paid.alcohol());
    }
    private static void near(GameTestHelper h, double actual, double expected, String what) {
        h.assertTrue(Math.abs(actual - expected) < .00001, what + ": expected " + expected + ", actual " + actual);
    }

    private record Hit(float health, float amount, boolean attackerless, boolean paying) {}
    private record Finish(float health, double spice, int hits, double alcohol) {}

    /** Reads only. Never changes event amount/cancellation, HP, nutrition, or clocks. */
    public static final class Spy {
        private final Fixture f;
        final List<Hit> hits = new ArrayList<>();
        final List<Finish> finishes = new ArrayList<>();
        int healEvents, tickDeclines;
        double endSpice;
        Spy(Fixture f) { this.f = f; }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void hurt(LivingHurtEvent event) {
            if (event.getEntity() == f.player && SpiceRuntime.isConsumption(event.getSource())) f.capture(() ->
                    hits.add(new Hit(f.player.getHealth(), event.getAmount(),
                            event.getSource().getEntity() == null && event.getSource().getDirectEntity() == null,
                            SpiceRuntime.isPaying(f.player))));
        }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void finished(LivingEntityUseItemEvent.Finish event) {
            if (event.getEntity() == f.player && event.getItem().is(CultivationContent.item("chilli"))) f.capture(() ->
                    finishes.add(new Finish(f.player.getHealth(), DietBridge.snapshot(f.player).spice(), hits.size(),
                            MetabolicStateStore.get(f.player).alcohol)));
        }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void healed(LivingHealEvent event) {
            if (event.getEntity() == f.player) f.capture(() -> {
                healEvents++;
                System.out.println("BSP_SPICE_NATIVE_HEAL beforeHP=" + f.player.getHealth() + " amount=" + event.getAmount());
            });
        }
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void beforeEnd(TickEvent.PlayerTickEvent event) {
            if (event.player == f.player && event.phase == TickEvent.Phase.END)
                f.capture(() -> endSpice = DietBridge.snapshot(f.player).spice());
        }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void afterEnd(TickEvent.PlayerTickEvent event) {
            if (event.player == f.player && event.phase == TickEvent.Phase.END) f.capture(() -> {
                double after = DietBridge.snapshot(f.player).spice();
                if (after < endSpice) {
                    tickDeclines++;
                    System.out.println("BSP_SPICE_NATIVE_TICK_DECLINE before=" + endSpice + " after=" + after);
                }
            });
        }
    }

    private static final class Fixture {
        final GameTestHelper helper;
        final ServerPlayer player;
        final Connection connection;
        final EmbeddedChannel channel;
        final List<Object> observers = new ArrayList<>();
        final Spy spy = new Spy(this);
        boolean closed, failureQueued;
        Throwable nativeFailure;
        Fixture(GameTestHelper helper) {
            this.helper = helper;
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "spice-physiology"));
            connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
        }
        void join() {
            player.server.getPlayerList().placeNewPlayer(connection, player);
            helper.assertTrue(!player.server.getConnection().getConnections().contains(connection), "Connection is not tick-registered twice");
            player.server.getConnection().getConnections().add(connection);
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
            var pos = helper.absolutePos(new BlockPos(0, 2, 0));
            player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            player.getFoodData().setFoodLevel(10); player.getFoodData().setSaturation(0);
        }
        void observe(Object observer) { observers.add(observer); MinecraftForge.EVENT_BUS.register(observer); }
        void after(long ticks, Runnable action) { helper.runAfterDelay(ticks, () -> guard(action)); }
        void capture(Runnable action) {
            if (closed || failureQueued) return;
            try { action.run(); }
            catch (Throwable failure) {
                failureQueued = true;
                nativeFailure = failure;
                // Native callbacks may be inside Connection.tick's iterator: never remove it there.
                after(1, () -> { throw new AssertionError("Native observer failed", failure); });
            }
        }
        void guard(Runnable action) {
            if (closed) return;
            try {
                // An earlier queued success must never outrun a failure captured later in the same native tick.
                if (nativeFailure != null) throw new AssertionError("Native observer failed", nativeFailure);
                action.run();
            }
            catch (Throwable failure) {
                try { cleanup(); } catch (Throwable cleanupFailure) { failure.addSuppressed(cleanupFailure); }
                failure.printStackTrace();
                helper.fail(failure.toString());
            }
        }
        void succeed() {
            System.out.println("BSP_SPICE_FIXTURE nativeSpiceHurts=" + spy.hits.size() + " healEvents=" + spy.healEvents
                    + " nativeTickDeclines=" + spy.tickDeclines + " finalHP=" + player.getHealth()
                    + " finalSpice=" + DietBridge.snapshot(player).spice());
            cleanup(); helper.succeed();
        }
        void cleanup() {
            if (closed) return;
            closed = true;
            try {
                for (Object observer : observers) MinecraftForge.EVENT_BUS.unregister(observer);
            } finally {
                try {
                    player.server.getConnection().getConnections().remove(connection);
                    if (player.server.getPlayerList().getPlayers().contains(player)) player.server.getPlayerList().remove(player);
                } finally {
                    try { player.discard(); }
                    finally { channel.finishAndReleaseAll(); }
                }
            }
        }
    }

    /** Loaded only after the hard typed-API gate; never an implicit legacy-provider upgrade. */
    public static final class TypedFixture {
        private final Fixture f;
        private final List<Portion> portions = new ArrayList<>();
        private int wholePortions;
        private boolean armed, done;
        private float beforeHP;
        private double beforeSpice;
        private int beforeHits;
        private TypedFixture(Fixture f) { this.f = f; }
        static void start(Fixture f) {
            var juice = new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation("better_drinking_water", "apple_juice")));
            f.helper.assertTrue(!juice.isEmpty(), "Qualified typed provider supplies the actual juice item");
            com.bettercontent.betterspicyfarmers.seasoning.SeasoningNbt.write(juice,
                    new com.bettercontent.betterspicyfarmers.seasoning.SeasoningDose(java.util.Map.of("hot_chilli_sauce", 250)));
            near(f.helper, com.bettercontent.betterspicyfarmers.api.SpiceApi.resolveUnits(f.player, juice), 4,
                    "Actual seasoned whole beverage resolves four canonical Spice units");
            var thirst = f.player.getCapability(dev.ghen.thirst.foundation.common.capability.ModCapabilities.PLAYER_THIRST)
                    .resolve().orElseThrow();
            thirst.setThirst(20); thirst.setQuenched(0);
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(f.player).resolve().orElseThrow()
                    .getStacksHandler("water").orElseThrow().getStacks().setStackInSlot(0, juice);
            f.observe(new TypedFixture(f));
        }
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void prepare(TickEvent.PlayerTickEvent event) {
            if (event.player != f.player || event.phase != TickEvent.Phase.END || f.player.tickCount % 10 != 0 || done) return;
            f.capture(() -> {
                f.helper.assertTrue(!armed, "Previous natural paid-portion callback completed before the next native cadence");
                beforeHP = f.player.getHealth(); beforeSpice = DietBridge.snapshot(f.player).spice(); beforeHits = f.spy.hits.size();
                // Only the native thirst precondition is prepared. The real provider's NORMAL END
                // handler performs debit, reserve persistence and typed Sip; never call/post it here.
                var thirst = f.player.getCapability(dev.ghen.thirst.foundation.common.capability.ModCapabilities.PLAYER_THIRST)
                        .resolve().orElseThrow();
                thirst.setThirst(19); thirst.setQuenched(0);
                armed = true;
                f.after(2, () -> f.helper.assertTrue(!armed, "Natural typed Water cadence produced its paid Sip callback"));
            });
        }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void paid(com.bettercontent.betterdrinkingwater.api.BeverageEvents.Sip event) {
            if (event.player() != f.player) return;
            f.capture(() -> {
                f.helper.assertTrue(armed && !done, "Exactly one real typed paid callback per prepared natural cadence");
                if (wholePortions == 0) {
                    var reserve = f.player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)
                            .getCompound("better_drinking_water:reserve");
                    f.helper.assertTrue(!reserve.isEmpty(), "Actual provider accepted and persisted captured Spice profile");
                    wholePortions = reserve.getInt("thirst");
                    f.helper.assertTrue(wholePortions > 1 && wholePortions <= 20, "Actual fixed whole serving has multiple paid portions");
                }
                portions.add(new Portion(beforeHP, f.player.getHealth(), beforeSpice, DietBridge.snapshot(f.player).spice(),
                        beforeHits, f.spy.hits.size(), event.fraction()));
                armed = false;
                if (portions.size() == wholePortions) {
                    done = true;
                    // Check captured boundaries and clean up OUTSIDE the provider/connection iterator.
                    f.after(1, this::verify);
                }
            });
        }
        private void verify() {
            var h = f.helper;
            h.assertTrue(portions.size() == wholePortions, "One actual paid callback for every whole-serving portion");
            h.assertTrue(f.spy.hits.size() == wholePortions, "Whole bottle has exactly one Spice hurt per portion and no extra charges");
            double expectedCost = Math.min(8, 4 * (4.0 / wholePortions));
            double totalCost = 0, totalGain = 0, totalFraction = 0;
            for (int i = 0; i < portions.size(); i++) {
                var portion = portions.get(i);
                near(h, portion.fraction(), 1.0 / wholePortions, "Each real paid sip releases exactly one canonical fraction");
                near(h, portion.spice() - portion.beforeSpice(), .8 / wholePortions, "Each paid sip awards its canonical fraction once");
                h.assertTrue(portion.hits() == portion.beforeHits() + 1, "Each actual paid fraction has exactly one Spice hurt");
                var hit = f.spy.hits.get(portion.beforeHits());
                h.assertTrue(hit.attackerless() && hit.paying(), "Each paid-fraction hurt is scoped and attackerless");
                near(h, hit.amount(), expectedCost, "Actual paid portion hurt applies its resolved portion cap");
                near(h, hit.health() - portion.health(), expectedCost, "Actual paid portion deducts resolved HP through native hurt");
                near(h, portion.beforeHP() - portion.health(), expectedCost, "Natural provider transaction charges only this paid fraction");
                totalCost += hit.health() - portion.health(); totalGain += portion.spice() - portion.beforeSpice();
                totalFraction += portion.fraction();
            }
            near(h, portions.get(0).spice(), .8 / wholePortions, "First paid sip awards canonical fraction once");
            near(h, portions.get(0).health(), f.player.getMaxHealth() - expectedCost, "First paid sip incurs its own resolved portion HP cost");
            near(h, portions.get(portions.size() - 1).spice(), .8, "All paid portions sum to one canonical whole nutrition dose");
            near(h, totalGain, .8, "Actual paid nutrition deltas conserve the whole dose");
            near(h, totalFraction, 1, "Actual provider fractions conserve one whole paid serving");
            near(h, totalCost, wholePortions * expectedCost, "Caps apply per completed portion, not a hidden whole-bottle HP budget");
            h.assertTrue(f.spy.finishes.isEmpty(), "Typed fractions never impersonate a native whole-item Finish");
            System.out.println("BSP_SPICE_TYPED portions=" + wholePortions + " paidHP=" + totalCost + " paidSpice=" + totalGain);
            f.succeed();
        }
        private record Portion(float beforeHP, float health, double beforeSpice, double spice,
                               int beforeHits, int hits, double fraction) {}
    }
}
