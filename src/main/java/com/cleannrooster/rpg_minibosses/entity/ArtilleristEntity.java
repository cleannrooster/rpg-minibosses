package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.AI.ArtilleristCrossbowAttackGoal;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.sblforked.api.core.behaviour.custom.misc.Panic;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.brain.task.PanicTask;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.AttackGoal;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;

import java.util.List;
import java.util.Optional;


public class ArtilleristEntity extends MinibossEntity implements RangedAttackMob, CrossbowUser {
    List<Item> bonusList = List.of();

    protected ArtilleristEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
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
    protected ArtilleristEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser) {
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
    protected ArtilleristEntity(EntityType<? extends PathAwareEntity> entityType, World world,boolean lesser,float spawnCoeff) {
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
    public static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.merc.heavy_run");
    public static final RawAnimation RUN_THROW = RawAnimation.begin().thenPlay("animation.merc.heavy_run_THROW");
    public static final RawAnimation SHOOT_HEAVY = RawAnimation.begin().thenPlay("animation.merc.shoot_heavy");
    public static final RawAnimation SHOOT_HEAVY_MANY = RawAnimation.begin().thenPlay("animation.merc.shoot_heavy_many");
    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("animation.merc.idle");
    public static final RawAnimation IDLE_TRUE = RawAnimation.begin().thenPlay("idle");

    public static final RawAnimation RELOAD = RawAnimation.begin().thenPlay("animation.unknown.merc.reload");
    public static final RawAnimation RELOADLONGER = RawAnimation.begin().thenPlay("animation.unknown.merc.reloadlonger");

    @Override
    public MoveControl getMoveControl() {
        return super.getMoveControl();
    }

    @Override
    protected void initCustomGoals() {
        this.goalSelector.add(0, new ArtilleristCrossbowAttackGoal<>(this,0.5,16));

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
        builder.add(RUNNING, false);

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
    public int trapCooldown = 160;
    @Override
    public void tick() {

            super.tick();
        if(this.getTarget() != null && !this.getDataTracker().get(RUNNING)) {
            this.getLookControl().lookAt(this.getTarget(), 360, 390);

        }
    }

    @Override
    public LookControl getLookControl() {
        return super.getLookControl();
    }

    @Override
    protected void mobTick() {

        super.mobTick();
        if((this.getTarget() != null && !this.performing) && ((this.getTarget().distanceTo(this) < 4 && this.sinceRunning > 180) || this.sinceRunning > 260 )){
            this.resetIndicator();
            ((WorldScheduler) this.getWorld()).schedule(20, () -> {

                this.startRunning = true;
                this.getDataTracker().set(RUNNING, true);
                this.runningTick = 80;
            });
            this.sinceRunning = 0;


        }
        if(this.startRunning){
            if(!this.getNavigation().isFollowingPath()) {
                Vec3d vec3d = NoPenaltyTargeting.find(this, 16, 12);
                for(int i = 0 ; i < 8; i++){
                    Vec3d newVec = NoPenaltyTargeting.find(this, 16, 12);

                    if(vec3d == null || (newVec != null && newVec.getY()> vec3d.getY())){
                        vec3d = newVec;
                    }
                }
                if(vec3d != null) {
                    this.getNavigation().startMovingTo(vec3d.getX(), vec3d.getY(), vec3d.getZ(), 2);

                    if(this.trapCooldown <= 0){
                        for(int i = 0; i < 4; i++){
                            TrapCleann trap = new TrapCleann(RPGMinibossesEntities.TRAP, this, this.getWorld(), Identifier.of(RPGMinibosses.MOD_ID,"explosion"), new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_RANGED,this)));
                            trap.setPosition(this.getEyePos());
                            trap.setVelocity(this.getRotationVector().multiply(0.1).rotateY(i*90));
                            trap.setYaw(this.getYaw());
                            trap.prevYaw = this.getYaw();
                            this.getWorld().spawnEntity(trap);
                            this.getWorld().playSound((PlayerEntity) null, trap.getX(), trap.getY(), trap.getZ(), SoundEvents.ENTITY_ARMOR_STAND_PLACE, SoundCategory.BLOCKS, 0.75F, 0.8F);


                        }
                        this.trapCooldown = 160;
                    }
                }

            }
            this.runningTick--;
        }
        if(this.startRunning && (this.runningTick  <= 0 || (this.getTarget()!= null &&( this.getTarget().isDead() || this.getTarget().distanceTo(this) > 16 || !this.canSee(this.getTarget()))))){
            this.startRunning = false;
            this.getDataTracker().set(RUNNING,false);
            this.getNavigation().stop();

        }
        if(!this.getDataTracker().get(RUNNING) && this.getTarget() != null){
            this.getNavigation().stop();
        }
        if(!this.getWorld().isClient()){
            this.sinceRunning++;
            this.trapCooldown--;
        }

    }
    public boolean startRunning = false;
    public int runningTick = 0;
    public int runningCooldown = 160;
    public int sinceRunning = 0;

    private PlayState predicateShoot(AnimationState<MinibossEntity> state) {
        state.setControllerSpeed((float) (state.isMoving() ? this.getVelocity().length()/0.1F : 1F));

        if(this.getDataTracker().get(DOWN)){
            return  state.setAndContinue(DOWNANIM);
        }
        if (state.isMoving()) {
            if(this.getDataTracker().get(RUNNING)){
                return state.setAndContinue(RUN);

            }
            return this.isAttacking() ? state.setAndContinue(WALK) : this.getVelocity().length() > 0.2F ? state.setAndContinue(SPRINT):  state.setAndContinue(WALK_NO_AGGRO);
        }
        else{
            return this.isAttacking() ? state.setAndContinue(IDLE) : state.setAndContinue(IDLE_TRUE);

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
                new AnimationController<>(this, "shoot_heavy", event -> PlayState.CONTINUE)
                        .triggerableAnim("shoot_heavy", SHOOT_HEAVY));
        animationData.add(
                new AnimationController<>(this, "shoot_heavy_many", event -> PlayState.CONTINUE)
                        .triggerableAnim("shoot_heavy_many", SHOOT_HEAVY_MANY));

    }

    public static final TrackedData<Boolean> CHARGING;
    public static final TrackedData<Boolean> EXTRACHARGE;
    public static final TrackedData<Boolean> RUNNING;


    public void setCharging(boolean charging) {
        this.dataTracker.set(CHARGING, charging);
    }
    static {
        CHARGING = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        EXTRACHARGE = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        RUNNING = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    }
    public class ArtilleristLookControl extends LookControl {
        protected final MobEntity entity;
        protected float maxYawChange;
        protected float maxPitchChange;
        protected int lookAtTimer;
        protected double x;
        protected double y;
        protected double z;
        public ArtilleristLookControl(MobEntity entity) {
            super(entity);
            this.entity = entity;
        }

        @Override
        protected boolean shouldStayHorizontal() {
            return false;
        }
    }
    @Override
    public void postShoot() {

    }
    public void shootAt(LivingEntity target, float pullProgress) {
        this.shoot(this, 1.6F);
    }

}
