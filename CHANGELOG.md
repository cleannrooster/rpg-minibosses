# Changelog

## 3.0.0-beta.1

> [!WARNING]
> **3.0 does not load 2.x worlds.** The mod is now `anarchy_rpg` (formerly `rpg-minibosses`), and nothing is
> converted. In a world made with 2.x, every adventurer, item, block, custom stat and the guild hall dimension from
> this mod is lost. Start a new world, or keep 2.x for that world.

3.0 turns the adventurers into people you can deal with. They belong to guilds, live in camps, talk, trade, gossip,
and can be hired. Fighting them is still an option.

### Guilds and standing
- Every world has three guilds, each with its own name and banner. How the guilds get along with each other (allied,
  neutral, rival, at war) is rolled from the seed, and they are never all at war at once.
- You have a standing with each guild. Trading and hiring raise it; killing its members lowers it. How a guild's
  adventurers treat you follows your standing: **hostile**, **wary**, **neutral**, **friendly** or **devoted**.
- Grudges cool: negative standing climbs back toward neutral by 2 points per in-game day, whether you are online or
  not (`standingRecoveryPerDay`). Good standing never decays.
- Adventurers from guilds at war fight each other, and those fights end in real deaths.
- **Forsaken Adventurers:** adventurers who don't live in a camp or tower (natural spawns, wanderers, encounter
  bosses; patrols still belong to a guild) answer to no guild. They attack on sight, wear a red and black aura so
  you can tell, and fight guild adventurers to the death. Killing them doesn't touch your standing; down one and
  you can still spare and recruit it, which ends its forsaking. `forsakenOutsideCamps` (on by default) turns this off.
- `guildsNeverHostile` (off by default) lets bad standing go no lower than wary: no one attacks on sight, though anyone
  you attack still fights back.

### Talking to adventurers
- Right-click a non-hostile adventurer to talk. The conversation offers whatever that adventurer can do for you.
- **Exchange:** barter for its stock. The stock includes its drop pool, a little dungeon loot, and a rare boss key.
  The prices follow your standing.
  - Armor and tools are valued on their repair material (a diamond chestplate is worth 8 diamonds; a hoe less than
    a sword; netherite armor is its diamond version plus the ingot; a netherite tool adds the ingot in proportion to its use,
    so a hoe a quarter of it). Modded gear made of an unpriced material keeps
    its attribute-based value.
  - Enchantments are worth their share of their highest level; treasure ones such as Mending count double, and
    curses lower the value.
- **Rumors:** it tells of a nearby structure, boss or player, as a direction and a rough distance. How far to trust it
  depends on your standing. When someone asks about you, you hear about it as gossip.
  - Structures from many popular structure mods count: YUNG's, Repurposed Structures, When Dungeons Arise,
    Explorify, Moog's, CTOV, Towns & Towers, Integrated Villages, Dungeons & Taverns, IDAS and Structory.
- Banners float above adventurers and their camps when you're close.

### Camps and towers
- Adventurer camps and repurposed outpost towers are now living hubs. Residents come and go over time, so the faces
  and stock stay fresh. Camps no longer contain trial spawners, and they generate about three times as often as
  before.
- Every player starts with an explorer map to the nearest camp (`startWithCampMap`). It is given a few seconds after
  your first join, so starter-kit mods go first.
- **Camp services:** residents sell a paid, lesser version of their speciality, once a day (Gamble three times):

  | Adventurer | Service |
  |---|---|
  | Templar | **Lift a curse:** one random curse comes off the item you hold |
  | Fire Mage | **Enchant an item:** the held item is enchanted at random, as an enchanting table at level 30 would, for a hefty price plus 3 lapis |
  | Mercenary | **Gamble:** a set price, an unseen item from dungeon loot |
  | Rogue | **Buy directions:** exact coordinates of a place, in chat and on a paper note |
  | Juggernaut | **Guard the camp:** keeps watch over its camp for a day |

### Guild Journal
- Press **J** (rebindable) to open the Guild Journal. It has three tabs:
  - **Rumors & Gossip:** the last 30 rumors and gossip you were told, newest first, with who told you and when.
  - **Companion:** who your companion is, what it is doing and where it is, with a **Release** button that frees your
    companion slot (the same as `/companion release`).
  - **Guilds:** your standing with each guild, and how the guilds regard each other.

### Companions
- Spare a defeated adventurer, or **hire** one, to make it your companion. You can have one at a time.
- Companions don't die. When knocked down they get back up after a while, or straight away if you heal them.
  If you die while yours is down, it gets up at full health and joins you where you respawn.
- **Orders:** Follow, Wait (1–24 hours), Wait at rendezvous point, Set rendezvous point here, and Dismiss. Companions
  carry your gear in **Storage** and wear what you give them in **Equipment**.
- Companions follow you through teleports and between dimensions: waystones, `/tp`, portals and respawning.
- When you set off by boat, a following companion gets into its own (a nearby empty one, or one it conjures) and
  rows after you, holding station when close. It comes ashore near you once you land.
- Companions of different players never target or hurt each other. Players on the same vanilla team can't hurt each
  other or each other's companions through companions, whatever the team's friendlyFire setting.
- Lost or stuck companions find their way home. `/companion recall` sends yours home, and `/companion release`
  frees the slot. `/dismisscompanion` frees the slot with no refund.
- Each type of companion has a **speciality**:

  | Companion | Speciality |
  |---|---|
  | Juggernaut | **Guard my home:** patrols around your spawn point and fights what comes near |
  | Templar | **Lift the curses:** every curse comes off the item you hold, once a day |
  | Mercenary | Keeps trading with you instead of carrying storage, and refreshes its stock daily |
  | Fire Mage | **Disenchant:** the held item burns, and every enchantment comes back on its own book, once a day (asks twice) |
  | Rogue | **Lead me somewhere:** pick one of three places, then follow it there at a run. It vaults ledges and rows boats across water, and heads home if kept waiting |

### Changes and removals
- There is now one version of each adventurer, at the former lesser strength.
- The **guild hall**, its dimension and its commands are deprecated for 3.0, and should return in 3.1.
- The old villager-style trading is gone; Exchange replaces it.
- **Wanted posters** and the wanted board no longer generate. Placed poster blocks still exist.
- The Infamy and Benevolence stats are gone; guild standing replaces them.

### Fixes
- Attacks interrupted mid-swing no longer leave their animation playing, so there is no more endless spin.
- Health-based phase changes such as the Juggernaut's Last Stand happen once, instead of every tick (which replayed
  the roar and reset its attack cooldowns constantly).
- NPCs sit properly when riding, and their heads no longer turn backwards.

### Notes for server owners
- Everything new is configurable in the server config: prices, timers, spawn rates, rumor search ranges and the
  options above.
- Operator commands for testing: `/rpgminibosses guilds`, `/rpgminibosses companion`, `/rpgminibosses rumors`
  and `/rpgminibosses camp`.
- Spell Engine and Spell Power remain optional.
