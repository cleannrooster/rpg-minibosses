package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.AI.JuggernautLeapSlamGoal;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.Animation;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
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
    private boolean performing;
    List<Item> bonusList;


    protected JuggernautEntity(EntityType<? extends PatrolEntity> entityType, World world) {
        super(entityType, world);
        super.bonusList = Registries.ITEM.stream().filter(item -> {return
                (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_2_weapons")))
                        ||new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_3_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_4_weapons")))
                        || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","loot_tier/tier_5_weapons"))))
                        && ( new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM,Identifier.of("rpg_series","weapon_type/hammer"))))
                ;}).toList();

    }
    protected JuggernautEntity(EntityType<? extends PatrolEntity> entityType, World world,boolean lesser) {
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
    protected JuggernautEntity(EntityType<? extends PatrolEntity> entityType, World world, boolean lesser, float SpawnCoeff) {
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
    public static final RawAnimation LEAPSLAM = RawAnimation.begin().thenPlay("animation.mob.jugg.leapslam");
    public static final RawAnimation TWOHANDWAVE = RawAnimation.begin().then("animation.mob.wizard.staffwave", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation TWOHANDSPIN = RawAnimation.begin().thenPlayXTimes("animation.mob.spin_2h", 4);
    public static final RawAnimation WINDDOWN = RawAnimation.begin().thenPlay("animation.mob.spinwinddown");

    public static final RawAnimation SLAM = RawAnimation.begin().thenPlayXTimes("animation.mob.heavy.slam", 1);
    public static final RawAnimation SWING1 = RawAnimation.begin().then("animation.mob.swing1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation SWING2 = RawAnimation.begin().then("animation.mob.swing2", Animation.LoopType.PLAY_ONCE);

    public boolean skipOffHand(){
        return true;
    }
    @Override
    protected void initCustomGoals() {

        this.goalSelector.add(2, new MeleeAttackGoal(this,1.0F,true));

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

        if(this.getTarget() != null) {
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,this.getTarget().getEyePos());
        }
        if(defensetimer > 0) {
            if(this.getTarget()  != null ) {
                if (this.getTarget().distanceTo(this) > 4) {
                    this.getMoveControl().moveTo(this.getTarget().getX(), this.getTarget().getY(), this.getTarget().getZ(), 1F);
                } else {
                    this.getMoveControl().strafeTo(-1, this.getTarget().getPos().subtract(this.getPos()).crossProduct(new Vec3d(0, 1, 0)).dotProduct(this.getRotationVector()) > 0 ? -0.6F : 0.6F);

                }
                if (!this.getWorld().isClient() && slamtimer > 140 && !this.performing && this.getTarget() != null && this.isAttacking() && this.distanceTo(this.getTarget()) <= 3) {
                    this.resetIndicator();
                    ((WorldScheduler) this.getWorld()).schedule(20, () -> {

                        ((JuggernautEntity) this).triggerAnim("slam", "slam");
                        ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                            ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
                            for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Target.Area(), null)) {
                                SpellHelper.performImpacts(this.getWorld(), this, entity, this, SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "pound")).get(),
                                        SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).impacts, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, this)).position(this.getPos()));

                            }
                            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                            this.performing = false;

                        });

                    });
                    this.slamtimer = 140 - (int) (140 * this.getCooldownCoeff());
                    this.performing = true;
                }
            }
            if(this.defensetimer > defensetime){
                this.defensetimer = -80 - this.getRandom().nextInt(80);
                this.defensetime = 80 + this.getRandom().nextInt(80);
            }
        }
        if(!this.getWorld().isClient() && spintimer > 460 && !this.performing && this.getTarget() != null && this.isAttacking() && this.distanceTo(this.getTarget()) <= 10) {
            this.resetIndicator();

                ((JuggernautEntity) this).triggerAnim("twohandwave", "twohandwave");

                ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                    ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);

                    ((JuggernautEntity) this).triggerAnim("twohandspin", "twohandspin");
                    this.addVelocity(this.getRotationVector().subtract(0, this.getRotationVector().getY(), 0).multiply(2));
                    ((WorldScheduler) this.getWorld()).schedule(4*(2*20), () -> {

                        ((JuggernautEntity) this).triggerAnim("winddown", "winddown");
                        ((WorldScheduler) this.getWorld()).schedule(20, () -> {

                            this.performing = false;
                        });
                    });
                });

                ((WorldScheduler) this.getWorld()).schedule(40 * 2, () -> {
                    ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);

                    this.addVelocity(this.getRotationVector().subtract(0, this.getRotationVector().getY(), 0).multiply(2));

                });
                ((WorldScheduler) this.getWorld()).schedule(40 * 3, () -> {
                    ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);

                    this.addVelocity(this.getRotationVector().subtract(0, this.getRotationVector().getY(), 0).multiply(2));

                });
                ((WorldScheduler) this.getWorld()).schedule(40 * 4, () -> {
                    ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);

                    this.addVelocity(this.getRotationVector().subtract(0, this.getRotationVector().getY(), 0).multiply(2));

                });

            this.spintimer = 460 - (int)(460*this.getCooldownCoeff());
            this.performing = true;

        }
        if(!this.getWorld().isClient() && leapTimer > 160 && !this.performing && this.getTarget() != null && this.isAttacking()&& this.distanceTo(this.getTarget()) >= 6) {
            this.resetIndicator();

            ((WorldScheduler) this.getWorld()).schedule(20, () -> {

                ((JuggernautEntity) this).triggerAnim("leapslam", "leapslam");
                ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                    this.addVelocity(this.getRotationVector().subtract(0,this.getRotationVector().getY(),0).multiply(2).add(0, 0.5, 0));
                });

                ((WorldScheduler) this.getWorld()).schedule(28, () -> {
                    ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
                    for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Target.Area(), null)) {
                        SpellHelper.performImpacts(this.getWorld(), this, entity, this, SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "pound")).get(),
                                SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).impacts, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, this)).position(this.getPos()));

                    }
                    this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                    this.performing = false;

                });
            });
            this.leapTimer = 160 - (int)(160*this.getCooldownCoeff());
            this.performing = true;

        }

        if(!this.getWorld().isClient()) {
            slamtimer++;
            spintimer++;
            leapTimer++;
            defensetimer++;

        }
        super.mobTick();

    }

    public int defensetimer;


    public int leapTimer;
    public int spintimer = 300;

    public int slamtimer = 60;

    @Override
    public void tick() {

        super.tick();
    }
    public boolean   swingBool;

    public boolean tryAttack(Entity target) {
        if(!performing) {
            if (swingBool) {
                (this).triggerAnim("swing1", "swing1");
                swingBool = false;

            } else {
                (this).triggerAnim("swing2", "swing2");
                swingBool = true;

            }
        }
        return super.tryAttack(target);

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
        animationData.add(
                new AnimationController<>(this, "down", event -> PlayState.CONTINUE)
                        .triggerableAnim("down", DOWNANIM));
    }
}
