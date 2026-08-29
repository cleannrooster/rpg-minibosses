package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.AI.RogueNodeMaker;
import com.cleannrooster.rpg_minibosses.entity.brain.impl.RogueBrain;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineSounds;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import net.spell_engine.internals.delivery.ProjectileLauncher;
import net.spell_engine.internals.delivery.CloudPlacer;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.List;
import java.util.Optional;

import static net.spell_engine.utils.VectorHelper.angleBetween;

public class TricksterEntity extends MinibossEntity{
    List<Item> bonusList = List.of();

    protected TricksterEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        super.bonusList = Registries.ITEM.stream().filter(item -> {return
                (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                        ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                        && ( new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/sickle")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/dagger"))))
                ;}).toList();


    }

    protected TricksterEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser) {
        super(entityType, world);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/sickle")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/dagger"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && ( new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/sickle")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/dagger"))))
                    ;}).toList();
        }

    }
    protected TricksterEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser,float spawnCoeff) {
        super(entityType, world,spawnCoeff);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/sickle")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/dagger"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && ( new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/sickle")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/dagger"))))
                    ;}).toList();
        }

    }
    public boolean skipOffHand(){
        return true;
    }
/*
    public static final RawAnimation SWING1 = RawAnimation.begin().then("animation.mob.swing1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation SWING2 = RawAnimation.begin().then("animation.mob.swing2", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation POMMELSTRIKE = RawAnimation.begin().then("animation.mob.trickster.pommelstrike", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation ROLL = RawAnimation.begin().then("animation.mob.trickster.roll", Animation.LoopType.PLAY_ONCE);
*/

    public boolean   swingBool;
    @Override
    public boolean isTwoHand() {
        return false;
    }

    @Override
    public void registerAttackPrototypes() {
        // Discarded immediately; building it is what registers this mob's attack geometry
        // with AttackRegistry, which the client needs to draw incoming swings.
        new RogueBrain(this);
    }

    @Override
    protected void initCustomGoals() {
        this.brain = new RogueBrain(this);
        this.goalSelector.add(2, new com.cleannrooster.rpg_minibosses.entity.brain.MobBrainGoal(this, brain));
        super.initCustomGoals();
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new TricksterNavigation(this);
    }

    public Item getDefaultItem(){
        return Items.IRON_SWORD;
    }

    public ItemStack getMainWeapon(){
        ItemStack stack = new ItemStack(getDefaultItem());

        return stack;


    }

    public int pommelTick = 100;
/*
    public static final RawAnimation THROW1 = RawAnimation.begin().then("animation.mob.throw1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation THROW2 = RawAnimation.begin().then("animation.mob.throw2", Animation.LoopType.PLAY_ONCE);

    public static final RawAnimation DASHRIGHT = RawAnimation.begin().thenPlay("animation.valkyrie.dashright");
    public static final RawAnimation DASHLEFT = RawAnimation.begin().thenPlay("animation.valkyrie.dashleft");
*/


    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.getWorld().isClient()) {
            pommelTick++;
        }
        if (pommelTick == 120) {
            this.playSound(SoundEvents.ENTITY_PILLAGER_AMBIENT);
        }
    }

    @Override
    public boolean isMobile() {
        return true;
    }

    /**
     * Reactive dodge.
     *
     * <p>The evade itself — direction, clearance check, clip and committed motion — lives in
     * {@link com.cleannrooster.rpg_minibosses.entity.brain.impl.RogueBrain#tryDodge()}, so it goes through
     * the same committed-motion path as every other movement the Trickster makes instead of being a bare
     * velocity write with a scheduled callback to undo it.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.getDataTracker().get(DOWN) && amount > 4 && !this.getWorld().isClient()
                && this.getTarget() != null
                && brain instanceof com.cleannrooster.rpg_minibosses.entity.brain.impl.RogueBrain rb
                && rb.tryDodge()) {
            this.playSound(SoundEvents.ENTITY_PILLAGER_AMBIENT);
            return false;
        }
        return super.damage(source, amount);
    }
    public class TricksterNavigation extends MobNavigation{

        public TricksterNavigation(MobEntity entity) {
            super(entity, entity.getWorld());
            this.nodeMaker = new RogueNodeMaker();

        }

        protected PathNodeNavigator createPathNodeNavigator(int range) {
            this.nodeMaker = new RogueNodeMaker();
            this.nodeMaker.setCanEnterOpenDoors(true);
            return new PathNodeNavigator(this.nodeMaker, range);
        }
    }
    /**
     * Contact damage.
     *
     * <p>The periodic pommel strike keeps its debuff on its own timer regardless of how the contact was
     * produced, but its clip — like the legacy swing clips — is only played when no combat action is
     * running. During a crossing slash the action already owns the presentation, and layering a second
     * one-shot over it would fight the swing it is meant to be part of.
     */
    @Override
    public boolean tryAttack(Entity target) {
        if (pommelTick > 120) {
            if (!performing) {
                dispatcher.setPommelstrike();
            }
            if (target instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 10));
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 10, 10));
            }
            pommelTick = 120 - (int) (120 * this.getCooldownCoeff());
            return super.tryAttack(target);
        }
        if (!performing) {
            if (swingBool) {
                dispatcher.setSwing();
            } else {
                dispatcher.setSwing2();
            }
            swingBool = !swingBool;
        }
        return super.tryAttack(target);
    }
   /* @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        super.registerControllers(animationData);
        animationData.add(
                new AnimationController<>(this, "prepare", event -> PlayState.CONTINUE)
                        .triggerableAnim("prepare", PREPARE));
        animationData.add(
                new AnimationController<>(this, "throw1", event -> PlayState.CONTINUE)
                        .triggerableAnim("throw1", THROW1));
        animationData.add(
                new AnimationController<>(this, "throw2", event -> PlayState.CONTINUE)
                        .triggerableAnim("throw2", THROW2));

                animationData.add(
                new AnimationController<>(this, "pommelstrike", event -> PlayState.CONTINUE)
                        .triggerableAnim("pommelstrike", POMMELSTRIKE));

        animationData.add(
                new AnimationController<>(this, "swing1", event -> PlayState.CONTINUE)
                        .triggerableAnim("swing1", SWING1));
        animationData.add(
                new AnimationController<>(this, "swing2", event -> PlayState.CONTINUE)
                        .triggerableAnim("swing2", SWING2));
        animationData.add(
                new AnimationController<>(this, "roll", event -> PlayState.CONTINUE)
                        .triggerableAnim("roll", ROLL));
        animationData.add(
                new AnimationController<>(this, "dashleft", event -> PlayState.CONTINUE)
                        .triggerableAnim("dashleft", DASHLEFT));
        animationData.add(
                new AnimationController<>(this, "dashright", event -> PlayState.CONTINUE)
                        .triggerableAnim("dashright", DASHRIGHT));


    }*/
}
