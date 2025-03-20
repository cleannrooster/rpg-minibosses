package com.cleannrooster.rpg_minibosses.entity;

import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.Animation;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class TricksterEntity extends MinibossEntity{
    protected TricksterEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    public static final RawAnimation SWING1 = RawAnimation.begin().then("animation.mob.swing1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation SWING2 = RawAnimation.begin().then("animation.mob.swing2", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation POMMELSTRIKE = RawAnimation.begin().then("animation.mob.trickster.pommelstrike", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation ROLL = RawAnimation.begin().then("animation.mob.trickster.roll", Animation.LoopType.PLAY_ONCE);

    public boolean   swingBool;
    @Override
    public boolean isTwoHand() {
        return false;
    }

    @Override
    protected void initCustomGoals() {

        this.goalSelector.add(2, new MeleeAttackGoal(this,1F,true));

        super.initCustomGoals();
    }
    public ItemStack getMainWeapon(){
        return Items.IRON_SWORD.getDefaultStack();

    }
   public int pommelTick = 100;
    public int rolltimer = 40;

    @Override
    protected void mobTick() {
        if(!this.getWorld().isClient() && rolltimer > 80 &&  this.getTarget() != null && this.isAttacking()) {
            ((TricksterEntity)this).triggerAnim("roll","roll");
                this.addVelocity(this.getRotationVector().multiply(2F));

            this.rolltimer = 0;
        }
        if(!this.getWorld().isClient()){
            pommelTick++;
            rolltimer++;
        }
        super.mobTick();
    }

    @Override
    public boolean tryAttack(Entity target) {
        if(pommelTick > 120){
            ((TricksterEntity)this).triggerAnim("pommelstrike","pommelstrike");
            if(target instanceof LivingEntity living){
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,20,10));
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS,10,10));

            }
            pommelTick = 0;
            return super.tryAttack(target);
        }
        else if(swingBool){
            ((TricksterEntity)this).triggerAnim("swing1","swing1");
            swingBool = false;
            return super.tryAttack(target);

        }
        else{
            ((TricksterEntity)this).triggerAnim("swing2","swing2");
            swingBool = true;
            return super.tryAttack(target);

        }
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        super.registerControllers(animationData);
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

    }
}
