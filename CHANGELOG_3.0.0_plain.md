# Anarchy RPG 3.0.0

## Before updating
- The mod is renamed from RPG Minibosses (`rpg-minibosses`) to Anarchy RPG (`anarchy_rpg`).
- 3.0 does not load 2.x worlds. Adventurers, items, blocks, stats and the guild hall dimension from 2.x are lost. Start a new world, or keep 2.x for existing worlds.

## Adventurers
- There are five adventurers: the Juggernaut, the Templar, the Fire Mage, the Rogue and the Mercenary.
- There is now one version of each adventurer, at the former lesser strength.
- The Juggernaut is slow and heavy. It attacks with a slam, a spin and a leap.
- The Templar advances constantly. It uses sweeping melee attacks, a dash, a parry, and Divine Fall.
- The Fire Mage keeps its distance. It casts Fireball, Fire Volley and Fire Nova.
- The Rogue circles and attacks from angles. It throws knives, rolls, dodges, and can turn invisible.
- The Mercenary fights at range from firing positions. It uses a heavy shot, a four-shot burst, and traps.
- The Gemini twins and the Magus are unchanged. The Magus requires Spell Engine.

## Guilds and standing
- Every world has three guilds. Each guild has a name and a banner.
- Guilds are allied, neutral, rivals, or at war with each other. This is decided by the world seed. They are never all at war.
- Adventurers from guilds at war fight each other. Those fights end in deaths.
- You have a standing with each guild. Trading and hiring raise it. Killing members lowers it.
- Adventurers treat you as hostile, wary, neutral, friendly or devoted, based on your standing.
- Hostile adventurers attack on sight and do not talk.
- Negative standing recovers toward neutral by 2 points per in-game day, online or offline (`standingRecoveryPerDay`). Positive standing does not decay.
- `guildsNeverHostile` (off by default) keeps standing from going below wary.

## Forsaken Adventurers
- Adventurers who do not live in a camp or tower are Forsaken. This includes natural spawns, wanderers and encounter bosses. Patrols still belong to a guild.
- Forsaken have no guild. They attack on sight.
- Forsaken have a red and black aura.
- Forsaken and guild adventurers fight each other. Those fights end in deaths.
- Killing a Forsaken does not change your standing.
- A downed Forsaken can be spared and recruited. It stops being Forsaken.
- `forsakenOutsideCamps` (on by default) turns this off.

## Talking
- Right-click an adventurer to talk to it. The options depend on the adventurer.
- Banners show above adventurers and camps when you are close.

## Exchange
- Exchange replaces the old villager-style trading.
- You barter for an adventurer's stock. Any item with a value can be offered.
- Stock includes the adventurer's drop pool, some dungeon loot, and sometimes a boss key.
- Prices depend on your standing. Asking for more stacks raises the price.
- Offered items are used up. Stock does not refill.
- Armor and tools are valued by their repair material. A diamond chestplate is worth 8 diamonds. A hoe is worth less than a sword. Netherite armor is worth its diamond version plus a netherite ingot. Netherite tools add part of an ingot by usefulness: a sword adds a whole ingot, a hoe a quarter.
- Enchantments are valued by how close they are to their highest level. Treasure enchantments such as Mending are worth double. Curses lower an item's value.
- The currency is emeralds by default (`currencyItem`).

## Rumors
- Adventurers tell rumors about a nearby structure, boss or player, as a direction and a rough distance.
- Rumors can be wrong. Higher standing makes them more accurate.
- Each adventurer has a limited number of rumors. They come back over time.
- Structures from these mods are included: YUNG's, Repurposed Structures, When Dungeons Arise, Explorify, Moog's, CTOV, Towns & Towers, Integrated Villages, Dungeons & Taverns, IDAS and Structory.
- If someone asks an adventurer about you, you hear about it later as gossip.

## Camps and towers
- Adventurer camps and repurposed outpost towers are hubs.
- Each camp belongs to one guild and flies its banner.
- Residents leave and are replaced over time. Replacements bring new stock.
- Killed or recruited residents are not replaced until the next rotation.
- Camps no longer contain trial spawners.
- Camps generate about three times as often.
- Every player starts with a map to the nearest camp (`startWithCampMap`).

## Camp services
- Residents sell a paid service once a day. Gamble can be bought three times a day.
- Templar: removes one random curse from the item you hold.
- Fire Mage: enchants the item you hold at random, at level 30, for a fee and three lapis.
- Mercenary: sells an unseen item from dungeon loot for a set price.
- Rogue: sells the exact coordinates of a place, in chat and on a paper note.
- Juggernaut: guards its camp for a day.

## Companions
- Spare a defeated adventurer, or hire one in a camp, to make it your companion.
- You can have one companion at a time.
- Companions do not die. When knocked down, they get back up after a while, or right away if healed.
- Orders: Follow, Wait (1 to 24 hours), Wait at rendezvous point, Set rendezvous point here, and Dismiss.
- Companions carry items in Storage and wear what you give them in Equipment.
- Companions follow you through portals, teleports, waystones and respawning.
- When you set off by boat, a following companion takes its own boat and rows after you. It comes ashore when you do.
- Lost companions return home.
- If you die while your companion is downed, it gets up at full health where you respawn.
- Companions of different players never target or hurt each other.
- If two players are on the same vanilla team, their companions never hurt each other or the other player, and neither player can hurt the other's companion. This ignores the team's friendlyFire setting.
- `/companion recall` sends your companion home.
- `/companion release` frees your companion slot.
- `/dismisscompanion` frees your companion slot with no refund.

## Companion specialties
- Juggernaut: patrols around your spawn point.
- Templar: removes every curse from the item you hold, once a day.
- Mercenary: keeps trading with you instead of carrying storage. Its stock refreshes daily.
- Fire Mage: destroys the item you hold and returns each of its enchantments on a separate book, once a day.
- Rogue: leads you to one of three places. It vaults ledges and rows boats.

## Guild Journal
- Press J to open the Guild Journal. The key can be changed in Controls.
- Rumors & Gossip tab: the last 30 rumors and gossip you heard, with who told you and when.
- Companion tab: your companion, what it is doing, and where it is. The Release button frees your companion slot.
- Guilds tab: your standing with each guild, and how the guilds regard each other.

## Removed
- The guild hall, its dimension and its commands. These should return in 3.1.
- Villager-style trading.
- Wanted posters and the wanted board no longer generate. Placed poster blocks still exist.
- The Infamy and Benevolence stats. Guild standing replaces them.
- Lesser and greater versions of adventurers.

## Fixed
- Attacks interrupted mid-swing no longer leave their animation playing.
- Health-based phase changes, such as the Juggernaut's Last Stand, now happen once. Before, they repeated every tick, replaying the roar and resetting attack cooldowns.
- Adventurers sit properly when riding. Their heads no longer turn backwards.
- Fixed a NeoForge startup crash caused by a Fabric-only call.
- Fixed a NeoForge startup crash in the bundled AtlasVFX library.

## For server owners
- New features are configurable in the server config: prices, timers, spawn rates, rumor ranges and toggles.
- Operator commands: `/rpgminibosses guilds`, `/rpgminibosses companion`, `/rpgminibosses rumors`, `/rpgminibosses camp` and `/rpgminibosses hubs`.
- Spell Engine and Spell Power are optional.
