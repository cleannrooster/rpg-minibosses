# Spell Engine → optional migration

Goal: boss mechanics run on rpg-minibosses' own abstractions, visuals on AtlasVFX; Spell Engine and
Spell Power become optional. Everything that needs them lives in `compat/spellengine/` and is only
loaded when Spell Engine is installed.

## Phase status

| Phase | Content | Status |
|---|---|---|
| 0 | Dead-import sweep, spell classification | done |
| 1 | Isolate Spell Engine content behind `compat.SpellEngineCompat` (SE still required) | done — awaiting in-game check |
| 2 | Core mechanics (`CombatAction.cast`, `Impact`, `MagicDamage`, `CasterPower`, `Relations`, `TickScheduler`) | |
| 3 | `BossSounds` + sound assets + THIRD_PARTY_NOTICES | |
| 4 | Cue layer (`FxEmitter` / `FxCueHandler`) + telegraphs | |
| 5 | Port bosses: Mercenary → Rogue → Juggernaut → Templar → FireMage → Gemini | |
| 6 | `SlashVisual` → AtlasVFX `ArcSweep` trails | |
| 7 | Replace `Synchronized` / `CustomModelStatusEffect` on core bosses (GeminiRenderer) | |
| 8 | Flip deps to optional, load conditions on SE-only data, bytecode scan, 4-way test | |

## How the gate works

- `compat/SpellEngineCompat` (core, no SE types): presence probe by class resource, reflective load of
  `compat.spellengine.SpellEngineContent`, no-op `Hooks` otherwise. Each `RPGMinibosses` registry phase
  calls the matching hook, so NeoForge `RegisterEvent` phases are unchanged.
- `compat/SpellEngineCompatClient` loads `SpellEngineContentClient` the same way.
- `rpg-minibosses-spell_engine.mixins.json` is gated by `compat/SpellEngineMixinPlugin`.
- `client/DarkMatterView` lets the core sky/render mixins (shared with the guild hall) ask about the
  Magus dark-matter view without referencing Magus classes.

**Invariant:** no class outside `compat/spellengine/` references `net/spell_engine` or `net/spell_power`
in bytecode. Allowed exceptions: the probe string in `SpellEngineCompat`, the Fabric datagen classes
(only run by the `fabric-datagen` entrypoint). Everything else on the scan list is phase 2–7 debt.

## Spell classification (44 spell JSONs)

### Core bosses — port to own abilities (phases 2/5)

| Spell | Used by |
|---|---|
| explosion | Mercenary trap (`MercenaryBrain`, `TrapCleann`) |
| knifethrow | Rogue |
| pound | Juggernaut, `MinibossEntity` (also Magus — shared) |
| divine_fall | Templar |
| fireball, lesser_fire_volley | FireMage |
| fire_nova | FireMage (also Magus — shared) |
| beam, beam_cold, comet, starfall, flame_cloud, frost_cloud | Gemini |
| deatomization_storm | `StormAnchorEntity` (Gemini uber; also the storm scroll — shared) |

### Forsaken Magus — stays Spell Engine (compat)

amethyst_chunk, arcane_nova, arcane_projectile, arctic_armor, deathchill_nova, fire_volley, frostferno,
greater_fireball, ice_bolt, ice_chunk, lightning_fall, magus_dark_matter_proj, magus_fire_dominion,
phoenix_nova, plasma_blast, rain_of_fire, self_immolate, soul_burst, supernova, firefeather2 (Feather
effect, applied by phoenix_nova), plus its uses of pound and fire_nova.

### Items — stay Spell Engine (compat)

| Spell | Item |
|---|---|
| abberrath_nova | Abberrath's Hooves (`PlayerMixin`) |
| ice_storm | Whispering Ice |
| despot, foxshade, kintsugi, savant | unique armor set bonuses |
| summon_deatomization_storm | Gemini uber scroll drop (`summon_storm` custom delivery) |

### Shared — split (items/Magus keep SE JSON, core boss gets its own ability)

pound, fire_nova, deatomization_storm.

### Unreferenced

beartrap, holy_burst, dark_matter (the spell; the `dark_matter` effect and entity are separate and live).

## Known issues found along the way

- `compat/spellengine/mixin/SpellProjectileMixin` compares the spell id against
  `"rpg_minibosses:magus_dark_matter_proj"` but the namespace is `rpg-minibosses`, so the dark-matter
  orb redirect likely never fires.
- `./gradlew :fabric:runClient` fails resolution: Spell Engine 1.10 requires `playeranimator`, whose
  dependency line is commented out in `fabric/build.gradle`. Pre-existing.
- Magus's spawn egg now registers after the Gemini eggs (it is appended by the compat layer), so it moved
  to the end of the creative tab's egg list.
