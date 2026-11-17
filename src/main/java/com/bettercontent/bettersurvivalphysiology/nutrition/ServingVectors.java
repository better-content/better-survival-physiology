package com.bettercontent.bettersurvivalphysiology.nutrition;

import com.bettercontent.bettersurvivalphysiology.config.SalienceConfig;
import com.illusivesoulworks.diet.api.type.IDietResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Source labels are only inputs. CanonicalServing is the only shape allowed at a tracker write.
 * DietApi.get() returns an already-canonical native result: never pass it through SourceServing.
 * Native Diet groups are a set (including recipe inheritance), not ingredient quantities. */
public final class ServingVectors {
    private static final Set<String> SOURCES = Set.of("proteins", "grains", "fruits", "sugars", "sugar",
            "vegetables", "dairy", "spice", "alcohol");

    private ServingVectors() {}

    /** Shared paid milk-bucket source, used by direct and fractional capture before intake. */
    public static CanonicalServing milk() { return new SourceServing(Map.of("dairy", .06)).canonical(); }
    public static CanonicalServing milk(double share) { return new SourceServing(Map.of("dairy", .06)).canonical(share); }

    public record SourceServing(Map<String, Double> contributions) {
        public SourceServing {
            contributions = checked(contributions, SOURCES);
        }

        public CanonicalServing canonical() {
            return canonical(SalienceConfig.value(SalienceConfig.DAIRY_PROTEIN_SHARE, .80));
        }

        public CanonicalServing canonical(double dairyProteinShare) {
            if (!Double.isFinite(dairyProteinShare) || dairyProteinShare < 0 || dairyProteinShare > 1)
                throw new IllegalArgumentException("Invalid dairy Protein share");
            Map<String, Double> result = new HashMap<>();
            contributions.forEach((source, gain) -> {
                if (gain == 0) return;
                switch (source) {
                    case "proteins" -> result.merge("proteins", gain, Double::sum);
                    case "grains", "fruits", "sugars", "sugar" -> result.merge("carbs", gain, Double::sum);
                    case "dairy" -> {
                        result.merge("proteins", gain * dairyProteinShare, Double::sum);
                        result.merge("carbs", gain * (1 - dairyProteinShare), Double::sum);
                    }
                    case "vegetables", "spice", "alcohol" -> result.merge(source, gain, Double::sum);
                    default -> throw new IllegalArgumentException("Unsupported source " + source);
                }
            });
            return new CanonicalServing(result);
        }

        /** ONE paid ingredient: overlapping Carb source labels and its sugar mirror describe
         * the SAME food, so take the largest resolved entitlement once. Separately contributed
         * dairy Carbs survive, and distinct ingredients are summed by the caller AFTER this step. */
        public CanonicalServing canonicalIngredient(double sugarMirror, double alcoholPotency) {
            if (!Double.isFinite(sugarMirror) || sugarMirror < 0
                    || !Double.isFinite(alcoholPotency) || alcoholPotency < 0)
                throw new IllegalArgumentException("Invalid paid ingredient mirrors");
            double carbohydrate = Math.max(sugarMirror, Math.max(contributions.getOrDefault("grains", 0.0),
                    Math.max(contributions.getOrDefault("fruits", 0.0),
                    Math.max(contributions.getOrDefault("sugars", 0.0), contributions.getOrDefault("sugar", 0.0)))));
            double share = SalienceConfig.value(SalienceConfig.DAIRY_PROTEIN_SHARE, .80);
            Map<String, Double> canonical = new HashMap<>(canonical(share).gains());
            canonical.put("carbs", carbohydrate + contributions.getOrDefault("dairy", 0.0) * (1 - share));
            canonical.merge("alcohol", alcoholPotency, Math::max);
            return new CanonicalServing(canonical);
        }
    }

    public record CanonicalServing(Map<String, Double> gains) {
        public CanonicalServing {
            gains = checked(gains, NativeDietRoster.GROUPS);
        }

        /** Native Diet has already collapsed overlapping tags to one group and calculated its
         * gain; summing only *distinct native result entries* cannot award a source twice. */
        public static CanonicalServing fromNative(IDietResult result) {
            Map<String, Double> gains = new HashMap<>();
            result.get().forEach((group, amount) -> {
                if (group == null || amount == null) throw new IllegalArgumentException("Missing native Diet gain");
                gains.merge(group.getName(), amount.doubleValue(), Double::sum);
            });
            return new CanonicalServing(gains);
        }

        /** Native canonical gains and separately resolved optional potency refer to ONE
         * ingredient. A mirror can raise its one Carb/Alcohol contribution, never add a second
         * meter; apply before aggregating separate ingredients. No SOURCE remapping here. */
        public CanonicalServing resolveNativeMirrors(double sugar, double alcohol) {
            if (!Double.isFinite(sugar) || sugar < 0 || !Double.isFinite(alcohol) || alcohol < 0)
                throw new IllegalArgumentException("Invalid native ingredient mirror");
            Map<String, Double> updated = new HashMap<>(gains);
            updated.merge("carbs", sugar, Math::max);
            updated.merge("alcohol", alcohol, Math::max);
            return new CanonicalServing(updated);
        }
    }

    private static Map<String, Double> checked(Map<String, Double> values, Set<String> allowed) {
        if (values == null) throw new IllegalArgumentException("Missing serving vector");
        values.forEach((group, amount) -> {
            if (!allowed.contains(group) || amount == null || !Double.isFinite(amount) || amount < 0)
                throw new IllegalArgumentException("Invalid serving contribution: " + group);
        });
        return Map.copyOf(values);
    }
}
