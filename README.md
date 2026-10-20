# Better Survival Physiology

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Better Survival Physiology is the deliberately thin Better Content integration layer for dynamic bodily state. Diet remains the owner of ordinary nutrient values and its native screen; Brewin' and Chewin', Thirst Was Taken, Cold Sweat, Epic Fight, and vanilla remain the owners of their mechanics. This mod reads Diet nutrition for continuous primary benefits and group-specific behavioural unlocks, and owns Nectar/Booze loading, impairment, blackout and compatibility cues.

Food-group classification and recipe inheritance use Diet's native rules: direct food tags take precedence, and Diet generates ingredient-based groups for otherwise ungrouped foods. This mod does not extend inheritance through arbitrary recipe intermediates or merge generated groups into directly tagged foods.

On the server, Better Survival Physiology also owns the authoritative hunger-versus-nutrition episode boundary. A full hunger bar with any ordinary Diet group below the configured ordinary threshold starts one persisted episode; a completed edible use that brings all six groups to that threshold after Diet's update ends it. When Better Content Threads is installed, those transitions are emitted through its public reflection API with one correlation token.

## Nutrition balance

Every ordinary group grants its primary benefit continuously from the first positive trace.
At zero there is no bonus or deficiency penalty. For these formulas, `n` is the group's
Diet value clamped to `0–1`, and `a = 1 + Nectar load` (load clamped to `0–1`).

| Group / aspect | Continuous primary | Behavioural unlocks, based on unamplified nutrition |
| --- | --- | --- |
| Flesh / Impact | Melee damage `+40% × n × a` | 40%: charged strike after 4s quiet, +50% damage, 12s base cooldown; 80%: 35% splash to up to two nearby foes |
| Grain / Work | Correct-tool mining speed `+100% × n × a` | 40%: up to five successive-break bonuses of +10%; 60%: sequence grace grows from 3s to 5s; 80%: five breaks grant a 5s +100% burst |
| Fruit / Mobility | Movement and swimming `+25% × n × a` | 20%: fall damage reduction `35% × n × a`, capped at 70%; 40%: +12% stride after 3s sprinting; 80%: sprinting leap, 12s base cooldown |
| Tallow / Endurance | Hunger, thirst and stamina costs reduced by `min(85%, 50% × n × a)` | 80%: low reserves trigger 10s zero ordinary cost, 2m cooldown; load surcharges still apply |
| Earth / Robustness | Temperature drift reduced by `min(85%, 70% × n × a)` | 80%: cancel one temperature hit and pull temperature safe, 30s base cooldown |
| Cream / Renewal | Harmful-effect expiry `1 + n × a` times normal speed | 40%: edible meal heals one heart, 30s base cooldown; 80%: cleanse one finite harmful effect, 20s base cooldown |

Cold Sweat 2.4 exposes body temperature as the read-only sum `BODY = CORE + BASE`.
Earth's drift correction and temperature-hit guard read `BODY`, then write the desired
body value through `CORE = desiredBody - BASE`, preserving the native base contribution.

Nectar multiplies nutrient potency, supported perk durations and inverse cooldowns; it does
not move unlock thresholds or amplify RPG allocations. Full Nectar doubles ordinary primary
potency, subject to the reduction caps. Cream's fractional expiry is accumulated, not rounded
up each tick: 1% Cream adds 1% expiry speed, not an immediate extra tick every tick.

Booze reduces incoming damage by `min(30%, 30% × load × a)`. Impairment is
`min(1, max(0, (load - 0.30) / 0.70) × a)`: it reduces movement by up to 75%,
attack/item-use/mining speed by up to 90%, and adds up to 200% resource costs.
Nectar amplifies **both** Booze protection and impairment, within their caps. Blackout
starts at 100% raw Booze load and ends at 85%; Nectar does not move those thresholds.
Neither load is needed for dietary completeness.

RPG development owns complementary capabilities, not another copy of these primaries:
Strength owns knockback/stagger, Hand Speed cadence, Tool Handling block reach, Athletics
step/air capacity, Endurance stamina capacity, Hardiness knockback resistance, Preservation
beneficial-effect duration and milk preservation, and Marksmanship projectile handling.
Nutrition no longer adds step height, knockback/stagger, beneficial-effect preservation or
Earth movement speed. Heavy Blow and its splash retain damage rather than extra stagger.


Sugar's exponential consumption multiplier (`8^load`) applies uniformly to the six ordinary
nutrients, sugar's own decay, and alcohol clearance. Sugar retains its baseline exponential
decay curve and alcohol its linear clearance curve, but both run faster while sugar is present;
the sugar multiplier is recalculated as sugar falls. The configured sugar half-life is the
baseline before this self-acceleration, not a guaranteed wall-clock half-life.

Nectar also accelerates thirst: the shared resource-cost hook adds `8^load - 1` to the multiplier for
incoming native thirst exhaustion, alongside the ordinary cost after Tallow savings and any Booze surcharge.
Tallow's emergency reserve cannot cancel this extra cost. Thirst still owns quenching depletion,
hydration depletion, environmental modifiers and synchronization; no second thirst meter is added.

Opening Diet uses Diet's native screen and navigation. Better Survival Physiology no longer replaces that
screen or adds a separate metabolism panel; its gameplay effects and contextual feedback continue
to operate independently. A single graphical nutrition strip above the hotbar shows every
positive current nutrient, Nectar and Booze value with its aspect badge and percentage,
including values below ability thresholds. Zero values are omitted; positive traces display
`<1%` rather than rounding to zero, and metabolic loads can exceed 100%. The strip wraps on
narrow windows and is hidden while a screen is open or the HUD is hidden. There is no separate
text meal recap. Shared Journal nutrient hover copy stays compact: direct effect labels and short
perk lines retain values, triggers, cooldowns and drawbacks without repeating continuous-scaling prose.
This is copy-only; Journal layout, unlock thresholds and gameplay calculations are unchanged.

Heavy Blow readiness uses one short Impact sound and radial particle burst at the player on
entering the ready state; the existing discharge cue stays at the struck target. Combat and
feedback share the same protein threshold, 12-second base cooldown, four-second attack quiet
period and 90% native attack-charge check. Blackout cannot show ready. Initial login/reconnect
charge-up establishes a silent baseline, not a ready notification. Item swaps and other
charge-only interruptions do not re-arm that notification while underlying eligibility remains
uninterrupted. Actual attacks or loss of eligibility (nutrition, cooldown, attack quiet period
or blackout) re-arm it; recovery announces once when native charge is sufficient. Epic Fight's
native hits still pass through the shared attack handler. Nutrition's Heavy Blow no longer adds stun or
knockback; those belong to RPG Strength. This adds no permanent readiness HUD.

The eight identities use short physical-foley gestures with a quiet one-second room decay.
Nutrition, RPG allocation, and Matter use related contextual renders rather than identical
files. Sugar crash deliberately receives a broken Tempo gesture. The reproducible renderer,
checked-in CC0 excerpts, exact hashes, and source provenance live under `audio/` and `tools/`.

World owners can tune optional sugar/alcohol half-lives, threshold cues, and `item_id=load` alcohol profiles in `better_survival_physiology-server.toml`; Diet owns nutrition values, decay, effects, and saves. Defaults remain the authored Better Content balance contract, so ordinary iteration does not require a mod rebuild.

Food hover uses one horizontal row without a heading, with graphical aspect badges, pack names,
and per-serving percentage gains. A blank line immediately above that row keeps the oversized
badges clear of preceding tooltip text; items without nutrition entries gain no extra spacing. Diet supplies the six ordinary gains; Nectar and Booze
use the actual consumption loads, and milk shows its direct Cream/Tallow award. This replaces
Diet's vertical default-name list, blank separator and “When eaten” heading; it does not add
ASCII aspect glyphs or nutrient-effect prose. Diet's native screen remains unchanged.

For currently Nectar-eligible servings, the tag-based load is an upper bound: accompanying
lasting nutrition caps Nectar at half the smallest positive whole-serving Diet gain among
proteins, grains, fruits, fats, vegetables and dairy. Even Fruit + Nectar qualifies. Pure-Nectar
servings retain their base load; recipe-inherited sugars alone do not add Nectar eligibility.
The shared serving calculation drives consumption, hover and beverage capture without reading
current nutrient levels or remaining capacity. Unknown groups and nonpositive/nonfinite gains
do not constrain the cap. Failed Diet resolution logs an error and withholds Nectar rather than
assuming a pure-Nectar serving; tooltip lookup failures retain their existing omission behavior.

## Fractional beverages

When Better Drinking Water 1.1.0 or newer is installed, its server-only typed beverage events
bridge paid fractions into Diet and the existing Nectar/Booze state. `Profile` captures native
`DietApi` whole-serving gains once, before payment, together with the existing sugar and alcohol
profiles. The bounded payload uses `diet` (only the six ordinary group IDs), `sugar`, `alcohol`,
`edible`, and the source `item` ID. Milk buckets replace ordinary lookup with exactly 0.06 dairy
and 0.06 fats per serving; milk bottles retain normal Diet lookup. Newly captured profiles store
the capped whole-serving Nectar once; sips do not recalculate it. Existing paid profiles keep
their captured entitlements across saves, and existing metabolic loads are not migrated.

Each `Sip` awards only `to - from`, requiring finite ordered endpoints in `0–1`. Profile gains
are finite, nonnegative and capped at one; negative/nonfinite gains award nothing. Nectar does
not amplify these gains. Diet remains the owner of ordinary values, saves and sync; physiology
remains the owner of metabolic loads. Blackout cancels both purchase and already-paid release.
The beverage engine owns hunger, saturation and authored effects (including Fruits Delight
normalization). This bridge never calls native item completion, Diet whole-item consumption,
or a synthetic Forge finish event, so those mechanics cannot award a second full serving.

Nutrition threshold cues and episode recovery follow the actual fractional gain. An edible
serving can trigger Cream meal recovery only upon completion, not on every sip; native Diet
food discovery is recorded at completion. Fractional state is saved and synced immediately.
The engine owns fraction sequencing/replay prevention; these hooks additionally reject invalid
endpoints and defensively refuse release during blackout. Without Better Drinking Water, the
integration class is not registered and ordinary native item consumption remains unchanged.

It is not a global aspect registry and the other Better Content mods do not depend on it.

## Build

```sh
./gradlew verifyFull
```

Stage Better Drinking Water first, in dependency order. The compile/runtime integration uses
`../better-drinking-water/build/libs/better-drinking-water-1.1.0.jar`; pass
`-PdrinkingWaterRuntimeJar=/absolute/path/to/provider.jar` for an isolated provider snapshot.
The build copies exact bytes into a SHA-256-versioned local flat-directory artifact under
`.gradle/beverage-provider/`, outside the cleanable `build/` tree, so ForgeGradle actually remaps it
(file-collection dependencies bypass remapping). The provider is an optional
installed mod, not bundled into this artifact.

`verifyFast` covers fraction conservation, hostile bounds and the pinned native Diet boundary.
`verifyNativeBeverages` runs six nonshipping Forge GameTests with real Diet, the complete beverage
provider and its core native dependencies: milk bucket fractional awards without Nectar amplification,
milk bottle native lookup, honey/beer metabolic doses and persistence, completion-only meal recovery,
blackout/malformed-fraction rejection, and Nectar's actual native thirst exhaustion/depletion
(including the emergency-reserve boundary). `verifyFull` includes both plus runtime staging. Every native
run uses a fresh retained directory beneath `build/gametest/`; its fixtures are excluded from the
production JAR. Native runtime diagnostics reject failed-operation/ERROR/FATAL records. The fixture
mirrors the provider's authored pack-side Respite/Create recipe repairs, without changing production
resources. Source fixture success is not full-pack runtime acceptance.
