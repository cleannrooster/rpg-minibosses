package com.cleannrooster.anarchy_rpg.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.LinkedHashMap;

@Config(name = "server_v8")
public class ServerConfig  implements ConfigData {



    public ServerConfig(){}
    @Comment("Enable Guild Hall")
    public boolean guild = false;
    @Comment("Trade Multiplier")

    public float tradeMultiplier = 1;
    @Comment("Trade Amount")

    public int tradeAmount = 5;
    @Comment("Rare Trade Multiplier")

    public int rareTradeMultiplier = 4;
    @Comment("Trade Item")
    public String tradeItem = "minecraft:emerald";

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

    @Comment("Standing: lowest value a player's standing with a faction can fall to")
    public int standingMin = -100;
    @Comment("Standing: highest value a player's standing with a faction can rise to")
    public int standingMax = 100;
    @Comment("Standing: below this, a faction's adventurers attack on sight and refuse to talk")
    public int standingHostileBelow = -20;
    @Comment("Standing: below this (and not hostile), a faction's adventurers are wary")
    public int standingWaryBelow = -5;
    @Comment("Standing: above this, a faction's adventurers are friendly")
    public int standingFriendlyAbove = 20;
    @Comment("Standing: above this, a faction's adventurers are devoted")
    public int standingDevotedAbove = 50;
    @Comment("Standing change for killing a faction's adventurer")
    public int standingKillMember = -20;
    @Comment("Standing change for provoking a faction's adventurer")
    public int standingProvokeMember = -4;
    @Comment("Standing change for sparing a defeated adventurer")
    public int standingSpareMember = 8;
    @Comment("Adventurers attack adventurers of a faction their own faction is at war with (ignored when betrayal is on)")
    public boolean factionWarfare = true;

}
