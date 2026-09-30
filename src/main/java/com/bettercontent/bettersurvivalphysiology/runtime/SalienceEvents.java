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
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
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
    private static final UUID FRUIT_MOVE = UUID.fromString("613818db-da4d-42f6-ac8e-dd9c7f226c1c");
    private static final UUID FRUIT_SWIM = UUID.fromString("0d498917-ed03-4503-a129-4d403c947398");
    private static final UUID FRUIT_STEP = UUID.fromString("426336df-f8cb-42af-9fe8-a57797e8fcf3");
    private static final UUID ALCOHOL_MOVE = UUID.fromString("737044e1-940d-4ddb-beb3-bd968cdfa08d");
    private static final UUID GARDEN_EXTREME_MOVE = UUID.fromString("f28d33da-7708-43a2-a4f2-4e29969048e8");
    private static final UUID VEGETABLE_KNOCKBACK = UUID.fromString("bd4c1015-e215-4fd1-bbc7-a47aca7cd645");
    private static final UUID ALCOHOL_USE_SPEED = UUID.fromString("3295f760-2016-4cbb-8803-331b8b17e9b5");
    private static final UUID ALCOHOL_ATTACK_TIMING = UUID.fromString("fb8c17f4-59c7-4cb4-b6e9-c31316286787");
    private static final MilkEffectGuard MILK_EFFECT_GUARD = new MilkEffectGuard();
    private static final Map<UUID, ConsumptionStart> CONSUMPTION_STARTS = new HashMap<>();
    private static final Map<UUID, PendingMeal> PENDING_MEALS = new HashMap<>();
    private static final Map<UUID, Map<MobEffect, EffectStamp>> FRUIT_EFFECT_STARTS = new HashMap<>();
    private static final Map<UUID, PendingFruitEffects> PENDING_FRUIT_EFFECTS = new HashMap<>();
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
        DietBridge.syncMetabolic(player, state);
        NutritionSnapshot nutrition = DietBridge.snapshot(player);

        state.sprintTicks = player.isSprinting() ? state.sprintTicks + 1 : 0;
        int workGrace = (int) ((nutrition.grains() >= SalienceConfig.THIRD ? 100 : 60)
                * MetabolicMath.amplification(state.sugar));
        if (state.workSequence > 0 && state.lastBreakTick < player.level().getGameTime() - workGrace) state.workSequence = 0;
        if (nutrition.fruits() >= SalienceConfig.FOURTH && state.orchardBurstCooldown == 0
                && state.wasOnGround && !player.onGround()
                && state.sprintTicks >= 60 / MetabolicMath.amplification(state.sugar)
                && player.getDeltaMovement().y > 0.1) {
            double burst = 0.35 * nutrition.fruits() * MetabolicMath.amplification(state.sugar);
            player.push(player.getLookAngle().x * burst, 0.0, player.getLookAngle().z * burst);
            state.orchardBurstCooldown = (int) (12 * 20 / MetabolicMath.amplification(state.sugar));
            activation(player, AspectIdentity.MOBILITY, "Orchard leap");
        }
        state.wasOnGround = player.onGround();
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
            DietBridge.drainUpperBand(player, SalienceConfig.FOURTH, state.sugar);
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
        if (state.blackout) { event.setNewSpeed(0.0f); return; }
        double grains = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.GRAINS);
        double bonus = grains >= SalienceConfig.FIRST && player.hasCorrectToolForDrops(event.getState())
                ? 0.60 * grains * MetabolicMath.amplification(state.sugar) : 0.0;
        if (grains >= SalienceConfig.SECOND) bonus += 0.10 * state.workSequence * MetabolicMath.amplification(state.sugar);
        if (grains >= SalienceConfig.FOURTH && state.workBurstTicks > 0) bonus += MetabolicMath.amplification(state.sugar);
        event.setNewSpeed((float) (event.getNewSpeed() * Math.max(0.05,
                1.0 - 0.90 * MetabolicMath.alcoholImpairment(state.alcohol, state.sugar))));
        event.setNewSpeed((float) (event.getNewSpeed() * (1.0 + bonus)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        double grains = DietBridge.snapshot(player).actual(NutritionSnapshot.Group.GRAINS);
        if (state.blackout) { event.setCanceled(true); return; }
        if (grains < SalienceConfig.FIRST || !player.hasCorrectToolForDrops(event.getState())) {
            state.workSequence = 0;
            return;
        }
        long now = player.level().getGameTime();
        int previous = state.workSequence;
        int grace = (int) ((grains >= SalienceConfig.THIRD ? 100 : 60)
                * MetabolicMath.amplification(state.sugar));
        state.workSequence = state.lastBreakTick >= now - grace ? Math.min(5, state.workSequence + 1) : 1;
        state.lastBreakTick = now;
        if (grains >= SalienceConfig.FOURTH && state.workSequence == 5) state.workBurstTicks = 5 * 20;
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
            if (state.blackout) { event.setCanceled(true); return; }
            CONSUMPTION_STARTS.put(player.getUUID(), new ConsumptionStart(
                    DietBridge.snapshot(player), state.sugar, state.alcohol, stack.isEdible()));
            if (isFruitFood(stack)) FRUIT_EFFECT_STARTS.put(player.getUUID(), snapshotEffects(player));
        }
        if (stack.is(net.minecraft.world.item.Items.MILK_BUCKET)
                && DietBridge.snapshot(player).actual(NutritionSnapshot.Group.DAIRY) >= SalienceConfig.SECOND) {
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
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getItem().is(net.minecraft.world.item.Items.MILK_BUCKET)) MILK_EFFECT_GUARD.clear(player.getUUID());
            FRUIT_EFFECT_STARTS.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        MetabolicState state = MetabolicStateStore.get(player);
        if (isFruitFood(stack)) {
            Map<MobEffect, EffectStamp> baseline = FRUIT_EFFECT_STARTS.remove(player.getUUID());
            if (baseline != null) PENDING_FRUIT_EFFECTS.put(player.getUUID(),
                    new PendingFruitEffects(baseline, player.level().getGameTime() + 1));
        }
        double sugar = ConsumableProfiles.sugar(stack);
        if (sugar > 0.0) {
            state.addSugar(sugar);
            activation(player, AspectIdentity.TEMPO, "Sweetness ×" + String.format("%.1f", MetabolicMath.amplification(state.sugar)));
        }
        double alcohol = ConsumableProfiles.alcohol(stack);
        if (alcohol > 0.0) {
            state.addAlcohol(alcohol);
            if (state.blackout) {
                state.blackoutX = player.getX(); state.blackoutY = player.getY(); state.blackoutZ = player.getZ();
                activation(player, AspectIdentity.CONTROL, "Blackout");
            }
        }
        if (sugar > 0.0 || alcohol > 0.0) DietBridge.syncMetabolic(player, state);
        if (stack.is(net.minecraft.world.item.Items.MILK_BUCKET)) {
            DietBridge.awardMilk(player);
            if (MILK_EFFECT_GUARD.isGuarding(player.getUUID())) {
                MILK_EFFECT_GUARD.clear(player.getUUID());
                activation(player, AspectIdentity.RENEWAL, "Benefits preserved");
            }
        }
        ConsumptionStart start = CONSUMPTION_STARTS.remove(player.getUUID());
        if (start != null) PENDING_MEALS.put(player.getUUID(), new PendingMeal(start, player.level().getGameTime() + 1L));
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double dairy = DietBridge.snapshot(player).dairy();
        if (dairy >= SalienceConfig.FIRST)
            event.setAmount((float) (event.getAmount() * (1.0 + .25 * dairy
                    * MetabolicMath.amplification(MetabolicStateStore.get(player).sugar))));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double fruits = DietBridge.snapshot(player).fruits();
        if (fruits >= SalienceConfig.FIRST) event.setDamageMultiplier((float)
                (event.getDamageMultiplier() * Math.max(.3, 1.0 - .35 * fruits
                        * MetabolicMath.amplification(MetabolicStateStore.get(player).sugar))));
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
        double force = proteins >= SalienceConfig.FIRST ? .15 * MetabolicMath.amplification(state.sugar) : 0.0;
        long now = player.level().getGameTime();
        boolean heavy = proteins >= SalienceConfig.SECOND && state.heavyBlowCooldown == 0
                && now - state.lastAttackTick >= 80L && player.getAttackStrengthScale(0.5f) >= 0.90f;
        var motionBefore = target.getDeltaMovement();
        boolean impactApplied = false;
        if (heavy) {
            force += .4;
            state.heavyBlowCooldown = (int) (12 * 20 / MetabolicMath.amplification(state.sugar));
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
        if (proteins < SalienceConfig.FIRST) return;
        MetabolicState state = MetabolicStateStore.get(player);
        boolean heavy = proteins >= SalienceConfig.SECOND && event.getEntity().getUUID().equals(state.heavyBlowTarget)
                && player.level().getGameTime() == state.heavyBlowAttackTick;
        float base = event.getAmount();
        double amp = MetabolicMath.amplification(state.sugar);
        float boosted = (float) (base * (1.0 + .25 * proteins * amp) * (heavy ? 1.0 + .5 * amp : 1.0));
        event.setAmount(boosted);
        if (proteins >= SalienceConfig.THIRD && heavy) EpicFightCompat.tryImpact(event.getEntity(), .7);
        if (proteins >= SalienceConfig.FOURTH && heavy) {
            int cleaved = 0;
            for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class,
                    event.getEntity().getBoundingBox().inflate(2.5), entity -> entity != event.getEntity() && entity != player && entity.isAlive())) {
                nearby.hurt(player.damageSources().playerAttack(player), (float) (boosted * .35 * amp));
                if (++cleaved >= 2) break;
            }
        }
        if (heavy) state.heavyBlowTarget = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        if (DietBridge.snapshot(player).actual(NutritionSnapshot.Group.VEGETABLES) >= SalienceConfig.FOURTH && state.weatheredCooldown == 0) {
            event.setCanceled(true);
            state.weatheredCooldown = (int) (30 * 20 / MetabolicMath.amplification(state.sugar));
            activation(player, AspectIdentity.ROBUSTNESS, "Weathered Guard");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MetabolicState state = MetabolicStateStore.get(player);
        event.setAmount((float) (event.getAmount() * (1.0 - MetabolicMath.alcoholReduction(state.alcohol, state.sugar))));
        if (!ColdSweatCompat.isTemperatureDamage(event.getSource())) return;
        if (DietBridge.snapshot(player).actual(NutritionSnapshot.Group.VEGETABLES) >= SalienceConfig.FOURTH && state.weatheredCooldown == 0) {
            ColdSweatCompat.pullSafe(player);
            event.setCanceled(true);
            state.weatheredCooldown = (int) (30 * 20 / MetabolicMath.amplification(state.sugar));
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
        MILK_EFFECT_GUARD.clear(player.getUUID());
        CONSUMPTION_STARTS.remove(player.getUUID());
        PENDING_MEALS.remove(player.getUUID());
        FRUIT_EFFECT_STARTS.remove(player.getUUID());
        PENDING_FRUIT_EFFECTS.remove(player.getUUID());
    }

    private static void applyIdentityModifiers(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double fruits = nutrition.actual(NutritionSnapshot.Group.FRUITS);
        double amplification = MetabolicMath.amplification(state.sugar);
        double movement = fruits >= SalienceConfig.FIRST ? .20 * fruits * amplification
                + (fruits >= SalienceConfig.THIRD && state.sprintTicks >= 60 ? .12 * amplification : 0.0) : 0.0;
        double step = fruits >= SalienceConfig.SECOND ? .5 * amplification : 0.0;
        updateAttribute(player, "minecraft:generic.movement_speed", FRUIT_MOVE, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_stride");
        updateAttribute(player, "forge:swim_speed", FRUIT_SWIM, movement, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_fruit_swim");
        updateAttribute(player, "forge:step_height_addition", FRUIT_STEP, step, AttributeModifier.Operation.ADDITION, "salience_fruit_step");

        double vegetables = nutrition.actual(NutritionSnapshot.Group.VEGETABLES);
        updateAttribute(player, "minecraft:generic.knockback_resistance", VEGETABLE_KNOCKBACK,
                vegetables >= SalienceConfig.SECOND ? .5 * vegetables * amplification : 0.0,
                AttributeModifier.Operation.ADDITION, "salience_vegetable_weathered");
        double driftResistance = vegetables >= SalienceConfig.FIRST ? Math.min(.9, .5 * vegetables * amplification) : 0.0;
        if (driftResistance > 0.0) ColdSweatCompat.dampenDrift(player, driftResistance);
        double impairment = MetabolicMath.alcoholImpairment(state.alcohol, state.sugar);
        updateAttribute(player, "minecraft:generic.movement_speed", ALCOHOL_MOVE,
                -.75 * impairment, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_alcohol_move");
        updateAttribute(player, "minecraft:generic.attack_speed", ALCOHOL_ATTACK_TIMING,
                -.90 * impairment, AttributeModifier.Operation.MULTIPLY_TOTAL,
                "salience_alcohol_timing");
        updateAttribute(player, "tconstruct:player.use_item_speed", ALCOHOL_USE_SPEED,
                -.90 * impairment, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_alcohol_use");
        double extreme = vegetables >= SalienceConfig.THIRD && ColdSweatCompat.isExtreme(player) ? .5 * vegetables * amplification : 0.0;
        updateAttribute(player, "minecraft:generic.movement_speed", GARDEN_EXTREME_MOVE,
                extreme, AttributeModifier.Operation.MULTIPLY_TOTAL, "salience_garden_extreme");
    }

    private static void applyRenewal(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        double dairy = nutrition.actual(NutritionSnapshot.Group.DAIRY);
        if (dairy >= SalienceConfig.FOURTH && state.dairyCleanseCooldown == 0) {
            for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && !effect.isInfiniteDuration()) {
                    if (player.removeEffect(effect.getEffect()))
                        metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.CLEANSE, effect.getEffect().getDisplayName().getString());
                    state.dairyCleanseCooldown = (int) (20 * 20 / MetabolicMath.amplification(state.sugar));
                    activation(player, AspectIdentity.RENEWAL, "Cleansed " + effect.getEffect().getDisplayName().getString());
                    break;
                }
            }
        }
        int extraTicks = dairy >= SalienceConfig.THIRD ? (int) Math.ceil(2 * MetabolicMath.amplification(state.sugar)) : 0;
        if (extraTicks == 0) return;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) continue;
            MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) effect;
            accessor.systemicSalience$setDuration(Math.max(1, accessor.systemicSalience$getDuration() - extraTicks));
        }
    }

    private static void applyEnduranceReserve(ServerPlayer player, MetabolicState state, NutritionSnapshot nutrition) {
        if (nutrition.actual(NutritionSnapshot.Group.FATS) < SalienceConfig.FOURTH || state.enduranceReserveCooldown > 0) return;
        if (player.getFoodData().getFoodLevel() <= 2 || ThirstCompat.isLow(player) || EpicFightCompat.isStaminaBelow(player, 0.10)) {
            state.enduranceReserveTicks = (int) (10 * 20 * MetabolicMath.amplification(state.sugar));
            state.enduranceReserveCooldown = 2 * 60 * 20;
            activation(player, AspectIdentity.ENDURANCE, "Deep Reserve");
            metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.DEEP_RESERVE, "A nutritional reserve activated during resource depletion");
        }
    }

    private static void finishMealFeedback(ServerPlayer player, MetabolicState state, ConsumptionStart start) {
        NutritionSnapshot current = DietBridge.snapshot(player);
        if (start.edible() && current.dairy() >= SalienceConfig.FIRST && state.mealRecoveryCooldown == 0) {
            player.heal((float) (2.0 * MetabolicMath.amplification(state.sugar)));
            state.mealRecoveryCooldown = (int) (30 * 20 / MetabolicMath.amplification(state.sugar));
            activation(player, AspectIdentity.RENEWAL, "Meal recovery");
        }
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
        boolean sugarChanged = state.sugar > start.sugar() + .001;
        boolean alcoholChanged = state.alcohol > start.alcohol() + .001;
        if (changedMask == 0 && !sugarChanged && !alcoholChanged) return;
        int[] seconds = presentation.nutrientSeconds();
        SalienceNetwork.meal(player, new MealFeedbackPacket(changedMask,
                new float[]{current.proteins(), current.grains(), current.fruits(), current.fats(), current.vegetables(), current.dairy()},
                seconds, sugarChanged, alcoholChanged, (float) state.sugar, (float) state.alcohol,
                presentation.sugarSeconds(), presentation.alcoholSeconds()));
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
                    && state.sugar >= .15) {
                metabolicDiscovery(player, com.bettercontent.bettersurvivalphysiology.api.event.MetabolicDiscoveryEvent.Kind.SUGAR_CRASH,
                        "Sweetness drained " + NUTRIENT_ASPECTS[index].displayName + " below 20%");
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

    private record ConsumptionStart(NutritionSnapshot nutrition, double sugar, double alcohol,
                                    boolean edible) {}
    private record PendingMeal(ConsumptionStart start, long dueTick) {}
    private record EffectStamp(int duration, int amplifier) {}
    private record PendingFruitEffects(Map<MobEffect, EffectStamp> before, long dueTick) {}

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
