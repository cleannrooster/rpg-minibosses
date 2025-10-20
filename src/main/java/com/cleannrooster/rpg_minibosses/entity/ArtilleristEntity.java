package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.AI.ArtilleristCrossbowAttackGoal;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
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
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CHARGING, false);
        this.dataTracker.startTracking(EXTRACHARGE, false);
        this.dataTracker.startTracking(RUNNING, false);

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
    public boolean performing = false;
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
        if((this.getTarget() != null && !this.performing) && ((this.getTarget().distanceTo(this) < 4 && this.sinceRunning > 160) || this.sinceRunning > 240 )){
            this.startRunning = true;
            this.getDataTracker().set(RUNNING,true);
            this.runningTick = 80;
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
                state.setControllerSpeed((float) (state.isMoving() ? this.getVelocity().length()/0.4F : 1F));

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

    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {

        this.shoot(target,speed);
    }

    static {
        CHARGING = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        EXTRACHARGE = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        RUNNING = DataTracker.registerData(ArtilleristEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {

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

    public void shoot(LivingEntity entity, float speed) {
        Hand hand = ProjectileUtil.getHandPossiblyHolding(entity, Items.CROSSBOW);
        ItemStack itemStack = entity.getStackInHand(hand);
        float fs = getSoundPitch();

            shoot(entity.getWorld(), this, hand, new ItemStack(Items.ARROW), itemStack, fs, true, speed,  (float)(14 - entity.getWorld().getDifficulty().getId() * 4), 0.0F);


        this.postShoot();
    }
    private  void shoot(World world, LivingEntity shooter, Hand hand, ItemStack crossbow, ItemStack projectile, float soundPitch, boolean creative, float speed, float divergence, float simulated) {
        if (!world.isClient) {
            boolean bl = projectile.isOf(Items.FIREWORK_ROCKET);
            ProjectileEntity projectileEntity;
            if (bl) {
                projectileEntity = new FireworkRocketEntity(world, projectile, shooter, shooter.getX(), shooter.getEyeY() - (double)0.15F, shooter.getZ(), true);
            } else {
                projectileEntity = createArrow(world, shooter, crossbow, projectile);
                if (creative || simulated != 0.0F) {
                    ((PersistentProjectileEntity)projectileEntity).pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                }
            }

            if (shooter instanceof CrossbowUser crossbowUser && crossbowUser.getTarget() != null) {
                this.shoot( shooter,  ((CrossbowUser) shooter).getTarget(),  projectileEntity, 0,  speed);
            } else {
                Vec3d vec3d = shooter.getOppositeRotationVector(1.0F);
                Quaternionf quaternionf = (new Quaternionf()).setAngleAxis((double)(simulated * ((float)Math.PI / 180F)), vec3d.x, vec3d.y, vec3d.z);
                Vec3d vec3d2 = shooter.getRotationVec(1.0F);
                Vector3f vector3f = vec3d2.toVector3f().rotate(quaternionf);
                projectileEntity.setVelocity((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), speed, divergence);
            }

            world.spawnEntity(projectileEntity);
            world.playSound((PlayerEntity)null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0F, soundPitch);
        }
    }
    private static PersistentProjectileEntity createArrow(World world, LivingEntity entity, ItemStack crossbow, ItemStack arrow) {
        ArrowItem arrowItem = (ArrowItem)(arrow.getItem() instanceof ArrowItem ? arrow.getItem() : Items.ARROW);
        PersistentProjectileEntity persistentProjectileEntity = arrowItem.createArrow(world, arrow, entity);
        if (entity instanceof PlayerEntity) {
            persistentProjectileEntity.setCritical(true);
        }

        persistentProjectileEntity.setSound(SoundEvents.ITEM_CROSSBOW_HIT);
        persistentProjectileEntity.setShotFromCrossbow(true);
        int i = EnchantmentHelper.getLevel(Enchantments.PIERCING, crossbow);
        if (i > 0) {
            persistentProjectileEntity.setPierceLevel((byte)i);
        }

        return persistentProjectileEntity;
    }

    public  void shoot(LivingEntity entity, LivingEntity target, ProjectileEntity projectile, float multishotSpray, float speed) {
        double d = target.getX() - entity.getX();
        double e = target.getZ() - entity.getZ();
        double f = Math.sqrt(d * d + e * e);
        double g = target.getBodyY(0.3333333333333333) - projectile.getY() + f * (double)0.2F;
        Vector3f vector3f = this.getProjectileLaunchVelocity(entity, new Vec3d(d, g, e), multishotSpray);
        projectile.setVelocity((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), speed, (float)(14 - entity.getWorld().getDifficulty().getId() * 4));
        entity.playSound(SoundEvents.ITEM_CROSSBOW_SHOOT, 1.0F, 1.0F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
    }
}
