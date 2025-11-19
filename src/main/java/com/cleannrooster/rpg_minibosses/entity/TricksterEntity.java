package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.AI.RogueNodeMaker;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.Animation;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
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
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.List;
import java.util.Optional;

import static net.spell_engine.utils.VectorHelper.angleBetween;

public class TricksterEntity extends MinibossEntity{
    List<Item> bonusList = List.of();
    private boolean ambushing;

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
    protected void initCustomGoals() {

        this.goalSelector.add(2, new MeleeAttackGoal(this,1F,true));

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
    public int rolltimer = 40;
    public int defensetimer;
    public int defensetime = 80;
    public int dashtimer = 80;

    public int throwtimer;
/*
    public static final RawAnimation THROW1 = RawAnimation.begin().then("animation.mob.throw1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation THROW2 = RawAnimation.begin().then("animation.mob.throw2", Animation.LoopType.PLAY_ONCE);

    public static final RawAnimation DASHRIGHT = RawAnimation.begin().thenPlay("animation.valkyrie.dashright");
    public static final RawAnimation DASHLEFT = RawAnimation.begin().thenPlay("animation.valkyrie.dashleft");
*/


    @Override
    protected void mobTick() {



        if (this.getTarget() != null) {
            this.getLookControl().lookAt(this.getTarget(),360,360);
        }
        if(!this.getWorld().isClient() && rolltimer > 80 &&  this.getTarget() != null && this.isAttacking()) {
           dispatcher.roll();
                this.addVelocity(this.getRotationVector().multiply(2F));

            this.rolltimer = 80 - (int)(180*this.getCooldownCoeff());
        }

        if(pommelTick == 120){
            this.playSound(SoundEvents.ENTITY_PILLAGER_AMBIENT);

        }
        if(!this.getWorld().isClient()){
            pommelTick++;
            rolltimer++;
            dashtimer++;
            defensetimer++;
            throwtimer++;
        }
        if(defensetimer >= 0 && !this.getWorld().isClient()){
            if(this.getTarget()  != null && this.canSee(this.getTarget()) ){

                if( this.getTarget().distanceTo(this) > 4){
                }
                else{
                    ((MinibossMoveConrol)this.getMoveControl()).strafeTo(-2.5F, this.getTarget().getPos().subtract(this.getPos()).crossProduct(new Vec3d(0, 1, 0)).dotProduct(this.getRotationVector()) > 0 ? -0.6F : 0.6F,0.75F);

                }
                if(!this.getWorld().isClient() && throwtimer > 80 && !this.performing && this.getTarget() != null  && this.distanceTo(this.getTarget()) > 4) {
                    this.resetIndicator();
                    dispatcher.setPrepare();

                    ((WorldScheduler) this.getWorld()).schedule(20, () -> {

                        dispatcher.throw1();
                        if (this.getTarget() != null) {
                            this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, this.getTarget().getEyePos());
                        }
                        SoundHelper.playSound(this.getWorld(), this, new Sound(Identifier.of("minecraft:entity.player.attack.sweep")));
                        SpellHelper.shootProjectile(this.getWorld(), this, this.getTarget(), SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).get(),
                                new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, this)).position(this.getPos()));

                        ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).release.particles);

                        ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                            dispatcher.throw2();
                            this.performing = false;
                            if (this.getTarget() != null) {
                                this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, this.getTarget().getEyePos());
                            }
                            SoundHelper.playSound(this.getWorld(), this, new Sound(Identifier.of("minecraft:entity.player.attack.sweep")));

                            SpellHelper.shootProjectile(this.getWorld(), this, this.getTarget(), SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).get(),
                                    new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, this)).position(this.getPos()));

                            ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).release.particles);

                        });

                    });
                    this.throwtimer = 80 - (int)(180*this.getCooldownCoeff());
                    this.performing = true;
                }

            }
            if(this.defensetimer > defensetime){
                this.defensetimer = -80 - this.getRandom().nextInt(80);
                this.defensetime = 80 + this.getRandom().nextInt(80);
            }
        }
        if(!this.ambushing && this.getTarget() != null && this.distanceTo(this.getTarget()) > 12){
            this.ambushing = true;

        }
        super.mobTick();

    }

    @Override
    public boolean isMobile() {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(!this.getDataTracker().get(DOWN) &&  amount > 4 && !this.getWorld().isClient() && this.defensetimer > 0 && dashtimer > 80 && !this.performing && this.getTarget() != null  ) {
            if(this.getTarget().getPos().subtract(this.getPos()).crossProduct(new Vec3d(0,1,0)).dotProduct(this.getRotationVector()) > 0 ) {
                dispatcher.dashleft();
                this.setVelocity(this.getRotationVector().crossProduct(new Vec3d(0,-1,0)).multiply(2));
            }
            else{
                dispatcher.dashright();
                this.setVelocity(this.getRotationVector().crossProduct(new Vec3d(0,1,0)).multiply(2));

            }
            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

            ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                        this.performing = false;

                    }
            );
            this.dashtimer = 0;
            this.defensetimer += 40;
            this.performing = true;
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
    @Override
    public boolean tryAttack(Entity target) {
        this.ambushing = false;

        if(pommelTick > 120){

            dispatcher.setPommelstrike();
            if(target instanceof LivingEntity living){
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,20,10));
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS,10,10));

            }
            pommelTick = 120 - (int)(120*this.getCooldownCoeff());
            return super.tryAttack(target);
        }
        else if(swingBool){
            dispatcher.setSwing();
            swingBool = false;
            return super.tryAttack(target);

        }
        else{
            dispatcher.setSwing2();
            swingBool = true;
            return super.tryAttack(target);

        }
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
