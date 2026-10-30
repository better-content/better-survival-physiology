# Optional culinary serving provider (source-only)

`com.bettercontent.bettersurvivalphysiology.api.CulinaryNutritionApi` is owned by physiology; there is no dependency back on cooking.

```java
record Serving(Map<String, Double> unscaled, Map<String, Double> adjusted,
               double sugar, double alcohol)
interface Provider { Optional<Serving> resolve(Player player, ItemStack stack); }
static void register(ResourceLocation owner, Provider provider)
static Optional<Serving> resolve(Player player, ItemStack stack)
static boolean complete(ServerPlayer player, ItemStack stack)
```

Register during common setup, before any resolution. Duplicate owner IDs, late registration (after first resolve) and overlapping matches fail closed. Providers must be deterministic, side-effect-free and accept a null player for preflight and native Diet suppression. Empty means leave existing behavior unchanged.

The maps are immutable **final per-edible-portion entitlements**. The producer already divides ingredient provenance by actual yield and applies quality exactly once to ordinary nutrients. The provider must not add direct-tag credit after derived ingredient credit. `unscaled` retains the unscaled entitlement for inspection; `adjusted` alone is awarded/displayed. Accepted ordinary keys are proteins, grains, fruits, fats, dairy and vegetables. Spice is excluded; the existing Spice bridge resolves the actual unscaled dose separately. Sugar and alcohol are final actual loads, not quality-scaled and not recapped from adjusted nutrients.

## Single completion owner

There is **no automatic culinary completion listener**. The registering consumer must call `complete` exactly once after actual payment/portion commit (normal Finish or explicit placed/custom/nested serving). Never call from preview, start, cancellation, a rejected unchanged result or twice from overlapping handlers. Resolution does not mutate player state. `complete` returns false if unmanaged. It awards ordinary Diet gains, actual Spice Diet gain, sugar/alcohol, blackout location, persistence/sync and existing meal feedback. It does not apply Spice HP cost: register the existing SpiceRuntime external completion owner and apply its actual units once in the central committed-serving dispatcher.

Matched stacks suppress native Diet ConsumeItemStack intake. SalienceEvents skips its native managed sugar/alcohol/meal intake. Existing Fruits Delight start/stop effect baselines and post-completion caps are retained. Tooltip and completion both use this resolver and the same canonical actual Spice resolver. No nutrition/saturation-derived fallback is added after a typed entitlement.

Existing drinking-water-managed beverages must remain outside this FOOD provider: their paid reserve, partial-sip nutrient/sugar/alcohol and effect cadence path remains authoritative. Do not award a whole FOOD serving for a paid partial sip.

## Qualification boundary

`CulinaryNutritionApiTest` covers absent/duplicate/late/overlapping providers, immutable resolved values, quality not applied again and source dispatch boundaries. Direct real-mapped javac and five JVM tests passed in `/home/dev/.tmp/cooking-providers-20261028/`. These are not native real-completion/cancellation, placed-food, UI or pack acceptance evidence. Root-owned nonshipping native fixtures must prove those transactions before activation. No production JAR was deployed.

Serialized Java17 Gradle `compileJava test stageRuntimeJar` subsequently passed (47s; 86 tests, 0 failures/errors/skips), bound to explicit current source-built Spice, intermediate Water API JAR and actual bundled Compat/Notices. This is source-build qualification only; Water's subsequent fractional-credit source correction requires its own final rebuild before native fixture binding. Build logs, source manifests and exact staged JAR hashes are retained in `/home/dev/.tmp/cooking-providers-20261028/gradle/provider-builds.json`.
