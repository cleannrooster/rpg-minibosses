package com.cleannrooster.anarchy_rpg.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.LinkedHashMap;

@Config(name = "server_v8")
public class ServerConfig  implements ConfigData {



    public ServerConfig(){}
    @Comment("Guild Hall: deprecated in 3.0 (its dimension, commands and hooks are disabled); returns in 3.1")
    public boolean guild = false;
    @Comment("Currency item: worth exactly 1 in Exchange and hiring")
    public String currencyItem = "minecraft:emerald";

    @Comment("Minibosses can spawn in the Nether")
    public boolean hell = true;

    @Comment("Spawn the miniboss inside its worldgen encounter structure. When false, the structures still generate but spawn empty.")
    public boolean structureEncounterSpawns = true;

    @Comment("Chance for Templars to spawn naturally")
    public  float templarSpawnChance = 1.0F;
    @Comment("Templar maxHealth ")
    public  float templarMaxHealth = 25F;
    @Comment("Templar Armor ")
    public  float templarArmor = 12F;
    @Comment("Templar Attack Damage ")
    public  float templarAttackDamage = 0F;
    @Comment("Templar Movement Speed ")
    public  float templarMovementSpeed = 1.4F;
    @Comment("Templar Healing Power ")
    public  float templarHealingPower = 4F;




    @Comment("Chance for Fire Mages to spawn naturally")
    public  float fireMageSpawnChance = 1.0F;
    @Comment("Fire Mages maxHealth ")
    public  float fireMageMaxHealth = 25F;
    @Comment("Fire Mages Armor ")
    public  float fireMageArmor = 4F;
    @Comment("Fire Mages Movement Speed ")
    public  float fireMageMovementSpeed = 1.2F;
    @Comment("Fire Mages Fire Power ")
    public  float fireMageFirePower = 4F;

    @Comment("Chance for Juggernauts to spawn naturally")
    public  float juggernautSpawnChance = 1.0F;
    @Comment("Juggernauts maxHealth ")
    public  float juggernautMaxHealth = 25F;
    @Comment("Juggernauts Armor ")
    public  float juggernautArmor = 16F;
    @Comment("Juggernauts Defiance ")
    public  float juggernautDefiance = 2F;
    @Comment("Juggernauts Attack Damage ")
    public  float juggernautAttackDamage = 1.0F;
    @Comment("Juggernauts Movement Speed Modifier")
    public  float juggernautMovementSpeed = 1F;
    @Comment("Juggernauts Knockback Resistance ")
    public  float juggernautKnockbackResistance = 0.75F;

    @Comment("Chance for Rogues to spawn naturally")
    public  float rogueSpawnChance = 1.0F;
    @Comment("Rogues Max Health ")
    public  float rogueMaxHealth = 25;
    @Comment("Rogues Armor ")
    public  float rogueArmor = 8F;
    @Comment("Rogues Attack Damage ")
    public  float rogueAttackDamage = 1.0F;
    @Comment("Rogues Movement Speed ")
    public  float rogueMovementSpeed = 1.4F;
    @Comment("Rogues Suppress ")
    public  float rogueSuppress = 40F;
    @Comment("Rogues Evasion ")
    public  float rogueEvasion = 40F;

    @Comment("Chance for Mercenaries to spawn naturally")
    public  float mercenarySpawnChance = 1.0F;
    @Comment("Mercenary Max Health ")
    public  float mercenaryMaxHealth = 25F;
    @Comment("Mercenaries Armor ")
    public  float mercenaryArmor = 10;
    @Comment("Mercenaries Ranged Damage: added to each crossbow bolt they fire")
    public  float mercenaryRangedDamage = 0F;
    @Comment("Mercenaries Movement Speed Modifier")
    public  float mercenaryMovementSpeed = 1.4F;
    @Comment("Mercenary Defiance ")
    public  float mercenaryDefiance = 1F;

    @Comment("Adventurer Scale ")
    public  float scale = 1.0F;



    @Comment("Forsaken Magus maxHealth ")
    public  float magusMaxHealth = 400F;
    @Comment("Forsaken Magus Armor ")
    public  float magusArmor = 8F;
    @Comment("Forsaken Magus Movement Speed ")
    public  float magusMovementSpeed = 1.2F;
    @Comment("Forsaken Magus KnockbackResistance ")
    public  float magusKnockbackResistance = 1F;
    @Comment("Forsaken Magus Fire Power ")
    public  float magusFirePower = 8F;
    @Comment("Forsaken Magus Frost Power ")
    public  float magusFrostPower = 8F;
    @Comment("Forsaken Magus Arcane Power ")
    public  float magusArcanePower = 8F;
    @Comment("Forsaken Magus Soul Power ")
    public  float magusSoulPower = 8F;
    @Comment("Forsaken Magus Lightning Power ")
    public  float magusLightningPower = 8F;
    @Comment("Alpha Lightning Power ")

    public float alphaLightningPower = 8F;
    @Comment("Alpha Attack Damage ")

    public float alphaAttackDamage = 8F;
    @Comment("Alpha Arcane Power ")

    public float alphaArcanePower = 8F;

    @Comment("Alpha Fire Power ")

    public float alphaFirePower = 8F;
    @Comment("Alpha Health Power ")

    public float alphaHealth = 800;
    @Comment("Alpha Armor ")


    public float alphaArmor = 12;
    @Comment("Beta Frost Power ")


    public float betaFrostPower = 8F;
    @Comment("Beta Attack Damage ")

    public float betaAttackDamage = 8F;
    @Comment("Beta Lightning Power ")


    public float betaLightningPower = 8F;
    @Comment("Beta Health ")
    public float betaHealth = 800;
    @Comment("Beta Armor ")


    public float betaArmor = 12;

    @Comment("Uber Alpha Health")
    public float alphaUberHealth = 1200;
    @Comment("Uber Alpha Armor")
    public float alphaUberArmor = 16;
    @Comment("Uber Alpha Fire Power")
    public float alphaUberFirePower = 12F;
    @Comment("Uber Alpha Arcane Power")
    public float alphaUberArcanePower = 12F;
    @Comment("Uber Alpha Lightning Power")
    public float alphaUberLightningPower = 12F;
    @Comment("Uber Alpha Attack Damage")
    public float alphaUberAttackDamage = 12F;

    @Comment("Uber Beta Health")
    public float betaUberHealth = 1200;
    @Comment("Uber Beta Armor")
    public float betaUberArmor = 16;
    @Comment("Uber Beta Frost Power")
    public float betaUberFrostPower = 12F;
    @Comment("Uber Beta Lightning Power")
    public float betaUberLightningPower = 12F;
    @Comment("Uber Beta Attack Damage")
    public float betaUberAttackDamage = 12F;

    @Comment("Deatomization Storm Base Radius")
    public float stormBaseRadius = 5.0F;
    @Comment("Deatomization Storm Pulse Amplitude")
    public float stormPulseAmplitude = 0.8F;
    @Comment("Distance from existing Minibosses within which new ones cannot spawn (Deprecated)")
    public  float areaCannotSpawn = 30;
    @Comment("Chance for adventurers to be Petrified upon Spawn")
    public  float petrifyChance = 0F;
    @Comment("Minibosses target players on sight")

    public boolean trueAnarchy = false;
    @Comment("Minibosses target each other on sight (Takes precedence over Despotism)")

    public boolean betrayal = false;
    @Comment("Minibosses conduct patrols with each other (Incompatible with Betrayal)")
    public boolean despotism = true;
    @Comment("Minibosses friendly fire multiplier (No effect with Betrayal on)")
    public float friendlyFire = 0.2000F;

    @Comment("Global Spawn Multiplier (Integer)")
    public int mult = 1;
    @Comment("Time in days from the beginning of the world until the first patrols can happen")
    public long patrolGrace =  10L;

    @Comment("Patrol Cooldown (Ticks != 0)")
    public int patrolCooldown =  12000;
    @Comment("Patrol Cooldown Random Addition (Ticks != 0)")
    public int patrolAdded = 1200;
    @Comment("Debug NPC spawning requirements.")
    public boolean debug = false;
    @Comment("Make NPCs only spawn near players that have fulfilled advancement requirements")

    public boolean enableAdvancementRequirement = false;
    @Comment("List of Advancements and their required state for mobs to spawn near a player. Format: id,boolean")
    public LinkedHashMap<String, Boolean> advancements = new LinkedHashMap<String, Boolean>() {
        {
            this.put("examplemod:exampleadvancement", false);

        }};
    @Comment("All advancements and states required to be fulfilled for a mob to spawn.")
    public boolean allRequired = false;
    @Comment("Distance to enforce advancement requirement.")
    public float distance = 128;

    @Comment("Standing: lowest value a player's standing with a guild can fall to")
    public int standingMin = -100;
    @Comment("Standing: highest value a player's standing with a guild can rise to")
    public int standingMax = 100;
    @Comment("Standing: below this, a guild's adventurers attack on sight and refuse to talk")
    public int standingHostileBelow = -20;
    @Comment("Standing: below this (and not hostile), a guild's adventurers are wary")
    public int standingWaryBelow = -5;
    @Comment("Standing: above this, a guild's adventurers are friendly")
    public int standingFriendlyAbove = 20;
    @Comment("Standing: above this, a guild's adventurers are devoted")
    public int standingDevotedAbove = 50;
    @Comment("Standing change for killing a guild's adventurer")
    public int standingKillMember = -20;
    @Comment("Standing change for provoking a guild's adventurer")
    public int standingProvokeMember = -4;
    @Comment("Standing change for sparing a defeated adventurer")
    public int standingSpareMember = 8;
    @Comment("Adventurers attack adventurers of a guild their own guild is at war with (ignored when betrayal is on)")
    public boolean factionWarfare = true;

    @Comment("Exchange: each extra stack requested raises the price of everything requested by this fraction")
    public double exchangeMarkupPerStack = 0.15;
    @Comment("Exchange: price multiplier from the angriest adventurer still willing to talk (the most prices ever rise)")
    public double exchangeAngryPriceMultiplier = 2.0;
    @Comment("Exchange: price multiplier from a devoted adventurer (the most prices ever fall)")
    public double exchangeDevotedPriceMultiplier = 0.75;
    @Comment("Exchange: rolls of the adventurer's own drop pool (its loot table, including Spell Engine and RPG Series drops) into its stock")
    public int exchangeStockDropRolls = 4;
    @Comment("Exchange: chance that an adventurer's stock carries one boss key (item tag anarchy_rpg:boss_keys)")
    public double exchangeBossKeyChance = 0.08;
    @Comment("Exchange: chance that an adventurer's stock includes some generic dungeon loot")
    public double exchangeDungeonLootChance = 0.3;
    @Comment("Exchange: chest loot tables the dungeon loot is drawn from (one is picked at random)")
    public java.util.List<String> exchangeDungeonLootTables = new java.util.ArrayList<>(java.util.List.of(
            "minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple"));
    @Comment("Exchange: at most this many stacks of a dungeon loot roll make it into the stock")
    public int exchangeDungeonLootMaxStacks = 3;
    @Comment("Exchange: weapons drawn from the adventurer's weapon pool into its stock")
    public int exchangeStockWeapons = 3;
    @Comment("Exchange: rolls of the anarchy_rpg:exchange/supplies loot table into an adventurer's stock")
    public int exchangeStockSupplyRolls = 3;
    @Comment("Standing change for completing a trade")
    public int standingTrade = 1;

    @Comment("Hubs (camps and towers): how many adventurers live in each")
    public int hubResidents = 8;
    @Comment("Hubs: ticks between rotations (72000 = 3 Minecraft days)")
    public long hubRotationTicks = 72000L;
    @Comment("Hubs: how many of the longest-staying residents leave at each rotation")
    public int hubRetireCount = 4;
    @Comment("Hubs: a hub never rotates while a player is within this many blocks of it")
    public double hubWatchRadius = 48.0;
    @Comment("Hubs: chunks around each player checked for hub structures")
    public int hubScanRadiusChunks = 8;

    @Comment("Companions: ticks a companion stays down after a lethal blow before getting back up")
    public long companionDownedTicks = 1200L;
    @Comment("Companions: fraction of max health a companion gets back up with")
    public float companionReviveHealth = 0.5F;
    @Comment("Companions: within this many blocks of base (same dimension) a companion walks home; farther, the trip is simulated")
    public double companionWalkRange = 128.0;
    @Comment("Companions: simulated travel speed, in blocks per tick (0.2 = 4 blocks a second)")
    public double companionTravelBlocksPerTick = 0.2;
    @Comment("Companions: a simulated trip between dimensions never takes longer than this many ticks (48000 = 2 days)")
    public long companionCrossDimensionMaxTicks = 48000L;
    @Comment("Companions: price (in currency items) to hire an adventurer at neutral standing; standing raises or lowers it like Exchange prices")
    public double companionHirePrice = 32.0;
    @Comment("Standing change for hiring a guild's adventurer")
    public int standingHire = 2;
    @Comment("Companions: a companion that has been away from its online owner this long (unloaded, another dimension, or too far) makes its way home")
    public long companionLostTimeoutTicks = 2400L;
    @Comment("Companions: farther than this from its online owner (same dimension) counts as away")
    public double companionLostDistance = 256.0;
    @Comment("Companions: health regained per second while out of combat near the owner's spawn point")
    public float companionRestHealPerSecond = 1.0F;
    @Comment("Companions: how close to the owner's spawn point (same dimension) a companion must be to heal at rest")
    public double companionRestRadius = 64.0;
    @Comment("Companions: a rendezvous point must be within this many blocks of the owner's spawn point")
    public double companionRendezvousRadius = 64.0;

}
