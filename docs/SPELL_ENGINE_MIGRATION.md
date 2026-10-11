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
| 4 | Cue layer (`FxEmitter` / `CueHandlers`) + telegraphs | done — awaiting in-game check |
| 5 | Port bosses: Mercenary → Rogue → Juggernaut → Templar → FireMage → Gemini | done — awaiting in-game check |
| 6 | Swept-arc slashes → AtlasVFX trails | done — awaiting in-game check |
| 7 | Core effect sync (petrification fixed); Forsaken Magus ported; boss spell JSONs deleted | done — awaiting in-game check |
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

## Cues and telegraphs (phase 4)

Server: `magic/fx/FxEmitter` sends `FxCuePayload` (cue id + `CueArgs`: school, points, size, angle,
tier, seed) and `TelegraphPayload` (a placed `TelegraphArea` + world-tick `TelegraphTiming`) to players
within 96 blocks, and plays sounds. Only AtlasVFX's server-safe telegraph geometry is touched here.
Client: `client/fx/CueHandlers` maps cue ids to AtlasVFX shapes (`FxCues`: pulse, nova, explosion,
blast, falling blast, beam, arc, cone, burst, wisp, rain), tinted by `client/fx/SchoolStyles`; it also
draws `BossProjectile` / `BossCloud` in flight. Bespoke boss cues: `CueHandlers.register(id, handler)`
on the client, `FxEmitter.emit(world, id, args)` on the server. Kept behind these two classes so the
layer can move into AtlasVFX later.

`magic/AbilityPresentation` gives every ability defaults: wind-up = school sound + telegraph of its
area (areas, clouds, bursting meteors), landing on the tick the active window opens; release = school
sound + a shape from the delivery (nova / explosion / cone / beam, muzzle pulse for projectiles);
impact = school sound + pulse, explosion (bursts) or falling blast (meteors). `AbilitySpec.Builder`
overrides: `sounds(...)`, `releaseCue`, `impactCue`, `noTelegraph()`. `ActionRunner` runs the wind-up
when an action with `cast(...)` starts.

Test: `/rpgminibosses ability rpg-minibosses:debug/nova 10 40` (2-second telegraph, then the cast).

## Boss ports (phase 5)

Every core boss casts `AbilitySpec`s built from its old spell JSON's numbers (see the javadoc on each:
`MercenaryBrain.TRAP_BLAST`, `RogueBrain.KNIFE`, `JuggernautBrain.POUND`, `TemplarBrain.DIVINE_FALL`,
`FireMageBrain.FIREBALL/VOLLEY_BOLT/NOVA`, `GeminiAbilities.*`). Bosses state their school power from
config through `MagicCaster` (`RPGMinibossesEntities.Entry.magic(...)`), no longer through Spell Power
attributes. `MinibossEntity.canHarm` carries the relation rules `EntityRelationsMixin` used to add.
Damage hits through invulnerability frames by default, as Spell Engine's did.

Deliberate differences from the Spell Engine versions:
- Rogue's knife flies as motes (no `iron_dagger` model) and no longer bounces once.
- Fire Mage volley bolts burst once on first contact instead of piercing/bouncing 99 times.
- Gemini meteor launch divergence is a landing scatter (comet 1.3 blocks, starfall 5.8).
- Templar's slash flourish uses vanilla `ENCHANTED_HIT` until phase 6 replaces it.
- The core spell JSONs (`beam`, `comet`, `fireball`, …) are no longer read by anything; they stay for now.

## Slash trails (phase 6)

Swept arcs (horizontal / vertical / diagonal) with a ribbon are drawn by AtlasVFX: the client builds a
`Trail` on `client/combat/AttackSurface`, a `SweptSurface` whose points come from
`AttackGeometry.surfacePoint` — the server's damage call — so the trail covers exactly what can be hit,
speed curve, asymmetric arcs and inner radius included. Colour from the ribbon profile
(`TrailStyles.slash`), length and fade from its `trail` / `fadeOutTicks`; the ribbon's overreach bloom
is not carried over. Their server particle spray (`SlashVisual`) is skipped. Thrust lanes, charge paths
and one-tick cones keep the custom ribbon/particles until AtlasVFX gains thrust/point trails.
The Templar's out-of-action flourish is an `FxCues.SLASH` cue (an AtlasVFX slash trail).

## Magus and spell JSONs (phase 7)

Forsaken Magus stays Spell Engine-only (compat) but casts `compat/spellengine/magus/MagusAbilities`, built
from its old spell JSONs; its barrier still reads Spell Power's damage types. The barrier "empower" scales
power squared, as Spell Engine's impact context did. Every boss spell JSON is gone; only the item spells
remain (`abberrath_nova`, `ice_storm`, `despot`, `foxshade`, `kintsugi`, `savant`,
`summon_deatomization_storm`). `EntityRelationsMixin` is removed (its rules live in
`MinibossEntity.canHarm`); `SpellProjectileMixin` no longer fires for any boss.

Petrification works now: the petrified effect was never in Spell Engine's synced list, so the checks were
dead. Bosses mirror it into a tracked `PETRIFIED` flag; petrified bosses take no damage and can be released.

Deliberate differences: Magus's dark-matter orb now grows when its bolts hit players (the old hook compared
against `rpg_minibosses:` and never matched); its shockwaves telegraph their 25-tick fuse; bounce /
ricochet / chain-reaction / falloff perks are dropped; projectiles stop homing at tick 12 as
`SpellProjectileMixin` made them (also restored for the Fire Mage volley).

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
