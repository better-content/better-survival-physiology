package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.compat.BrewingCompat;
import com.bettercontent.bettersurvivalphysiology.compat.ColdSweatCompat;
import com.bettercontent.bettersurvivalphysiology.compat.EpicFightCompat;
import com.bettercontent.bettersurvivalphysiology.compat.ThirstCompat;
import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.mixin.MobEffectInstanceAccessor;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import com.bettercontent.bettersurvivalphysiology.network.SalienceNetwork;
import com.bettercontent.bettersurvivalphysiology.network.MealFeedbackPacket;
import com.bettercontent.bettersurvivalphysiology.nutrition.DietBridge;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionDrain;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionSnapshot;
import com.bettercontent.bettersurvivalphysiology.nutrition.NutritionThreadBoundary;
import com.bettercontent.bettersurvivalphysiology.presentation.AspectIdentity;
import com.bettercontent.bettersurvivalphysiology.presentation.NutritionTier;
import com.bettercontent.bettersurvivalphysiology.presentation.ModSounds;
import com.bettercontent.bettersurvivalphysiology.presentation.PresentationFlags;
import com.bettercontent.bettersurvivalphysiology.presentation.PresentationSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SystemicSalienceMod.MOD_ID)
public final class SalienceEvents {
    private static final UUID FRUIT_MOVE = UUID.fromString("613818db-da4d-42f6-ac8e-dd9c7f226c1c");
    private static final UUID FRUIT_SWIM = UUID.fromString("0d498917-ed03-4503-a129-4d403c947398");
    private static final UUID FRUIT_STEP = UUID.fromString("426336df-f8cb-42af-9fe8-a57797e8fcf3");
    private static final UUID VEGETABLE_KNOCKBACK = UUID.fromString("bd4c1015-e215-4fd1-bbc7-a47aca7cd645");
    private static final UUID SUGAR_ATTACK_SPEED = UUID.fromString("07ecf601-8b76-4ef0-a897-a332902b9f1e");
    private static final UUID SUGAR_USE_SPEED = UUID.fromString("3295f760-2016-4cbb-8803-331b8b17e9b5");
    private static final UUID ALCOHOL_ATTACK_TIMING = UUID.fromString("fb8c17f4-59c7-4cb4-b6e9-c31316286787");
    private static final UUID ALCOHOL_RECOIL = UUID.fromString("6b26319b-81c4-4b3e-8dd6-d2586fd23467");
    private static final UUID ALCOHOL_DISPERSION = UUID.fromString("219ddb4f-3df3-43c6-828c-f1bf7aff5bb8");
    private static final MilkEffectGuard MILK_EFFECT_GUARD = new MilkEffectGuard();
    private static final Map<UUID, ConsumptionStart> CONSUMPTION_STARTS = new HashMap<>();
    private static final Map<UUID, PendingMeal> PENDING_MEALS = new HashMap<>();
    private static final NutritionSnapshot.Group[] GROUPS = NutritionSnapshot.Group.values();
    private static final AspectIdentity[] NUTRIENT_ASPECTS = {
            AspectIdentity.IMPACT, AspectIdentity.WORK, AspectIdentity.MOBILITY,
            AspectIdentity.ENDURANCE, AspectIdentity.ROBUSTNESS, AspectIdentity.RENEWAL
    };

    private SalienceEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        state.tickTransient();
        DietBridge.syncMetabolic(player, state);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);

        state.sprintTicks = player.isSprinting() ? state.sprintTicks + 1 : 0;
        if (state.workSequence > 0 && state.lastBreakTick < player.level().getGameTime() - 60L) state.workSequence = 0;
        applyIdentityModifiers(player, state, nutrition);
        applyRenewal(player, state, nutrition);
        applyEnduranceReserve(player, state, nutrition);

        PendingMeal pending = PENDING_MEALS.get(player.getUUID());
        if (pending != null && player.level().getGameTime() >= pending.dueTick()) {
            finishMealFeedback(player, state, pending.start());
            PENDING_MEALS.remove(player.getUUID());
            nutrition = DietBridge.snapshot(player);
        }

        PresentationSnapshot presentation = PresentationSnapshot.create(player, nutrition, state);
        boolean presentationChanged = updatePresentationFeedback(player, state, presentation.flags());

        if (player.tickCount % 20 == 0) {
            DietBridge.drainUpperBand(player, prepared(), state.sugar);
            if (DietBridge.tracker(player).isPresent()) state.addDebt(NutritionDrain.debtPerSecond(state.sugar));
            NutritionSnapshot authoritativeNutrition = DietBridge.snapshot(player);
            updateNutritionFalls(player, state, authoritativeNutrition);
            if (DietBridge.tracker(player).isPresent()) {
                NutritionThreadBoundary.onAuthoritativeTick(player, authoritativeNutrition, ordinary());
            }
            BrewingCompat.suppressNumbedHearts(player);
            MetabolicStateStore.save(player);
            SalienceNetwork.sync(player, authoritativeNutrition, state);
        } else if (presentationChanged) {
            SalienceNetwork.sync(player, nutrition, state);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double grains = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.GRAINS);
        double bonus = grains >= feast() ? 1.0 + Math.min(1.0, state.workSequence * 0.20)
                : grains >= prepared() ? 1.0 : grains >= ordinary() ? 0.10 : 0.0;
        event.setNewSpeed((float) (event.getNewSpeed() * (1.0 + bonus)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double grains = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.GRAINS);
        if (grains < feast() || !player.hasCorrectToolForDrops(event.getState())) {
            state.workSequence = 0;
            return;
        }
        long now = player.level().getGameTime();
        int previous = state.workSequence;
        state.workSequence = state.lastBreakTick >= now - 60L ? Math.min(5, state.workSequence + 1) : 1;
        state.lastBreakTick = now;
        if (previous < 5 && state.workSequence == 5) {
            metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.WORK_RHYTHM, "Work Rhythm reached five successive valid breaks");
            cueAt(player, AspectIdentity.WORK, "Work Rhythm ×5", event.getPos().getX() + .5,
                    event.getPos().getY() + .7, event.getPos().getZ() + .5, 10);
        }
        SalienceNetwork.sync(player, DietBridge.snapshot(player), state);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (stack.isEdible() || ConsumableProfiles.sugar(stack) > 0.0 || ConsumableProfiles.isAlcohol(stack)
                || stack.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            MetabolicState state = MetabolicStateStore.get(player);
            CONSUMPTION_STARTS.put(player.getUUID(), new ConsumptionStart(
                    DietBridge.snapshot(player), state.sugar, state.debt, state.alcohol, stack.isEdible()));
        }
        if (stack.is(net.minecraft.world.item.Items.MILK_BUCKET)
                && DietBridge.snapshot(player).actual(NutritionSnapshot.Group.DAIRY) >= prepared()) {
            // Guard removal itself: restoring through addEffect would cross RPG Stats Vitality's
            // duration-scaling hook and change the original expiry.
            MILK_EFFECT_GUARD.begin(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preserveMilkEffects(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!MILK_EFFECT_GUARD.isGuarding(player.getUUID())) return;
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && effect.getEffect().isBeneficial()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getItem().is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            MILK_EFFECT_GUARD.clear(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        MetabolicState state = MetabolicStateStore.get(player);
        double sugar = ConsumableProfiles.sugar(stack);
        double sugarBefore = state.sugar;
        if (sugar > 0.0) {
            state.addSugar(sugar);
            if (sugarBefore < .60 && state.sugar >= .60) activation(player, AspectIdentity.TEMPO, "Sugar Tempo II");
            else if (sugarBefore < .25 && state.sugar >= .25) activation(player, AspectIdentity.TEMPO, "Sugar Tempo I");
        }
        double alcohol = ConsumableProfiles.alcohol(stack);
        if (alcohol > 0.0) {
            state.addAlcohol(alcohol);
            ThirstCompat.addExhaustion(player, (float) (2.0 * alcohol));
        }
        if (sugar > 0.0 || alcohol > 0.0) DietBridge.syncMetabolic(player, state);
        if (stack.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            if (MILK_EFFECT_GUARD.isGuarding(player.getUUID())) {
                MILK_EFFECT_GUARD.clear(player.getUUID());
                activation(player, AspectIdentity.RENEWAL, "Benefits preserved");
            }
        }
        ConsumptionStart start = CONSUMPTION_STARTS.remove(player.getUUID());
        if (start != null) PENDING_MEALS.put(player.getUUID(), new PendingMeal(start, player.level().getGameTime() + 1L));
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double proteins = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.PROTEINS);
        double force = proteins >= prepared() ? 0.20 : proteins >= ordinary() ? 0.10 : 0.0;
        long now = player.level().getGameTime();
        boolean heavy = proteins >= feast() && state.heavyBlowCooldown == 0
                && now - state.lastAttackTick >= 80L && player.getAttackStrengthScale(0.5f) >= 0.90f;
        var motionBefore = target.getDeltaMovement();
        boolean impactApplied = false;
        if (heavy) {
            force += 1.0;
            state.heavyBlowCooldown = 8 * 20;
            state.heavyBlowTarget = target.getUUID();
            state.heavyBlowAttackTick = now;
            impactApplied = EpicFightCompat.tryImpact(target, 1.0);
            cueAt(player, AspectIdentity.IMPACT, "Heavy Blow", target.getX(),
                    target.getY() + target.getBbHeight() * .6, target.getZ(), 14);
        } else if (force > 0.0) {
            EpicFightCompat.applyImpact(target, force);
        }
        if (force > 0.0) target.knockback(force, player.getX() - target.getX(), player.getZ() - target.getZ());
        if (heavy && (impactApplied || !motionBefore.equals(target.getDeltaMovement())))
            metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.HEAVY_BLOW, target.getName().getString());
        state.lastAttackTick = now;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMeleeDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player) return;
        double proteins = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.PROTEINS);
        if (proteins < prepared()) return;
        MetabolicState state = MetabolicStateStore.get(player);
        boolean heavy = proteins >= feast() && event.getEntity().getUUID().equals(state.heavyBlowTarget)
                && player.level().getGameTime() == state.heavyBlowAttackTick;
        event.setAmount(event.getAmount() * (heavy ? 3.0f : 1.75f));
        if (heavy) state.heavyBlowTarget = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (DietBridge.snapshot(player).actual(NutritionSnapshot.Group.VEGETABLES) >= feast() && state.weatheredCooldown == 0) {
            event.setCanceled(true);
            state.weatheredCooldown = 30 * 20;
            activation(player, AspectIdentity.ROBUSTNESS, "Weathered Guard");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ColdSweatCompat.isTemperatureDamage(event.getSource())) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (DietBridge.snapshot(player).actual(NutritionSnapshot.Group.VEGETABLES) >= feast() && state.weatheredCooldown == 0) {
            ColdSweatCompat.pullSafe(player);
            event.setCanceled(true);
            state.weatheredCooldown = 30 * 20;
            activation(player, AspectIdentity.ROBUSTNESS, "Weathered Guard");
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EpicFightCompat.register(player);
        SalienceNetwork.sync(player, DietBridge.snapshot(player), MetabolicStateStore.get(player));
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EpicFightCompat.register(player);
        SalienceNetwork.sync(player, DietBridge.snapshot(player), MetabolicStateStore.get(player));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer replacement)) return;
        // Death clones retain the player's UUID. A use interrupted by death must not preserve
        // a milk guard into the replacement's later effect removals.
        MILK_EFFECT_GUARD.clear(replacement.getUUID());
        CONSUMPTION_STARTS.remove(replacement.getUUID());
        PENDING_MEALS.remove(replacement.getUUID());
        if (event.isWasDeath()) MetabolicStateStore.reset(replacement);
        else MetabolicStateStore.copy(event.getOriginal(), replacement);
        EpicFightCompat.register(replacement);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicStateStore.save(player);
        MetabolicStateStore.unload(player);
        ColdSweatCompat.unload(player);
        MILK_EFFECT_GUARD.clear(player.getUUID());
        CONSUMPTION_STARTS.remove(player.getUUID());
        PENDING_MEALS.remove(player.getUUID());
    }

    private static void applyIdentityModifiers(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double fruits = nutrition.actual(NutritionSnapshot.Group.FRUITS);
        double movement = fruits >= feast() && state.sprintTicks >= 60 ? 1.0 : fruits >= prepared() ? 0.50 : fruits >= ordinary() ? 0.05 : 0.0;
        double step = fruits >= feast() && state.sprintTicks >= 60 ? 1.0 : fruits >= prepared() ? 1.0 : 0.0;
        updateAttribute(player, "minecraft:generic.movement_speed", FRUIT_MOVE, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_stride");
        updateAttribute(player, "forge:swim_speed", FRUIT_SWIM, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_swim");
        updateAttribute(player, "forge:step_height_addition", FRUIT_STEP, step, AttributeModifier.Operation.ADDITION, "salience_fruit_step");

        double vegetables = nutrition.actual(NutritionSnapshot.Group.VEGETABLES);
        updateAttribute(player, "minecraft:generic.knockback_resistance", VEGETABLE_KNOCKBACK,
                vegetables >= prepared() ? 0.50 : 0.0, AttributeModifier.Operation.ADDITION, "salience_vegetable_weathered");
        double driftResistance = vegetables >= prepared() ? 0.80 : vegetables >= ordinary() ? 0.15 : 0.0;
        if (driftResistance > 0.0) ColdSweatCompat.dampenDrift(player, driftResistance);

        double tempo = state.sugar >= 0.60 ? 1.0 : state.sugar >= 0.25 ? 0.50 : 0.0;
        updateAttribute(player, "minecraft:generic.attack_speed", SUGAR_ATTACK_SPEED, tempo,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_sugar_tempo");
        updateAttribute(player, "tconstruct:player.use_item_speed", SUGAR_USE_SPEED, tempo,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_sugar_use_tempo");

        double control = 0.75 * MetabolicMath.alcoholPositive(state.alcohol) - 0.75 * MetabolicMath.alcoholImpairment(state.alcohol);
        updateAttribute(player, "better_rpg_progression:recoil_reduction", ALCOHOL_RECOIL, control,
                AttributeModifier.Operation.ADDITION, "salience_alcohol_recoil");
        updateAttribute(player, "better_rpg_progression:dispersion_reduction", ALCOHOL_DISPERSION, control,
                AttributeModifier.Operation.ADDITION, "salience_alcohol_dispersion");
        updateAttribute(player, "minecraft:generic.attack_speed", ALCOHOL_ATTACK_TIMING,
                -0.50 * MetabolicMath.alcoholImpairment(state.alcohol), AttributeModifier.Operation.MULTIPLY_TOTAL,
                "salience_alcohol_timing");
        EpicFightCompat.reinforceStunShield(player, MetabolicMath.alcoholPositive(state.alcohol));
        if (state.alcohol >= 0.90 && player.tickCount % 80 == 0) {
            player.push((player.getRandom().nextDouble() - 0.5) * 0.45, 0.0, (player.getRandom().nextDouble() - 0.5) * 0.45);
            particles(player, AspectIdentity.CONTROL, player.getX(), player.getY() + .3, player.getZ(), 3);
        }
    }

    private static void applyRenewal(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double dairy = nutrition.actual(NutritionSnapshot.Group.DAIRY);
        if (dairy >= feast() && state.dairyCleanseCooldown == 0) {
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && !effect.isInfiniteDuration()) {
                    if (player.removeEffect(effect.getEffect()))
                        metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.CLEANSE, effect.getEffect().getDisplayName().getString());
                    state.dairyCleanseCooldown = 20 * 20;
                    activation(player, AspectIdentity.RENEWAL, "Cleansed " + effect.getEffect().getDisplayName().getString());
                    break;
                }
            }
        }
        int extraTicks = dairy >= prepared() ? 3 : dairy >= ordinary() ? (player.tickCount % 10 == 0 ? 1 : 0) : 0;
        if (extraTicks == 0) return;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) continue;
            MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) effect;
            accessor.systemicSalience$setDuration(Math.max(1, accessor.systemicSalience$getDuration() - extraTicks));
        }
    }

    private static void applyEnduranceReserve(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        if (nutrition.actual(NutritionSnapshot.Group.FATS) < feast() || state.enduranceReserveCooldown > 0) return;
        if (player.getFoodData().getFoodLevel() <= 2 || ThirstCompat.isLow(player) || EpicFightCompat.isStaminaBelow(player, 0.10)) {
            state.enduranceReserveTicks = 10 * 20;
            state.enduranceReserveCooldown = 2 * 60 * 20;
            activation(player, AspectIdentity.ENDURANCE, "Deep Reserve");
            metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.DEEP_RESERVE, "A nutritional reserve activated during resource depletion");
        }
    }

    private static void finishMealFeedback(ServerPlayer player, MetabolicState state, ConsumptionStart start) {
        NutritionSnapshot current = DietBridge.snapshot(player);
        if (start.edible() && DietBridge.tracker(player).isPresent()) {
            NutritionThreadBoundary.onMealSettled(player, start.nutrition(), current, ordinary());
        }
        PresentationSnapshot presentation = PresentationSnapshot.create(player, current, state);
        int changedMask = 0;
        NutritionTier highestCrossing = null;
        AspectIdentity audibleAspect = null;
        for (int index = 0; index < GROUPS.length; index++) {
            float before = start.nutrition().actual(GROUPS[index]);
            float after = current.actual(GROUPS[index]);
            if (after > before + .001f) changedMask |= 1 << index;
            NutritionTier oldTier = NutritionTier.of(before, ordinary(), prepared(), feast());
            NutritionTier newTier = NutritionTier.of(after, ordinary(), prepared(), feast());
            if (newTier.ordinal() > oldTier.ordinal()) {
                if (highestCrossing == null || newTier.ordinal() > highestCrossing.ordinal()) {
                    highestCrossing = newTier;
                    audibleAspect = NUTRIENT_ASPECTS[index];
                }
                particles(player, NUTRIENT_ASPECTS[index], player.getX(), player.getY() + 1.0, player.getZ(), 5 + newTier.ordinal() * 2);
            }
        }
        if (audibleAspect != null) playAspectSound(player, audibleAspect, .36f);
        boolean sugarChanged = state.sugar > start.sugar() + .001;
        boolean alcoholChanged = state.alcohol > start.alcohol() + .001;
        if (changedMask == 0 && !sugarChanged && !alcoholChanged) return;
        int[] seconds = presentation.nutrientSeconds();
        SalienceNetwork.meal(player, new MealFeedbackPacket(changedMask,
                new float[]{current.proteins(), current.grains(), current.fruits(), current.fats(), current.vegetables(), current.dairy()},
                seconds, sugarChanged, alcoholChanged, (float) state.sugar, (float) state.debt, (float) state.alcohol,
                presentation.sugarSeconds(), presentation.debtSeconds(), presentation.alcoholSeconds()));
        SalienceNetwork.sync(player, current, state);
    }

    private static boolean updatePresentationFeedback(ServerPlayer player, MetabolicState state, int flags) {
        int previous = state.lastPresentationFlags;
        state.lastPresentationFlags = flags;
        if (previous < 0) return true;
        if (!PresentationFlags.has(previous, PresentationFlags.MOBILITY_STRIDE)
                && PresentationFlags.has(flags, PresentationFlags.MOBILITY_STRIDE)) {
            activation(player, AspectIdentity.MOBILITY, "Stride");
        }
        if (!PresentationFlags.has(previous, PresentationFlags.TEMPO_CRASH)
                && PresentationFlags.has(flags, PresentationFlags.TEMPO_CRASH)) {
            action(player, AspectIdentity.TEMPO, "Sugar crash");
            metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.SUGAR_CRASH, "Sugar crash");
            particles(player, AspectIdentity.TEMPO, player.getX(), player.getY() + 1.0, player.getZ(), 9);
            player.level().playSound(null, player.blockPosition(), ModSounds.brokenTempo(), SoundSource.PLAYERS, .48f, 1.0f);
        }
        return previous != flags;
    }

    private static void updateNutritionFalls(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        List<String> faded = new ArrayList<>();
        AspectIdentity audible = null;
        for (int index = 0; index < GROUPS.length; index++) {
            byte current = (byte) NutritionTier.of(nutrition.actual(GROUPS[index]), ordinary(), prepared(), feast()).ordinal();
            byte previous = state.lastNutritionTiers[index];
            state.lastNutritionTiers[index] = current;
            if (previous < NutritionTier.PREPARED.ordinal() || current >= previous) continue;
            AspectIdentity aspect = NUTRIENT_ASPECTS[index];
            if (audible == null) audible = aspect;
            String group = aspect.representative;
            faded.add(group.substring(0, 1).toUpperCase() + group.substring(1));
        }
        if (audible == null) return;
        player.displayClientMessage(Component.literal("Diet waning — " + String.join(", ", faded))
                .withStyle(style -> style.withColor(0xEEE8D8)), true);
        playAspectSound(player, audible, .20f);
    }

    private static void metabolicDiscovery(ServerPlayer player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind kind, String detail) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent(player, kind, detail));
    }

    private static void activation(ServerPlayer player, AspectIdentity aspect, String label) {
        action(player, aspect, label);
        particles(player, aspect, player.getX(), player.getY() + 1.0, player.getZ(), 9);
        playAspectSound(player, aspect, .48f);
    }

    private static void cueAt(ServerPlayer player, AspectIdentity aspect, String label,
                              double x, double y, double z, int count) {
        action(player, aspect, label);
        particles(player, aspect, x, y, z, count);
        player.level().playSound(null, x, y, z, ModSounds.get(aspect), SoundSource.PLAYERS, .52f, 1.0f);
    }

    private static void action(ServerPlayer player, AspectIdentity aspect, String label) {
        player.displayClientMessage(Component.literal(aspect.glyph + " " + aspect.displayName + " — " + label)
                .withStyle(style -> style.withColor(aspect.color)), true);
    }

    private static void playAspectSound(ServerPlayer player, AspectIdentity aspect, float volume) {
        player.level().playSound(null, player.blockPosition(), ModSounds.get(aspect), SoundSource.PLAYERS, volume, 1.0f);
    }

    private static void particles(ServerPlayer player, AspectIdentity aspect, double x, double y, double z, int count) {
        float red = ((aspect.color >> 16) & 255) / 255.0f;
        float green = ((aspect.color >> 8) & 255) / 255.0f;
        float blue = (aspect.color & 255) / 255.0f;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(red, green, blue), .85f);
        double angleBase = Math.atan2(player.getLookAngle().z, player.getLookAngle().x);
        for (int index = 0; index < count; index++) {
            double progress = count <= 1 ? 1.0 : index / (double) (count - 1);
            double px = x, py = y, pz = z;
            switch (aspect) {
                case IMPACT -> {
                    double angle = Math.PI * 2.0 * index / count;
                    double radius = .12 + .48 * progress;
                    px += Math.cos(angle) * radius; pz += Math.sin(angle) * radius; py += (index % 3 - 1) * .08;
                }
                case TEMPO -> {
                    int pulse = index % 2; double distance = .18 + .48 * progress;
                    px += Math.cos(angleBase) * distance + Math.cos(angleBase + Math.PI / 2) * (pulse == 0 ? -.12 : .12);
                    pz += Math.sin(angleBase) * distance + Math.sin(angleBase + Math.PI / 2) * (pulse == 0 ? -.12 : .12);
                }
                case WORK -> { px += (index % 3 - 1) * .12; py += .45 - progress * .55; pz += ((index / 3) % 3 - 1) * .12; }
                case MOBILITY -> {
                    double distance = .12 + .62 * progress;
                    px += Math.cos(angleBase) * distance; pz += Math.sin(angleBase) * distance; py += -.15 + Math.sin(progress * Math.PI) * .42;
                }
                case ENDURANCE -> { double angle = Math.PI * 2.0 * progress; px += Math.cos(angle) * .42; pz += Math.sin(angle) * .42; }
                case ROBUSTNESS -> {
                    double angle = Math.PI * 2.0 * (index % 6) / 6.0; double radius = .52 - .34 * progress;
                    px += Math.cos(angle) * radius; pz += Math.sin(angle) * radius;
                }
                case RENEWAL -> { double angle = Math.PI * 3.0 * progress; px += Math.cos(angle) * (.22 - .1 * progress); pz += Math.sin(angle) * (.22 - .1 * progress); py += progress * .7 - .2; }
                case CONTROL -> {
                    double angle = Math.PI * .5 * (index % 4); double radius = .5 * (1.0 - progress);
                    px += Math.cos(angle) * radius; pz += Math.sin(angle) * radius;
                }
            }
            player.serverLevel().sendParticles(dust, px, py, pz, 1, 0, 0, 0, 0);
        }
    }

    private record ConsumptionStart(NutritionSnapshot nutrition, double sugar, double debt, double alcohol,
                                    boolean edible) {}
    private record PendingMeal(ConsumptionStart start, long dueTick) {}

    private static double ordinary() { return SalienceConfig.ORDINARY_THRESHOLD.get(); }
    private static double prepared() { return SalienceConfig.PREPARED_THRESHOLD.get(); }
    private static double feast() { return SalienceConfig.FEAST_THRESHOLD.get(); }

    private static void updateAttribute(ServerPlayer player, String id, UUID uuid, double amount,
                                        AttributeModifier.Operation operation, String name) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
        if (attribute == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) instance.removeModifier(existing);
        if (amount != 0.0) instance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
    }
}
