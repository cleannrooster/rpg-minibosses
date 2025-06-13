package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.entity.AI.ArtilleristCrossbowAttackGoal;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class ArtilleristEntity extends MinibossEntity implements RangedAttackMob, CrossbowUser {
    List<Item> bonusList = List.of();

    protected ArtilleristEntity(EntityType<? extends PatrolEntity> entityType, World world) {
        super(entityType, world);
        super.bonusList = Registries.ITEM.stream().filter(item -> {return
                (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                        ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/heavy_crossbow")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/long_bow")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/short_bow")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/rapid_crossbow"))));}).toList();

}
    protected ArtilleristEntity(EntityType<? extends PatrolEntity> entityType, World world, boolean lesser) {
        super(entityType, world);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                                ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/heavy_crossbow")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/long_bow")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/short_bow")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/rapid_crossbow"))));
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/heavy_crossbow")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/long_bow")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/short_bow")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/rapid_crossbow"))));}).toList();

        }
    }
    protected ArtilleristEntity(EntityType<? extends PatrolEntity> entityType, World world,boolean lesser,float spawnCoeff) {
        super(entityType, world,spawnCoeff);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                                ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/heavy_crossbow")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/long_bow")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/short_bow")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/rapid_crossbow"))));
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);

        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                            ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                            && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/heavy_crossbow")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/long_bow")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/short_bow")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/rapid_crossbow"))));}).toList();

        }
    }
    public static final RawAnimation IDLESHOOT = RawAnimation.begin().thenLoop("animation.mob.idleshoot");
    public static final RawAnimation SHOOTWALK_BACKWARDS = RawAnimation.begin().thenLoop("animation.unknown.walk_backwards_shoot");
    public static final RawAnimation SHOOTWALK_BACKWARDST = RawAnimation.begin().thenPlay("animation.unknown.walk_backwards_shoot_transition").thenLoop("animation.unknown.walk_backwards_shoot");

    public static final RawAnimation SHOOTWALK = RawAnimation.begin().thenLoop("animation.mob.shootwalk");
    public static final RawAnimation SHOOTWALKT = RawAnimation.begin().thenPlay("animation.mob.shootwalk2").thenLoop("animation.mob.shootwalk");

    public static final RawAnimation RELOAD = RawAnimation.begin().thenPlay("animation.unknown.merc.reload");
    public static final RawAnimation RELOADLONGER = RawAnimation.begin().thenPlay("animation.unknown.merc.reloadlonger");

    @Override
    public MoveControl getMoveControl() {
        return super.getMoveControl();
    }

    @Override
    protected void initCustomGoals() {
        this.goalSelector.add(2, new ArtilleristCrossbowAttackGoal<>(this,1,12));

        super.initCustomGoals();
    }

    @Override
    public boolean isTwoHand() {
        return false;
    }
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CHARGING, false);
        builder.add(EXTRACHARGE, false);

    }
    public Item getDefaultItem(){
        return Items.AIR;
    }
    public ItemStack getBackWeapon(){
        ItemStack stack = new ItemStack(getDefaultItem());
        if(this.getRandom().nextBoolean() && !bonusList.isEmpty()){
            Item item = bonusList.get(this.getRandom().nextInt(bonusList.size()));
            stack = new ItemStack(item);

        }

        return stack;

    }
    public ItemStack getMainWeapon(){
        return Items.CROSSBOW.getDefaultStack();

    }
    public boolean skipMainHand(){
        return true;
    }
    @Override
    public void tick() {

            super.tick();
        if(this.getTarget() != null) {
            this.getLookControl().lookAt(this.getTarget(), 360, 390);

        }
    }

    @Override
    protected void mobTick() {

        super.mobTick();


    }

    private PlayState predicateShoot(AnimationState<MinibossEntity> state) {
        if(this.getDataTracker().get(CHARGING)) {
            if(this.getDataTracker().get(EXTRACHARGE)) {
                return state.setAndContinue(RELOADLONGER);

            }else {
                return state.setAndContinue(RELOAD);
            }

        }
        if(CrossbowItem.isCharged(this.getMainHandStack())) {
            if (state.isMoving()) {
                if(this.getVelocity().normalize().dotProduct(this.getRotationVector().normalize()) < -0.2 ){
                    return state.setAndContinue(SHOOTWALK_BACKWARDST);
                }
                return state.setAndContinue(SHOOTWALKT);

            } else {
                return state.setAndContinue(IDLESHOOT);

            }

        }

        if (state.isMoving()) {
            if(this.getVelocity().length() > 0.06F && this.getVelocity().dotProduct(this.getRotationVector()) < -0.2  ){
                return state.setAndContinue(WALK_B_T);
            }
            return state.setAndContinue(WALK);
        }
        else{
            return state.setAndContinue(IDLE);

        }

    }

    @Override
    public void onAttacking(Entity target) {
        target.timeUntilRegen = 0;
        if(target instanceof LivingEntity living){
            living.hurtTime = 0;
        }
        super.onAttacking(target);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        animationData.add(new AnimationController<MinibossEntity>(this,"shoot",
                0,this::predicateShoot)
        );
        animationData.add(
                new AnimationController<>(this, "down", event -> PlayState.CONTINUE)
                        .triggerableAnim("down", DOWNANIM));
    }

    public static final TrackedData<Boolean> CHARGING;
    public static final TrackedData<Boolean> EXTRACHARGE;


    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }
    static {
        CHARGING = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        EXTRACHARGE = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    }

    @Override
    public void postShoot() {

    }
    public void shootAt(LivingEntity target, float pullProgress) {
        this.shoot(this, 1.6F);
    }

}
