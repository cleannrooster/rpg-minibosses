package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.AI.JuggernautLeapSlamGoal;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.Animation;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.WorldScheduler;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

public class JuggernautEntity extends MinibossEntity{
    private boolean performing;

    protected JuggernautEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }
    public static final RawAnimation LEAPSLAM = RawAnimation.begin().thenPlay("animation.mob.jugg.leapslam");
    public static final RawAnimation TWOHANDWAVE = RawAnimation.begin().then("animation.mob.wizard.staffwave", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation TWOHANDSPIN = RawAnimation.begin().thenPlayXTimes("animation.mob.spin_2h", 4);
    public static final RawAnimation SLAM = RawAnimation.begin().thenPlayXTimes("animation.mob.heavy.slam", 1);

    @Override
    protected void initCustomGoals() {

        this.goalSelector.add(2, new MeleeAttackGoal(this,1.0F,true));

        super.initCustomGoals();
    }
    @Override
    public boolean isTwoHand() {
        return true;
    }
    public ItemStack getMainWeapon(){
        return Items.IRON_AXE.getDefaultStack();

    }



            public void applyIntroEffect(){
        super.applyIntroEffect();
    }
    public RegistryEntry<StatusEffect> getIntroEffect(){
        return Effects.PETRIFIED.registryEntry;
    }
    @Override
    protected void mobTick() {
        if(this.getTarget() != null) {
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,this.getTarget().getEyePos());
        }
        if(!this.getWorld().isClient() && slamtimer > 80 && !this.performing && this.getTarget() != null && this.isAttacking() && this.distanceTo(this.getTarget()) <= 3) {
            ((JuggernautEntity)this).triggerAnim("slam","slam");
            ((WorldScheduler) this.getWorld()).schedule(20, () ->{
                ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"pound")).release.particles);
                for(Entity entity : TargetHelper.targetsFromArea(this,6,new Spell.Release.Target.Area(), null)) {
                    SpellHelper.performImpacts(this.getWorld(), this, entity, this, SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "pound")).get(),
                            SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).impact, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

                }
                this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                this.performing = false;

            });

            this.slamtimer = 0;
            this.performing = true;
        }
        if(!this.getWorld().isClient() && spintimer > 320 && !this.performing && this.getTarget() != null && this.isAttacking() && this.distanceTo(this.getTarget()) <= 10) {
            ((JuggernautEntity)this).triggerAnim("twohandwave","twohandwave");
            ((WorldScheduler) this.getWorld()).schedule(40, () ->{
                ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"pound")).release.particles);

                ((JuggernautEntity)this).triggerAnim("twohandspin","twohandspin");
                this.addVelocity(this.getRotationVector().subtract(0,this.getRotationVector().getY(),0).multiply(2));

            });
            ((WorldScheduler) this.getWorld()).schedule(40*2, () ->{
                ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"pound")).release.particles);

                this.addVelocity(this.getRotationVector().subtract(0,this.getRotationVector().getY(),0).multiply(2));

            });
            ((WorldScheduler) this.getWorld()).schedule(40*3, () ->{
                ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"pound")).release.particles);

                this.addVelocity(this.getRotationVector().subtract(0,this.getRotationVector().getY(),0).multiply(2));

            });
            ((WorldScheduler) this.getWorld()).schedule(40*4, () ->{
                ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"pound")).release.particles);

                this.addVelocity(this.getRotationVector().subtract(0,this.getRotationVector().getY(),0).multiply(2));

            });
            ((WorldScheduler) this.getWorld()).schedule(40*5, () -> {

                this.performing = false;
            });
            this.spintimer = 0;
            this.performing = true;
        }
        if(!this.getWorld().isClient() && leapTimer > 80 && !this.performing && this.getTarget() != null && this.isAttacking()&& this.distanceTo(this.getTarget()) >= 6) {
            ((JuggernautEntity)this).triggerAnim("leapslam","leapslam");
            ((WorldScheduler) this.getWorld()).schedule(10, () ->{
                this.addVelocity(this.getRotationVector().multiply(2).add(0,0.5,0));
            });

            ((WorldScheduler) this.getWorld()).schedule(28, () -> {
                ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"pound")).release.particles);
                for(Entity entity : TargetHelper.targetsFromArea(this,6,new Spell.Release.Target.Area(), null)) {
                    SpellHelper.performImpacts(this.getWorld(), this, entity, this, SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "pound")).get(),
                            SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).impact, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

                }
                this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                this.performing = false;

            });
            this.leapTimer = 0;
            this.performing = true;
        }

        if(!this.getWorld().isClient()) {
            slamtimer++;
            spintimer++;
            leapTimer++;
        }
        super.mobTick();
    }

    public int leapTimer;
    public int spintimer = 300;

    public int slamtimer = 20;

    @Override
    public void tick() {

        super.tick();
    }

    @Override
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
                new AnimationController<>(this, "slam", event -> PlayState.CONTINUE)
                        .triggerableAnim("slam", SLAM));

    }
}
