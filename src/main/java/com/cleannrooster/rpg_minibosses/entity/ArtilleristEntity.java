package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.entity.AI.ArtilleristCrossbowAttackGoal;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

public class ArtilleristEntity extends MinibossEntity implements RangedAttackMob, CrossbowUser {
    protected ArtilleristEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static final RawAnimation IDLESHOOT = RawAnimation.begin().thenLoop("animation.mob.idleshoot");

    public static final RawAnimation SHOOTWALK = RawAnimation.begin().thenLoop("animation.mob.shootwalk");
    public static final RawAnimation RELOAD = RawAnimation.begin().thenPlay("animation.unknown.merc.reload");

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
    }
    public ItemStack getMainWeapon(){
        return Items.CROSSBOW.getDefaultStack();

    }

    @Override
    public void tick() {

            super.tick();
    }

    @Override
    protected void mobTick() {
        if(this.getTarget() != null) {
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,this.getTarget().getEyePos());
        }
        super.mobTick();
    }

    private PlayState predicateShoot(AnimationState<MinibossEntity> state) {
        if(this.getDataTracker().get(CHARGING)) {
            return state.setAndContinue(RELOAD);

        }
        if(CrossbowItem.isCharged(this.getMainHandStack())) {
            if (state.isMoving()) {

                return state.setAndContinue(SHOOTWALK);

            } else {
                return state.setAndContinue(IDLESHOOT);

            }

        }

        if (state.isMoving()) {
            return state.setAndContinue(WALK);
        }
        else{
            return state.setAndContinue(IDLE);

        }

    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        animationData.add(new AnimationController<MinibossEntity>(this,"shoot",
                0,this::predicateShoot)
        );

    }

    private static final TrackedData<Boolean> CHARGING;


    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }
    static {
        CHARGING = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    }
    @Override
    public void postShoot() {

    }
    public void shootAt(LivingEntity target, float pullProgress) {
        this.shoot(this, 1.6F);
    }

}
