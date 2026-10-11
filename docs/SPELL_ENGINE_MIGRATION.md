# Spell Engine → optional migration

Goal: boss mechanics run on rpg-minibosses' own abstractions, visuals on AtlasVFX; Spell Engine and
Spell Power become optional. Everything that needs them lives in `compat/spellengine/` and is only
loaded when Spell Engine is installed.

## Phase status

| Phase | Content | Status |
|---|---|---|
| 0 | Dead-import sweep, spell classification | done |
| 1 | Isolate Spell Engine content behind `compat.SpellEngineCompat` (SE still required) | done, checked in-game |
| 2a | Architectury API replaces Forgified Fabric API; AtlasVFX added (jar-in-jar) | done — awaiting in-game check |
| 2b | Core mechanics in `magic/` (see below) | done — awaiting in-game check |
| 3 | `BossSounds` + sound assets + THIRD_PARTY_NOTICES | done — awaiting in-game check |
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

## Mechanics core (phase 2b)

Package `magic/`, no Spell Engine types:

| Piece | Role |
|---|---|
| `AbilitySpec` (+ `AbilityRegistry`) | One ability: school, intent, `Delivery`, `Impact`s. Built in code; replaces a boss spell JSON |
| `Delivery` | `Direct`, `Area` (AtlasVFX `TelegraphArea`, anchored on caster or committed target), `Projectile`, `Meteor`, `Cloud` |
| `Impact` | `Damage` (Spell Engine's formula), `Ignite`, `ApplyEffect`, `Knockback`, `Heal` |
| `MagicSchool` | Spell Power's schools; magical ones have damage types `rpg-minibosses:<school>`, tagged like Spell Power's |
| `MagicCaster` | Bosses state their own power per school (config values) |
| `MagicBackend` | `VanillaMagicBackend` (no crits, own damage types) or `compat/spellengine/SpellPowerMagicBackend` (crit, vulnerability, Spell Power damage types) |
| `Relations`, `AbilityTargets` | Who can be hurt/helped; footprint hit-test against a `TelegraphArea` |
| `TickScheduler` | Per-world delayed tasks (replaces `WorldScheduler`) |
| `entity/BossProjectile`, `entity/BossCloud` | Transient carriers; no visuals until the cue layer |
| `CombatAction.Builder.cast(spec)` | Fired by `ActionRunner` as the active window opens, at the committed target/heading |

`Relations.canHarm` delegates to `MinibossEntity.canHarm` for minibosses, which still ends in Spell
Engine's `EntityRelations` until phase 5 ports it.

Test from in-game with `/rpgminibosses ability rpg-minibosses:debug/<bolt|fireball|nova|ring|cone|line|meteor|cloud|heal> [power]`
(aims at the entity or block under the crosshair) and `/rpgminibosses backend`.

## Sounds (phase 3)

`magic/BossSounds` registers `rpg-minibosses:boss.<key>` events: per school `windup` / `release` /
`impact` (+ `loop` for fire, frost, lightning, soul), physical weapon sounds, teleport, shield,
explosion. `BossSounds.of(school)` gives a school's set; physical schools map to sword / bow sounds.

`tools/convert_sounds.py` holds the key → source-file table, converts to mono trimmed Ogg into
`assets/rpg-minibosses/sounds/boss/`, and adds missing `sounds.json` keys. It never overwrites
(`--force` backs up first) and never edits an existing `sounds.json` key, so swap a sound by editing
`sounds.json` directly. Licences: `THIRD_PARTY_NOTICES.md` (also shipped in the jars).

Audition in-game: `/playsound rpg-minibosses:boss.fire.release master @s`.

## Loader API rule (phase 2a)

Common code uses Architectury API, never Fabric API: `common/build.gradle` excludes the
`net.fabricmc.fabric-api` group from every configuration, so a Fabric API import is a compile error.
Fabric-only code (datagen, `fabric/mixin/EndSkyMixin`, the `WorldRenderEvents` adapter) lives in the
`fabric` module. Things Architectury lacks are bridged in core: `client/WorldRenderHook` (each loader
feeds its after-entities render event) and `entity/EntityTracking` (stand-in for `PlayerLookup.tracking`).
Payload receivers are declared in `RPGMinibossesClient.earlyInit()`, called during mod construction.

## Known issues found along the way

- Magus's spawn egg now registers after the Gemini eggs (it is appended by the compat layer), so it moved
  to the end of the creative tab's egg list.
