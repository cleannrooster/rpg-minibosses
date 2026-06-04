package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.AI.JuggernautLeapSlamGoal;

import com.cleannrooster.rpg_minibosses.entity.brain.impl.JuggernautBrain;
import com.cleannrooster.rpg_minibosses.entity.brain.impl.RogueBrain;
import com.cleannrooster.rpg_minibosses.entity.brain.impl.TemplarBrain;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.feature.CreeperChargeFeatureRenderer;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.List;
import java.util.Optional;

public class JuggernautEntity extends MinibossEntity{
    List<Item> bonusList;


    protected JuggernautEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        super.bonusList = Registries.ITEM.stream().filter(item -> {return
                (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                        ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                        && ( new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/hammer"))))
                ;}).toList();

    }
    protected JuggernautEntity(EntityType<? extends PathAwareEntity> entityType, World world,boolean lesser) {
        super(entityType, world);

        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/double_axe"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && ( new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/hammer"))))
                    ;}).toList();
        }

    }
    protected JuggernautEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser, float SpawnCoeff) {
        super(entityType, world,SpawnCoeff);

        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/double_axe"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && ( new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/hammer"))))
                    ;}).toList();
        }

    }
/*
    public static final RawAnimation LEAPSLAM = RawAnimation.begin().thenPlay("animation.mob.jugg.leapslam");
    public static final RawAnimation TWOHANDWAVE = RawAnimation.begin().then("animation.mob.wizard.staffwave2", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation TWOHANDSPIN = RawAnimation.begin().thenPlayXTimes("animation.mob.spin_2h", 4);
    public static final RawAnimation WINDDOWN = RawAnimation.begin().thenPlay("animation.mob.spinwinddown");

    public static final RawAnimation SLAM = RawAnimation.begin().thenPlayXTimes("animation.mob.heavy.slam", 1);
    public static final RawAnimation SWING1 = RawAnimation.begin().then("animation.mob.swing1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation SWING2 = RawAnimation.begin().then("animation.mob.swing2", Animation.LoopType.PLAY_ONCE);
*/

    public boolean skipOffHand(){
        return true;
    }
    @Override
    protected void initCustomGoals() {
        this.brain = new JuggernautBrain(this);

        this.goalSelector.add(2, new com.cleannrooster.rpg_minibosses.entity.brain.MobBrainGoal(this, brain));
        super.initCustomGoals();
    }
    @Override
    public boolean isTwoHand() {
        return true;
    }
    public Item getDefaultItem(){
        return Items.IRON_AXE;
    }
    public ItemStack getMainWeapon(){
        ItemStack stack = new ItemStack(getDefaultItem());

        return stack;

    }

    public int defensetime = 80;


            public void applyIntroEffect(){
        super.applyIntroEffect();
    }
    public RegistryEntry<StatusEffect> getIntroEffect(){
        return Effects.PETRIFIED.registryEntry;
    }
    @Override
    protected void mobTick() {
        super.mobTick();
    }


    /** Kept for backward-compatibility with JuggernautLeapSlamGoal (unused by brain). */
    public int leapTimer = 0;
    public boolean   swingBool;

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (brain instanceof JuggernautBrain jb && jb.getCurrentCombatState() == JuggernautBrain.CombatState.BRACING) {
            amount *= 0.5f;
        }
        return super.damage(source, amount);
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        if (brain instanceof JuggernautBrain jb && jb.getCurrentCombatState() == JuggernautBrain.CombatState.BRACING) {
            super.takeKnockback(strength * 0.1, x, z);
        } else {
            super.takeKnockback(strength, x, z);
        }
    }

    public boolean tryAttack(Entity target) {
        if(!performing) {
            if (swingBool) {
                dispatcher.setSwing();
                swingBool = false;

            } else {
                //(this).triggerAnim("swing2", "swing2");
                dispatcher.setSwing2();

                swingBool = true;

            }
        }
        return super.tryAttack(target);

    }
/*    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        super.registerControllers(animationData);
        animationData.add(
                new AnimationController<>(this, "leapslam", event -> PlayState.CONTINUE)
                        .triggerableAnim("leapslam", LEAPSLAM));
        animationData.add(
                new AnimationController<>(this, "twohandwave", event -> PlayState.CONTINUE)
                        .triggerableAnim("twohandwave", TWOHANDWAVE));
        animationData.add(
                new AnimationController<>(this, "twohandspin", event -> PlayState.CONTINUE)
                        .triggerableAnim("twohandspin", TWOHANDSPIN));
        animationData.add(
                new AnimationController<>(this, "winddown", event -> PlayState.CONTINUE)
                        .triggerableAnim("winddown", WINDDOWN));
        animationData.add(
                new AnimationController<>(this, "slam", event -> PlayState.CONTINUE)
                        .triggerableAnim("slam", SLAM));
        animationData.add(
                new AnimationController<>(this, "swing1", event -> PlayState.CONTINUE)
                        .triggerableAnim("swing1", SWING1));
        animationData.add(
                new AnimationController<>(this, "swing2", event -> PlayState.CONTINUE)
                        .triggerableAnim("swing2", SWING2));
    }*/
}
