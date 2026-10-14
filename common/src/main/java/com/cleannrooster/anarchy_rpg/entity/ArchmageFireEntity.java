package com.cleannrooster.anarchy_rpg.entity;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.client.entity.effect.Effects;
import com.cleannrooster.anarchy_rpg.entity.AI.ArtilleristCrossbowAttackGoal;

import com.cleannrooster.anarchy_rpg.entity.brain.impl.FireMageBrain;
import com.cleannrooster.anarchy_rpg.entity.brain.impl.RogueBrain;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ArchmageFireEntity extends MinibossEntity  {
    public List<Item> bonusList = new ArrayList<>();

    protected ArchmageFireEntity(EntityType<? extends PathAwareEntity> entityType, World world, float spawnCoeff) {
        super(entityType, world, spawnCoeff);
        super.bonusList = Registries.ITEM.stream().filter(item -> {
            return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                            && new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/damage_staff")));
        }).toList();
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
    public void registerAttackPrototypes() {
        // Discarded immediately; building it is what registers this mob's attack geometry
        // with AttackRegistry, which the client needs to draw incoming swings.
        new FireMageBrain(this);
    }

    @Override
    protected void initCustomGoals() {
        this.brain = new FireMageBrain(this);

        this.goalSelector.add(2, new com.cleannrooster.anarchy_rpg.entity.brain.MobBrainGoal(this, brain));
        super.initCustomGoals();
    }
    @Override
    public boolean isTwoHand() {
        return false;
    }
    /**
     * Ticks of incantation left to mouth. Tracked so the client can drive the mouth texture without
     * needing to know anything about the combat action that started it.
     */
    public static final TrackedData<Integer> TALK_TICKS =
            DataTracker.registerData(ArchmageFireEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** Mouthed for the length of a quick cast. */
    public static final int TALK_SHORT = 14;
    /** Mouthed across the volley's long charge, stopping short of its recovery. */
    public static final int TALK_LONG = 58;

    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TALK_TICKS, 0);
    }

    /** Starts (or extends) the mouth flap. Server-side; the countdown syncs to the client. */
    public void startTalking(int ticks) {
        if (this.getWorld().isClient()) return;
        this.dataTracker.set(TALK_TICKS, Math.max(this.dataTracker.get(TALK_TICKS), ticks));
    }

    public boolean isTalking() {
        return this.dataTracker.get(TALK_TICKS) > 0;
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
            if (!this.getWorld().isClient()) {
                int talking = this.dataTracker.get(TALK_TICKS);
                if (talking > 0) {
                    this.dataTracker.set(TALK_TICKS, talking - 1);
                }
            }
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
