package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.AI.ArtilleristCrossbowAttackGoal;

import com.cleannrooster.rpg_minibosses.entity.brain.impl.FireMageBrain;
import com.cleannrooster.rpg_minibosses.entity.brain.impl.RogueBrain;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.tag.FabricTagKey;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.FollowMobGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.WanderNearTargetGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineSounds;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ArchmageFireEntity extends MinibossEntity  {
    public List<Item> bonusList = new ArrayList<>();

    protected ArchmageFireEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        super.bonusList = Registries.ITEM.stream().filter(item -> {return
                (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                        ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                        && new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/damage_staff")));}).toList();
    }
    protected ArchmageFireEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser) {
        super(entityType, world);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                                ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/damage_staff")));
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/damage_staff")));}).toList();

        }

    }

    protected ArchmageFireEntity(EntityType<? extends PathAwareEntity> entityType, World world,boolean lesser, float spawnCoeff) {
        super(entityType, world,spawnCoeff);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                                ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/damage_staff")));
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/damage_staff")));}).toList();

        }

    }
    public boolean skipOffHand(){
        return true;
    }
/*
    public static final RawAnimation THROW1 = RawAnimation.begin().then("animation.mob.throw1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation THROW2 = RawAnimation.begin().then("animation.mob.throw2", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation WAVE_LEFTHAND = RawAnimation.begin().then("animation.mob.wave_lefthand", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation WALK_WAVE_LEFTHAND = RawAnimation.begin().then("animation.unknown.walk_wave_lefthand", Animation.LoopType.PLAY_ONCE);
*/

    public static Stream<Item> itemList;
    @Override
    public MoveControl getMoveControl() {
        return super.getMoveControl();
    }

    @Override
    protected void initCustomGoals() {
        this.brain = new FireMageBrain(this);

        this.goalSelector.add(2, new com.cleannrooster.rpg_minibosses.entity.brain.MobBrainGoal(this, brain));
        super.initCustomGoals();
    }
    @Override
    public boolean isTwoHand() {
        return false;
    }
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }
    public Item getDefaultItem(){
        return Items.AIR;
    }
    public ItemStack getMainWeapon(){
        ItemStack stack = new ItemStack(getDefaultItem());

        return stack;

    }

    @Override
    public void tick() {

            super.tick();
    }

    @Override
    public boolean isAttacking() {
        return super.isAttacking();
    }

    @Override
    protected void mobTick() {
        super.mobTick();
    }


/*
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        super.registerControllers(animationData);
        animationData.add(
                new AnimationController<>(this, "throw1", event -> PlayState.CONTINUE)
                        .triggerableAnim("throw1", THROW1));
        animationData.add(
                new AnimationController<>(this, "throw2", event -> PlayState.CONTINUE)
                        .triggerableAnim("throw2", THROW2));    animationData.add(
                new AnimationController<>(this, "wave", event -> PlayState.CONTINUE)
                        .triggerableAnim("wave", WAVE_LEFTHAND));    animationData.add(
                new AnimationController<>(this, "walk_wave", event -> PlayState.CONTINUE)
                        .triggerableAnim("walk_wave", WALK_WAVE_LEFTHAND));

    }

*/


}
