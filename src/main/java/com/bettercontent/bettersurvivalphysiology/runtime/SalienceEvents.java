package com.bettercontent.bettersurvivalphysiology.runtime;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.bettercontent.bettersurvivalphysiology.compat.SpiceBridge;
import com.bettercontent.bettersurvivalphysiology.compat.BrewingCompat;
import com.bettercontent.bettersurvivalphysiology.compat.ColdSweatCompat;
import com.bettercontent.bettersurvivalphysiology.compat.EpicFightCompat;
import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.bettercontent.bettersurvivalphysiology.metabolism.ConsumableProfiles;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicMath;
import com.bettercontent.bettersurvivalphysiology.metabolism.NutritionEffects;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicState;
import com.bettercontent.bettersurvivalphysiology.metabolism.MetabolicStateStore;
import com.bettercontent.bettersurvivalphysiology.mixin.MobEffectInstanceAccessor;
import com.bettercontent.bettersurvivalphysiology.network.SalienceNetwork;
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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
    private static final UUID SPICE_MOVE = UUID.fromString("247b31f1-b115-421a-8401-fb69e209c7fa");
    private static final UUID SPICE_SWIM = UUID.fromString("767811d5-130f-455d-8fa7-c007635446af");
    private static final UUID FRUIT_MOVE = UUID.fromString("613818db-da4d-42f6-ac8e-dd9c7f226c1c");
    private static final UUID FRUIT_SWIM = UUID.fromString("0d498917-ed03-4503-a129-4d403c947398");
    private static final UUID ALCOHOL_MOVE = UUID.fromString("737044e1-940d-4ddb-beb3-bd968cdfa08d");
    private static final UUID ALCOHOL_USE_SPEED = UUID.fromString("3295f760-2016-4cbb-8803-331b8b17e9b5");
    private static final UUID ALCOHOL_ATTACK_TIMING = UUID.fromString("fb8c17f4-59c7-4cb4-b6e9-c31316286787");
    private static final Map<UUID, ConsumptionStart> CONSUMPTION_STARTS = new HashMap<>();
    private static final Map<UUID, PendingMeal> PENDING_MEALS = new HashMap<>();
    private static final Map<UUID, Map<MobEffect, EffectStamp>> FRUIT_EFFECT_STARTS = new HashMap<>();
    private static final Map<UUID, PendingFruitEffects> PENDING_FRUIT_EFFECTS = new HashMap<>();
    private static final NutritionSnapshot.Group[] GROUPS = NutritionSnapshot.Group.values();
    private static final AspectIdentity[] NUTRIENT_ASPECTS = {
            AspectIdentity.POWER, AspectIdentity.MOTION, AspectIdentity.ENDURANCE,
            AspectIdentity.RENEWAL, AspectIdentity.CONTROL
    };

    private SalienceEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        state.tickTransient();
        DietBridge.tickTimed(player);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        if (com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.available())
            com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.clearBlackout(state, nutrition.alcohol());
        else if (state.blackout && nutrition.alcohol() <= .85f) state.blackout = false;
        PendingFruitEffects fruitEffects = PENDING_FRUIT_EFFECTS.get(player.getUUID());
        if (fruitEffects != null && player.level().getGameTime() >= fruitEffects.dueTick()) {
            tuneFruitEffects(player, fruitEffects.before());
            PENDING_FRUIT_EFFECTS.remove(player.getUUID());
        }
        if (state.blackout) {
            double dx = player.getX() - state.blackoutX;
            double dz = player.getZ() - state.blackoutZ;
            if (dx * dx + dz * dz > 0.01) player.teleportTo(state.blackoutX, player.getY(), state.blackoutZ);
            player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
            player.setSprinting(false);
        }
        state.sprintTicks = player.isSprinting() ? state.sprintTicks + 1 : 0;
        if (nutrition.carbs() >= SalienceConfig.FOURTH && state.orchardBurstCooldown == 0
                && state.wasOnGround && !player.onGround()
                && state.sprintTicks >= 60 / MetabolicMath.amplification(nutrition.carbs())
                && player.getDeltaMovement().y > 0.1) {
            double burst = 0.35 * nutrition.carbs() * MetabolicMath.amplification(nutrition.carbs());
            player.push(player.getLookAngle().x * burst, 0.0, player.getLookAngle().z * burst);
            state.orchardBurstCooldown = (int) (12 * 20 / MetabolicMath.amplification(nutrition.carbs()));
            activation(player, AspectIdentity.MOTION, "Carbs leap");
        }
        state.wasOnGround = player.onGround();
        applyIdentityModifiers(player, state, nutrition);
        applyRenewal(player, state, nutrition);
        com.bettercontent.bettersurvivalphysiology.api.SpiceEffects.activateEndurance(player, state, nutrition);

        PendingMeal pending = PENDING_MEALS.get(player.getUUID());
        if (pending != null && player.level().getGameTime() >= pending.dueTick()) {
            finishMealFeedback(player, state, pending.start());
            PENDING_MEALS.remove(player.getUUID());
            nutrition = DietBridge.snapshot(player);
        }

        PresentationSnapshot presentation = PresentationSnapshot.create(player, nutrition, state);
        boolean presentationChanged = updatePresentationFeedback(player, state, nutrition, presentation.flags());

        if (player.tickCount % 20 == 0) {
            DietBridge.drainUpperBand(player, SalienceConfig.FOURTH, nutrition.carbs());
            NutritionSnapshot authoritativeNutrition = DietBridge.snapshot(player);
            updateNutritionFalls(player, state, authoritativeNutrition);
            if (DietBridge.tracker(player).isPresent()) {
                NutritionThreadBoundary.onAuthoritativeTick(player, authoritativeNutrition, ordinary());
            }
            if (com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.available())
                com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.suppressBrewing(player);
            else BrewingCompat.suppressNumbedHearts(player);
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
        if (state.blackout) { event.setNewSpeed(0.0f); return; }
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        boolean correctTool = player.hasCorrectToolForDrops(event.getState());
        double bonus = correctTool ? NutritionEffects.miningSpeed(nutrition.proteins(), nutrition.carbs()) : 0.0;
        event.setNewSpeed((float) (event.getNewSpeed() * Math.max(0.05,
                1.0 - 0.90 * MetabolicMath.alcoholImpairment(nutrition.alcohol(), nutrition.carbs()))));
        if (correctTool) bonus += SpiceBridge.miningBonus(nutrition.spice(), MetabolicMath.amplification(nutrition.carbs()));
        event.setNewSpeed((float) (event.getNewSpeed() * (1.0 + bonus)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (state.blackout) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (com.bettercontent.bettersurvivalphysiology.compat.BeverageAvailability.manages(event.getItem())) return;
        ItemStack stack = event.getItem();
        if (com.bettercontent.bettersurvivalphysiology.api.CulinaryNutritionApi.resolve(player, stack).isPresent()) {
            if (MetabolicStateStore.get(player).blackout) { event.setCanceled(true); return; }
            if (isFruitFood(stack)) FRUIT_EFFECT_STARTS.put(player.getUUID(), snapshotEffects(player));
            return; // Culinary completion owner performs all intake and meal feedback.
        }
        if (stack.isEdible() || ConsumableProfiles.baseSugar(stack) > 0.0 || ConsumableProfiles.isAlcohol(stack)
                || stack.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            MetabolicState state = MetabolicStateStore.get(player);
            if (state.blackout) { event.setCanceled(true); return; }
            NutritionSnapshot before = DietBridge.snapshot(player);
            CONSUMPTION_STARTS.put(player.getUUID(), new ConsumptionStart(
                    before, before.carbs(), before.alcohol(), stack.isEdible()));
            if (isFruitFood(stack)) FRUIT_EFFECT_STARTS.put(player.getUUID(), snapshotEffects(player));
        }
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FRUIT_EFFECT_STARTS.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (com.bettercontent.bettersurvivalphysiology.compat.BeverageAvailability.manages(event.getItem())) return;
        ItemStack stack = event.getItem();
        if (com.bettercontent.bettersurvivalphysiology.api.CulinaryNutritionApi.resolve(player, stack).isPresent()) {
            return; // Root culinary completion dispatcher is the sole committed-serving owner.
        }
        MetabolicState state = MetabolicStateStore.get(player);
        if (isFruitFood(stack)) {
            Map<MobEffect, EffectStamp> baseline = FRUIT_EFFECT_STARTS.remove(player.getUUID());
            if (baseline != null) PENDING_FRUIT_EFFECTS.put(player.getUUID(),
                    new PendingFruitEffects(baseline, player.level().getGameTime() + 1));
        }
        // Native unmanaged food awards its canonical Carbs once. Paid Alcohol potency is
        // excluded from native food results and committed only on this finished use.
        if (com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.available()) {
            com.bettercontent.bettersurvivalphysiology.api.BeverageProfiles.applyNativeAlcohol(player, stack, state);
        } else {
            // Preserve standalone core's previous native alcohol handling when Water is absent.
            double alcohol = ConsumableProfiles.alcohol(stack);
            DietBridge.awardAlcohol(player, alcohol);
            if (DietBridge.snapshot(player).alcohol() >= 1.0f) {
                state.blackout = true;
                state.blackoutX = player.getX(); state.blackoutY = player.getY(); state.blackoutZ = player.getZ();
                activation(player, AspectIdentity.CONTROL, "Blackout");
            }
        }
        if (stack.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            DietBridge.awardMilk(player);
        }
        ConsumptionStart start = CONSUMPTION_STARTS.remove(player.getUUID());
        if (start != null) PENDING_MEALS.put(player.getUUID(), new PendingMeal(start, player.level().getGameTime() + 1L));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double carbs = DietBridge.snapshot(player).carbs();
        if (carbs >= SalienceConfig.FIRST) event.setDamageMultiplier((float)
                (event.getDamageMultiplier() * Math.max(.3, 1.0 - .35 * carbs
                        * MetabolicMath.amplification(carbs))));
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && MetabolicStateStore.get(player).blackout)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInteractItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && MetabolicStateStore.get(player).blackout)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (state.blackout) { event.setCanceled(true); return; }
        double proteins = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.PROTEINS);
        long now = player.level().getGameTime();
        boolean heavy = HeavyBlow.ready(proteins, SalienceConfig.SECOND, state.heavyBlowCooldown,
                now, state.lastAttackTick, player.getAttackStrengthScale(0.5f), state.blackout);
        if (heavy) {
            state.heavyBlowCooldown = (int) (12 * 20 / MetabolicMath.amplification(DietBridge.snapshot(player).carbs()));
            state.heavyBlowTarget = target.getUUID();
            state.heavyBlowAttackTick = now;
            cueAt(player, AspectIdentity.IMPACT, "Heavy Blow", target.getX(),
                    target.getY() + target.getBbHeight() * .6, target.getZ(), 14);
        }
        state.lastAttackTick = now;
        state.heavyBlowFeedback.attacked();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMeleeDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getSource().getDirectEntity() != player) return;
        double proteins = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.PROTEINS);
        MetabolicState state = MetabolicStateStore.get(player);
        if (state.nutritionSplash) return; // Splash already includes the originating strike's nutrition.
        boolean heavy = proteins >= SalienceConfig.SECOND && event.getEntity().getUUID().equals(state.heavyBlowTarget)
                && player.level().getGameTime() == state.heavyBlowAttackTick;
        float base = event.getAmount();
        double carbs = DietBridge.snapshot(player).carbs();
        double amp = MetabolicMath.amplification(carbs);
        float boosted = (float) (base * (1.0 + NutritionEffects.meleeDamage(proteins, carbs))
                * (heavy ? 1.0 + .5 * amp : 1.0));
        event.setAmount(boosted);
        if (heavy) metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.HEAVY_BLOW, event.getEntity().getName().getString());
        if (heavy) state.heavyBlowTarget = null;
        if (proteins >= SalienceConfig.FOURTH && heavy) {
            state.nutritionSplash = true;
            try {
                int cleaved = 0;
                for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class,
                        event.getEntity().getBoundingBox().inflate(2.5), entity -> entity != event.getEntity() && entity != player && entity.isAlive())) {
                    nearby.hurt(player.damageSources().playerAttack(player), (float) (boosted * .35 * amp));
                    if (++cleaved >= 2) break;
                }
            } finally {
                state.nutritionSplash = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (SpiceBridge.isConsumption(event.getSource())) return;
        MetabolicState state = MetabolicStateStore.get(player);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);
        event.setAmount((float) (event.getAmount() * (1.0 - MetabolicMath.alcoholReduction(nutrition.alcohol(), nutrition.carbs()))));
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
        CONSUMPTION_STARTS.remove(replacement.getUUID());
        PENDING_MEALS.remove(replacement.getUUID());
        FRUIT_EFFECT_STARTS.remove(replacement.getUUID());
        PENDING_FRUIT_EFFECTS.remove(replacement.getUUID());
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
        CONSUMPTION_STARTS.remove(player.getUUID());
        PENDING_MEALS.remove(player.getUUID());
        FRUIT_EFFECT_STARTS.remove(player.getUUID());
        PENDING_FRUIT_EFFECTS.remove(player.getUUID());
    }

    private static void applyIdentityModifiers(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double carbs = nutrition.carbs();
        double amplification = MetabolicMath.amplification(carbs);
        double movement = NutritionEffects.movementSpeed(carbs, carbs);
        updateAttribute(player, "minecraft:generic.movement_speed", FRUIT_MOVE, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_stride");
        updateAttribute(player, "forge:swim_speed", FRUIT_SWIM, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_swim");

        double spiceMovement = SpiceBridge.movementBonus(nutrition.spice(), amplification);
        updateAttribute(player, "minecraft:generic.movement_speed", SPICE_MOVE, spiceMovement,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_spice_move");
        updateAttribute(player, "forge:swim_speed", SPICE_SWIM, spiceMovement,
                AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_spice_swim");
        // Vegetables own the existing resource-savings benefit, not a temperature modifier.
        double impairment = MetabolicMath.alcoholImpairment(nutrition.alcohol(), carbs);
        updateAttribute(player, "minecraft:generic.movement_speed", ALCOHOL_MOVE,
                -.75 * impairment, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_alcohol_move");
        updateAttribute(player, "minecraft:generic.attack_speed", ALCOHOL_ATTACK_TIMING,
                -.90 * impairment, AttributeModifier.Operation.MULTIPLY_TOTAL,
                "salience_alcohol_timing");
        updateAttribute(player, "tconstruct:player.use_item_speed", ALCOHOL_USE_SPEED,
                -.90 * impairment, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_alcohol_use");
    }

    private static void applyRenewal(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double vegetables = nutrition.vegetables();
        if (vegetables >= SalienceConfig.FOURTH && state.dairyCleanseCooldown == 0) {
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && !effect.isInfiniteDuration()) {
                    if (player.removeEffect(effect.getEffect()))
                        metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.CLEANSE, effect.getEffect().getDisplayName().getString());
                    state.dairyCleanseCooldown = (int) (20 * 20 / MetabolicMath.amplification(nutrition.carbs()));
                    activation(player, AspectIdentity.RENEWAL, "Cleansed " + effect.getEffect().getDisplayName().getString());
                    break;
                }
            }
        }
        // Fractional carry makes even 1% Vegetables work without rounding up a full tick.
        int extraTicks = state.renewalExtraTicks(vegetables, nutrition.carbs());
        if (extraTicks == 0) return;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) continue;
            MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) effect;
            accessor.systemicSalience$setDuration(Math.max(1, accessor.systemicSalience$getDuration() - extraTicks));
        }
    }

    /** Fractional gains get threshold feedback immediately; only a completed edible serving is a meal. */
    public static void beverageFeedback(ServerPlayer player, NutritionSnapshot before,
                                        double sugarBefore, double alcoholBefore,
                                        boolean edible, boolean completed) {
        MetabolicState state = MetabolicStateStore.get(player);
        if (edible && !completed && DietBridge.tracker(player).isPresent()) {
            NutritionThreadBoundary.onMealSettled(player, before, DietBridge.snapshot(player), ordinary());
        }
        finishMealFeedback(player, state,
                new ConsumptionStart(before, sugarBefore, alcoholBefore, edible && completed));
        // Tiny fractions must still reach the client, even below the feedback epsilon.
        SalienceNetwork.sync(player, DietBridge.snapshot(player), state);
    }

    private static void finishMealFeedback(ServerPlayer player, MetabolicState state, ConsumptionStart start) {
        NutritionSnapshot current = DietBridge.snapshot(player);
        if (start.edible() && current.vegetables() >= SalienceConfig.SECOND && state.mealRecoveryCooldown == 0) {
            player.heal((float) (2.0 * MetabolicMath.amplification(current.carbs())));
            state.mealRecoveryCooldown = (int) (30 * 20 / MetabolicMath.amplification(current.carbs()));
            activation(player, AspectIdentity.RENEWAL, "Meal recovery");
        }
        if (start.edible() && DietBridge.tracker(player).isPresent()) {
            NutritionThreadBoundary.onMealSettled(player, start.nutrition(), current, ordinary());
        }
        int changedMask = 0;
        NutritionTier highestCrossing = null;
        AspectIdentity audibleAspect = null;
        for (int index = 0; index < GROUPS.length; index++) {
            float before = start.nutrition().actual(GROUPS[index]);
            float after = current.actual(GROUPS[index]);
            if (after > before + .001f) changedMask |= 1 << index;
            NutritionTier oldTier = NutritionTier.of(before);
            NutritionTier newTier = NutritionTier.of(after);
            if (newTier.ordinal() > oldTier.ordinal()) {
                if (highestCrossing == null || newTier.ordinal() > highestCrossing.ordinal()) {
                    highestCrossing = newTier;
                    audibleAspect = NUTRIENT_ASPECTS[index];
                }
                particles(player, NUTRIENT_ASPECTS[index], player.getX(), player.getY() + 1.0, player.getZ(), 5 + newTier.ordinal() * 2);
            }
        }
        if (audibleAspect != null) playAspectSound(player, audibleAspect, .36f);
        boolean sugarChanged = current.carbs() > start.sugar() + .001;
        boolean alcoholChanged = current.alcohol() > start.alcohol() + .001;
        if (changedMask == 0 && !sugarChanged && !alcoholChanged) return;
        SalienceNetwork.sync(player, current, state);
    }

    private static boolean updatePresentationFeedback(ServerPlayer player, MetabolicState state,
                                                      NutritionSnapshot nutrition, int flags) {
        // Availability, not the charge-dependent presentation flag, re-arms the announcement.
        // Item swaps may interrupt native charge without ending the current eligibility cycle.
        boolean heavyBlowEligible = HeavyBlow.eligible(nutrition.proteins(), SalienceConfig.SECOND,
                state.heavyBlowCooldown, player.level().getGameTime(), state.lastAttackTick, state.blackout);
        if (state.heavyBlowFeedback.update(heavyBlowEligible, player.getAttackStrengthScale(0.5f))) {
            activation(player, AspectIdentity.IMPACT, "Heavy Blow ready");
        }
        int previous = state.lastPresentationFlags;
        state.lastPresentationFlags = flags;
        if (previous < 0) return true;
        if (!PresentationFlags.has(previous, PresentationFlags.CONTROL_BLACKOUT)
                && PresentationFlags.has(flags, PresentationFlags.CONTROL_BLACKOUT))
            activation(player, AspectIdentity.CONTROL, "Blackout");
        return previous != flags;
    }

    private static void updateNutritionFalls(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        List<String> faded = new ArrayList<>();
        AspectIdentity audible = null;
        for (int index = 0; index < GROUPS.length; index++) {
            byte current = (byte) NutritionTier.of(nutrition.actual(GROUPS[index])).ordinal();
            byte previous = state.lastNutritionTiers[index];
            state.lastNutritionTiers[index] = current;
            if (previous >= NutritionTier.FIRST.ordinal() && current == NutritionTier.BUILDING.ordinal()
                    && nutrition.carbs() >= .15) {
                metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.SUGAR_CRASH,
                        "Sugar drained " + NUTRIENT_ASPECTS[index].displayName + " below 20%");
                action(player, AspectIdentity.TEMPO, "Nutrition drained");
            }
            if (previous < NutritionTier.SECOND.ordinal() || current >= previous) continue;
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

    /** Spice owns the reserve transition; core retains the unchanged sound/discovery channel. */
    public static void deepReserveActivated(ServerPlayer player) {
        activation(player, AspectIdentity.ENDURANCE, "Deep Reserve");
        metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.DEEP_RESERVE,
                "A nutritional reserve activated during resource depletion");
    }

    /** Feedback identity stays on the unchanged core sound bus; Water owns the transition. */
    public static void alcoholBlackoutActivated(ServerPlayer player) {
        activation(player, AspectIdentity.CONTROL, "Blackout");
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
            switch (aspect.aspect) {
                case POWER -> {
                    double angle = Math.PI * 2.0 * index / count;
                    double radius = .12 + .48 * progress;
                    px += Math.cos(angle) * radius; pz += Math.sin(angle) * radius; py += (index % 3 - 1) * .08;
                }
                case MOTION -> {
                    double distance = .12 + .62 * progress;
                    px += Math.cos(angleBase) * distance; pz += Math.sin(angleBase) * distance; py += -.15 + Math.sin(progress * Math.PI) * .42;
                }
                case ENDURANCE -> { double angle = Math.PI * 2.0 * progress; px += Math.cos(angle) * .42; pz += Math.sin(angle) * .42; }
                case RENEWAL -> { double angle = Math.PI * 3.0 * progress; px += Math.cos(angle) * (.22 - .1 * progress); pz += Math.sin(angle) * (.22 - .1 * progress); py += progress * .7 - .2; }
                case CONTROL -> {
                    double angle = Math.PI * .5 * (index % 4); double radius = .5 * (1.0 - progress);
                    px += Math.cos(angle) * radius; pz += Math.sin(angle) * radius;
                }
            }
            player.serverLevel().sendParticles(dust, px, py, pz, 1, 0, 0, 0, 0);
        }
    }

    private record ConsumptionStart(NutritionSnapshot nutrition, double sugar, double alcohol,
                                    boolean edible) {}
    private record PendingMeal(ConsumptionStart start, long dueTick) {}
    private record EffectStamp(int duration, int amplifier) {}
    private record PendingFruitEffects(Map<MobEffect, EffectStamp> before, long dueTick) {}

    /** Schedules existing fruit caps only after the external owner commits a serving. */
    public static void culinaryEffectsCompleted(ServerPlayer player) {
        Map<MobEffect, EffectStamp> baseline = FRUIT_EFFECT_STARTS.remove(player.getUUID());
        if (baseline != null) PENDING_FRUIT_EFFECTS.put(player.getUUID(),
                new PendingFruitEffects(baseline, player.level().getGameTime() + 1));
    }

    private static boolean isFruitFood(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "fruitsdelight".equals(id.getNamespace());
    }

    private static Map<MobEffect, EffectStamp> snapshotEffects(ServerPlayer player) {
        Map<MobEffect, EffectStamp> result = new HashMap<>();
        for (MobEffectInstance effect : player.getActiveEffects())
            result.put(effect.getEffect(), new EffectStamp(effect.getDuration(), effect.getAmplifier()));
        return result;
    }

    private static void tuneFruitEffects(ServerPlayer player, Map<MobEffect, EffectStamp> before) {
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (id == null) continue;
            EffectStamp old = before.get(effect.getEffect());
            if (old != null && effect.getAmplifier() <= old.amplifier()
                    && effect.getDuration() <= old.duration()) continue;
            String name = id.toString();
            if (name.equals("fruitsdelight:cycling") || name.equals("fruitsdelight:digesting")
                    || name.equals("minecraft:health_boost")) {
                player.removeEffect(effect.getEffect());
                if (old != null) player.addEffect(new MobEffectInstance(effect.getEffect(), old.duration(), old.amplifier()));
            } else if (id.getNamespace().equals("minecraft") && effect.getEffect().isBeneficial()
                    && (effect.getAmplifier() > 0 || effect.getDuration() > 1200)) {
                player.removeEffect(effect.getEffect());
                int duration = old != null && old.amplifier() > 0 ? old.duration() : Math.min(1200, effect.getDuration());
                int amplifier = old != null && old.amplifier() > 0 ? old.amplifier() : 0;
                player.addEffect(new MobEffectInstance(effect.getEffect(), duration, amplifier));
            }
        }
    }

    private static double ordinary() { return SalienceConfig.FIRST; }

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
