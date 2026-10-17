# Better Survival Physiology

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Better Survival Physiology is the deliberately thin Better Content integration layer for dynamic bodily state. Diet remains the owner of ordinary nutrient values and its native screen; Brewin' and Chewin', Thirst Was Taken, Cold Sweat, Epic Fight, and vanilla remain the owners of their mechanics. This mod reads Diet nutrition for threshold interactions while retaining only optional sugar loading and debt, inverted-U alcohol, and compatibility cues.

Food-group classification and recipe inheritance use Diet's native rules: direct food tags take precedence, and Diet generates ingredient-based groups for otherwise ungrouped foods. This mod does not extend inheritance through arbitrary recipe intermediates or merge generated groups into directly tagged foods.

On the server, Better Survival Physiology also owns the authoritative hunger-versus-nutrition episode boundary. A full hunger bar with any ordinary Diet group below the configured ordinary threshold starts one persisted episode; a completed edible use that brings all six groups to that threshold after Diet's update ends it. When Better Content Threads is installed, those transitions are emitted through its public reflection API with one correlation token.

Each ordinary food group owns one behavioral identity: Proteins are Impact, Grains are Work,
Fruits are Mobility, Fats are Endurance, Vegetables are Robustness, and Dairy is Renewal.
Sugar expresses Tempo by amplifying and spending the state already built; Alcohol expresses
Control through its useful midpoint and independently rising impairment.
Sugar's exponential consumption multiplier (`8^load`) applies uniformly to the six ordinary
nutrients, sugar's own decay, and alcohol clearance. Sugar retains its baseline exponential
decay curve and alcohol its linear clearance curve, but both run faster while sugar is present;
the sugar multiplier is recalculated as sugar falls. The configured sugar half-life is the
baseline before this self-acceleration, not a guaranteed wall-clock half-life.

Opening Diet uses Diet's native screen and navigation. Better Survival Physiology no longer replaces that
screen or adds a separate metabolism panel; its gameplay effects and contextual feedback continue
to operate independently. A single graphical nutrition strip above the hotbar shows every
positive current nutrient, Nectar and Draught value with its aspect badge and percentage,
including values below ability thresholds. Zero values are omitted; positive traces display
`<1%` rather than rounding to zero, and metabolic loads can exceed 100%. The strip wraps on
narrow windows and is hidden while a screen is open or the HUD is hidden. There is no separate
text meal recap.

The eight identities use short physical-foley gestures with a quiet one-second room decay.
Nutrition, RPG allocation, and Matter use related contextual renders rather than identical
files. Sugar crash deliberately receives a broken Tempo gesture. The reproducible renderer,
checked-in CC0 excerpts, exact hashes, and source provenance live under `audio/` and `tools/`.

World owners can tune optional sugar/alcohol half-lives, threshold cues, and `item_id=load` alcohol profiles in `better_survival_physiology-server.toml`; Diet owns nutrition values, decay, effects, and saves. Defaults remain the authored Better Content balance contract, so ordinary iteration does not require a mod rebuild.

It is not a global aspect registry and the other Better Content mods do not depend on it.

## Build

```sh
./gradlew verifyFull
```
