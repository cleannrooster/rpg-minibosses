package com.cleannrooster.rpg_minibosses.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.LinkedHashMap;

@Config(name = "server_v4")
public class ServerConfig  implements ConfigData {
    public ServerConfig(){}
    @Comment("Chance for Forsaken Templars to spawn naturally")
    public  float templarGreater = 1.0F;
    @Comment("Forsaken Templar maxHealth ")
    public  float templarMaxHealth = 100F;

    @Comment("Forsaken Templar Armor ")
    public  float templarGreaterArmor = 16F;
    @Comment("Forsaken Templar Attack Damage ")
    public  float templarGreaterAttackDamage = 8F;
    @Comment("Forsaken Templar Movement Speed ")
    public  float templarGreaterMovementSpeed = 1.4F;
    @Comment("Forsaken Templar Healing Power ")
    public  float templarGreaterHealingPower = 6F;


    @Comment("Chance for Forsaken Fire Mages to spawn naturally")
    public  float fireMageGreater = 1.0F;
    @Comment("Forsaken Fire Mages maxHealth ")
    public  float fireMageGreaterMaxHealth = 100F;
    @Comment("Forsaken Fire Mages Armor ")
    public  float fireMageGreaterArmor = 4F;
    @Comment("Forsaken Fire Mages Movement Speed ")
    public  float fireMageMovementSpeed = 0.9F;
    @Comment("Forsaken Fire Mages Fire Power ")
    public  float fireMageFirePower = 6.0F;

    @Comment("Chance for Forsaken Juggernauts to spawn naturally")
    public  float juggernautGreater = 1.0F;
    @Comment("Forsaken Juggernauts maxHealth ")
    public  float juggernautGreaterMaxHealth = 100F;
    @Comment("Forsaken Juggernauts Armor ")
    public  float juggernautGreaterArmor = 20F;
    @Comment("Forsaken Juggernauts Defiance ")
    public  float juggernautGreaterDefiance = 4F;
    @Comment("Forsaken Juggernauts Attack Damage ")
    public  float juggernautGreaterAttackDamage = 1.0F;
    @Comment("Forsaken Juggernauts Movement Speed Modifier")
    public  float juggernautGreaterMovementSpeed = 1F;
    @Comment("Forsaken Juggernauts Knockback Resistance ")
    public  float juggernautGreaterKnockbackResistance = 0.75F;

    @Comment("Chance for Forsaken Rogues to spawn naturally")
    public  float rogueGreater = 1.0F;
    @Comment("Forsaken Rogues Max Health ")
    public  float rogueGreaterMaxHealth = 100F;
    @Comment("Forsaken Rogues Armor ")
    public  float rogueGreaterArmor = 8F;
    @Comment("Forsaken Rogues Attack Damage ")
    public  float rogueGreaterAttackDamage = 1.0F;
    @Comment("Forsaken Rogues Movement Speed ")
    public  float rogueGreaterMovementSpeed = 1.8F;
    @Comment("Forsaken Rogues Suppress ")
    public  float rogueGreaterSuppress = 60F;
    @Comment("Forsaken Rogues Evasion ")
    public  float rogueGreaterEvasion = 60F;
    @Comment("Chance for Forsaken Mercenaries to spawn naturally")
    public  float mercenaryGreater = 1.0F;
    @Comment("Forsaken Mercenaries Max Health ")
    public  float mercenaryGreaterMaxHealth = 100F;
    @Comment("Forsaken Mercenaries Armor ")
    public  float mercenaryGreaterArmor = 14F;
    @Comment("Forsaken Mercenaries Ranged Damage ")
    public  float mercenaryGreaterRangedDamage = 0F;
    @Comment("Forsaken Mercenaries Movement Speed Modifier")
    public  float mercenaryGreaterMovementSpeed = 1F;
    @Comment("Forsaken Mercenary Defiance ")
    public  float mercenaryGreaterDefiance = 2.0F;

    @Comment("Chance for Lesser Templars to spawn naturally")
    public  float templarLesser = 1.0F;
    @Comment("Lesser Templar maxHealth ")
    public  float templarLesserMaxHealth = 50F;
    @Comment("Lesser Templar Armor ")
    public  float templarLesserArmor = 12F;
    @Comment("Lesser Templar Attack Damage ")
    public  float templarLesserAttackDamage = 8F;
    @Comment("Lesser Templar Movement Speed ")
    public  float templarLesserMovementSpeed = 1.4F;
    @Comment("Lesser Templar Healing Power ")
    public  float templarLesserHealingPower = 4F;




    @Comment("Chance for Lesser Fire Mages to spawn naturally")
    public  float fireMageLesser = 1.0F;
    @Comment("Lesser Fire Mages maxHealth ")
    public  float fireMageLesserMaxHealth = 50F;
    @Comment("Lesser Fire Mages Armor ")
    public  float fireMageLesserArmor = 4F;
    @Comment("Lesser Fire Mages Movement Speed ")
    public  float fireMageLesserMovementSpeed = 0.9F;
    @Comment("Lesser Fire Mages Fire Power ")
    public  float fireMageLesserFirePower = 4F;

    @Comment("Chance for Lesser Juggernauts to spawn naturally")
    public  float juggernautLesser = 1.0F;
    @Comment("Lesser Juggernauts maxHealth ")
    public  float juggernautLesserMaxHealth = 50F;
    @Comment("Lesser Juggernauts Armor ")
    public  float juggernautLesserArmor = 16F;
    @Comment("Lesser Juggernauts Defiance ")
    public  float juggernautLesserDefiance = 2F;
    @Comment("Lesser Juggernauts Attack Damage ")
    public  float juggernautLesserAttackDamage = 1.0F;
    @Comment("Lesser Juggernauts Movement Speed Modifier")
    public  float juggernautLesserMovementSpeed = 1F;
    @Comment("Lesser Juggernauts Knockback Resistance ")
    public  float juggernautLesserKnockbackResistance = 0.75F;

    @Comment("Chance for Forsaken Rogues to spawn naturally")
    public  float rogueLesser = 1.0F;
    @Comment("Lesser Rogues Max Health ")
    public  float rogueLesserMaxHealth = 50;
    @Comment("Lesser Rogues Armor ")
    public  float rogueLesserArmor = 8F;
    @Comment("Lesser Rogues Attack Damage ")
    public  float rogueLesserAttackDamage = 1.0F;
    @Comment("Lesser Rogues Movement Speed ")
    public  float rogueLesserMovementSpeed = 1.8F;
    @Comment("Lesser Rogues Suppress ")
    public  float rogueLesserSuppress = 40F;
    @Comment("Lesser Rogues Evasion ")
    public  float rogueLesserEvasion = 40F;

    @Comment("Chance for Lesser Mercenaries to spawn naturally")
    public  float mercenaryLesser = 1.0F;
    @Comment("Lesser Mercenary Max Health ")
    public  float mercenaryLesserMaxHealth = 50F;
    @Comment("Lesser Mercenaries Armor ")
    public  float mercenaryLesserArmor = 10;
    @Comment("Lesser Mercenaries Ranged Damage ")
    public  float mercenaryLesserRangedDamage = 0F;
    @Comment("Lesser Mercenaries Movement Speed Modifier")
    public  float mercenaryLesserMovementSpeed = 1F;
    @Comment("Lesser Mercenary Defiance ")
    public  float mercenaryLesserDefiance = 1F;

    @Comment("Lesser Scale ")
    public  float lesserScale = 1.0F;
    @Comment("Greater Scale ")
    public  float greaterScale = 1.2F;



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


    @Comment("Distance from existing Minibosses within which new ones cannot spawn (Deprecated)")
    public  float areaCannotSpawn = 30;
    @Comment("Chance for Lesser Minibosses to be Petrified upon Spawn")
    public  float lesserPetrify = 0F;

    @Comment("Chance for Greater Minibosses to be Petrified upon Spawn")
    public  float greaterPetrify = 1F;
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

}
