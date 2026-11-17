package com.bettercontent.bettersurvivalphysiology.climate;

import com.bettercontent.bettersurvivalphysiology.SystemicSalienceMod;
import com.momosoftworks.coldsweat.api.event.core.init.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.*;
import com.momosoftworks.coldsweat.api.temperature.modifier.compat.SereneSeasonsTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.compat.StormTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.compat.ValkShipBlockTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import java.util.*;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Pinned Cold Sweat 2.4 ambient-only rate attribution. Does not alter WORLD, CORE or insulation. */
public final class AmbientRateSeam {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PROBE = new ResourceLocation(SystemicSalienceMod.MOD_ID, "ambient_world_probe");
    private static final ResourceLocation RATE = new ResourceLocation(SystemicSalienceMod.MOD_ID, "ambient_rate");
    private static final double EPS = 1e-9, TARGET_TICKS = 72_000D;
    private static final int MAX_DIAGNOSTICS = 32;
    private static final Set<String> REPORTED = new HashSet<>();
    private static final Map<ServerPlayer, Frame> FRAMES = new WeakHashMap<>();

    private AmbientRateSeam() {}
    /** Call from the mod entrypoint only when Cold Sweat is loaded; never from the MOD bus. */
    public static void register() { MinecraftForge.EVENT_BUS.register(AmbientRateSeam.class); }

    private static void diagnose(String reason) {
        if (REPORTED.size() < MAX_DIAGNOSTICS && REPORTED.add(reason))
            LOGGER.warn("Ambient rate attribution disabled: {}", reason);
    }
    private static boolean registered() {
        return TempModifierRegistry.containsKey(PROBE) && TempModifierRegistry.containsKey(RATE);
    }
    @SubscribeEvent public static void registerModifiers(TempModifierRegisterEvent event) {
        event.register(PROBE, WorldProbe::new);
        event.register(RATE, AmbientRateModifier::new);
    }

    private static boolean ambient(TempModifier modifier) {
        Class<?> type = modifier.getClass();
        return type == BiomeTempModifier.class || type == ElevationTempModifier.class
                || type == ShadeTempModifier.class || type == CaveBiomeTempModifier.class
                || type == SereneSeasonsTempModifier.class || type == StormTempModifier.class;
    }
    private static boolean local(TempModifier modifier) {
        Class<?> type = modifier.getClass();
        return type == BlockTempModifier.class || type == EntitiesTempModifier.class
                || type == ThermalSourceTempModifier.class || type == WaterTempModifier.class
                || type == ValkShipBlockTempModifier.class;
    }
    private static String signature(List<TempModifier> modifiers) {
        StringBuilder result = new StringBuilder();
        for (TempModifier modifier : modifiers) {
            if (result.length() > 220) break;
            if (!result.isEmpty()) result.append(',');
            result.append(modifier.getClass().getSimpleName());
        }
        return result.toString();
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void gather(GatherDefaultTempModifiersEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (!registered()) { diagnose("Cold Sweat modifier registry not ready"); return; }
        if (event.getTrait() == Temperature.Trait.RATE) {
            event.addModifier(new AmbientRateModifier());
            return;
        }
        if (event.getTrait() != Temperature.Trait.WORLD) return;
        List<TempModifier> original = new ArrayList<>(event.getModifiers());
        int firstLocal = -1;
        boolean biome = false, block = false;
        for (int i = 0; i < original.size(); i++) {
            TempModifier modifier = original.get(i);
            if (ambient(modifier) && firstLocal < 0) {
                biome |= modifier.getClass() == BiomeTempModifier.class;
            } else if (local(modifier)) {
                if (firstLocal < 0) firstLocal = i;
                block |= modifier.getClass() == BlockTempModifier.class;
            } else {
                diagnose("unknown/interleaved WORLD: " + signature(original));
                return;
            }
        }
        if (!biome || !block || firstLocal < 1 || original.size() > 32) {
            diagnose("missing ambient/local WORLD boundary: " + signature(original));
            return;
        }
        int locals = original.size() - firstLocal;
        List<TempModifier> withProbes = new ArrayList<>(original.size() + locals + 2);
        withProbes.addAll(original.subList(0, firstLocal));
        withProbes.add(new WorldProbe(0, locals));
        for (int i = 0; i < locals; i++) {
            withProbes.add(original.get(firstLocal + i));
            withProbes.add(new WorldProbe(i + 1, locals));
        }
        withProbes.add(new WorldProbe(locals + 1, locals));
        event.getModifiers().clear();
        event.getModifiers().addAll(withProbes);
    }

    /** Recheck the live list: later mods may insert sources after our final probe. */
    private static int verifiedLocals(ServerPlayer player) {
        List<TempModifier> list = Temperature.getModifiers(player, Temperature.Trait.WORLD);
        if (list.isEmpty() || list.size() > 70) return -1;
        int index = 0;
        boolean biome = false, block = false;
        while (index < list.size() && ambient(list.get(index))) {
            biome |= list.get(index).getClass() == BiomeTempModifier.class;
            index++;
        }
        if (!biome || index >= list.size() || !(list.get(index) instanceof WorldProbe first)
                || first.stage() != 0) return -1;
        int locals = first.locals();
        index++;
        if (locals < 1 || locals > 32) return -1;
        for (int stage = 1; stage <= locals; stage++) {
            if (index >= list.size() || !local(list.get(index))) return -1;
            block |= list.get(index++).getClass() == BlockTempModifier.class;
            if (index >= list.size() || !(list.get(index) instanceof WorldProbe probe)
                    || probe.stage() != stage || probe.locals() != locals) return -1;
            index++;
        }
        return block && index == list.size() - 1 && list.get(index) instanceof WorldProbe finalProbe
                && finalProbe.stage() == locals + 1 && finalProbe.locals() == locals ? locals : -1;
    }

    private static void record(ServerPlayer player, int stage, int locals, double value) {
        Frame frame = FRAMES.computeIfAbsent(player, ignored -> new Frame());
        if (frame.tick != player.tickCount) frame.reset(player.tickCount);
        frame.accept(stage, locals, value);
    }

    /** Probes retain only current-tick values, never a damage/exposure counter. */
    private static final class Frame {
        int tick = -1, seen = -1, locals;
        double ambient, previous;
        boolean safe, sourceActive, complete;
        void reset(int currentTick) {
            tick = currentTick; seen = -1; locals = 0; safe = false; sourceActive = false; complete = false;
        }
        void accept(int stage, int expected, double value) {
            if (complete || !Double.isFinite(value) || expected < 1 || expected > 32) { safe = false; return; }
            if (stage == 0) {
                if (seen != -1) { safe = false; return; }
                locals = expected; seen = 0; safe = true; ambient = previous = value;
            } else if (safe && expected == locals && stage == seen + 1 && stage <= expected + 1) {
                if (stage <= expected) sourceActive |= Math.abs(value - previous) > EPS;
                else complete = true;
                seen = stage; previous = value;
            } else safe = false;
        }
    }

    public static final class WorldProbe extends TempModifier {
        public WorldProbe() { this(0, 0); }
        private WorldProbe(int stage, int locals) {
            getNBT().putInt("stage", stage);
            getNBT().putInt("locals", locals);
            tickRate(1);
        }
        int stage() { return getNBT().getInt("stage"); }
        int locals() { return getNBT().getInt("locals"); }
        @Override protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
            return input -> {
                if (trait == Temperature.Trait.WORLD && entity instanceof ServerPlayer player)
                    record(player, stage(), locals(), input);
                return input;
            };
        }
    }

    public static final class AmbientRateModifier extends TempModifier {
        public AmbientRateModifier() { tickRate(1); }
        @Override protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait) {
            return nativeRate -> {
                if (trait != Temperature.Trait.RATE || !(entity instanceof ServerPlayer player)
                        || !Double.isFinite(nativeRate) || nativeRate == 0) return nativeRate;
                Frame frame = FRAMES.get(player);
                if (frame == null || frame.tick != player.tickCount || !frame.safe || !frame.complete
                        || frame.seen != frame.locals + 1 || frame.sourceActive) return nativeRate;
                int locals = verifiedLocals(player);
                if (locals != frame.locals) {
                    diagnose("live WORLD layout changed or unclassified source: "
                            + signature(Temperature.getModifiers(player, Temperature.Trait.WORLD)));
                    return nativeRate;
                }
                List<TempModifier> rates = Temperature.getModifiers(player, Temperature.Trait.RATE);
                if (rates.isEmpty() || rates.get(rates.size() - 1) != this) {
                    diagnose("RATE modifier is not after native insulation");
                    return nativeRate;
                }
                double f = Temperature.get(player, Temperature.Trait.FREEZING_POINT);
                double b = Temperature.get(player, Temperature.Trait.BURNING_POINT);
                double base = Temperature.get(player, Temperature.Trait.BASE);
                double body = Temperature.get(player, Temperature.Trait.BODY);
                Double configuredRate = ConfigSettings.TEMP_RATE.get();
                if (!Double.isFinite(f) || !Double.isFinite(b) || f >= b
                        || !Double.isFinite(base) || Math.abs(base) > EPS || !Double.isFinite(body)
                        || configuredRate == null || !Double.isFinite(configuredRate) || configuredRate <= 0)
                    return nativeRate;
                double delta = frame.ambient < f ? frame.ambient - f
                        : frame.ambient > b ? frame.ambient - b : 0;
                if (delta == 0 || !Double.isFinite(delta) || Math.signum(delta) != Math.signum(nativeRate)
                        || Math.signum(body) == -Math.signum(delta)) return nativeRate;
                double naked = Math.max(Math.abs(delta) * configuredRate / 7D, configuredRate / 50D);
                if (!Double.isFinite(naked) || naked <= 0) return nativeRate;
                double factor = Math.min(1D, (100D / TARGET_TICKS) / naked);
                return Double.isFinite(factor) && factor > 0 ? nativeRate * factor : nativeRate;
            };
        }
    }

    @SubscribeEvent public static void rateLast(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !registered()) return;
        List<TempModifier> rates = Temperature.getModifiers(player, Temperature.Trait.RATE);
        AmbientRateModifier own = null;
        for (TempModifier modifier : rates) if (modifier instanceof AmbientRateModifier rate) {
            if (own != null) { diagnose("duplicate ambient RATE modifiers"); return; }
            own = rate;
        }
        if (own != null && rates.get(rates.size() - 1) != own) {
            // Native insulation can be inserted after defaults on equipment change.
            // Move only our modifier, preserving all native modifiers' order.
            Temperature.removeModifiers(player, Temperature.Trait.RATE, AmbientRateModifier.class);
            Temperature.addModifier(player, new AmbientRateModifier(), Temperature.Trait.RATE, Placement.LAST);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FRAMES.remove(player);
    }
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) FRAMES.remove(player);
    }
}
