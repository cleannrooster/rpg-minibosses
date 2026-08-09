package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.AI.ArtilleristCrossbowAttackGoal;

import com.cleannrooster.rpg_minibosses.entity.brain.impl.MercenaryBrain;
import com.cleannrooster.rpg_minibosses.entity.brain.impl.RogueBrain;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.brain.task.PanicTask;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.CowEntity;
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
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
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

import static net.spell_engine.utils.VectorHelper.angleBetween;


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
   /* public static final RawAnimation IDLESHOOT = RawAnimation.begin().thenLoop("animation.mob.idleshoot");
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
*/
    @Override
    public MoveControl getMoveControl() {
        return super.getMoveControl();
    }

    /**
     * Shot timing now lives in {@link MercenaryBrain} as phase-driven actions, so
     * {@link ArtilleristCrossbowAttackGoal} is no longer installed — it fired on its own schedule next to
     * the brain's movement, which is exactly how shots ended up going off while the body was still
     * running. The goal class is retained, unused, alongside the legacy animation resource.
     */
    @Override
    protected void initCustomGoals() {
        this.brain = new MercenaryBrain(this);

        this.goalSelector.add(1, new com.cleannrooster.rpg_minibosses.entity.brain.MobBrainGoal(this, brain));
        super.initCustomGoals();
    }

    /**
     * Fire one bolt at the target. Charging state is handled implicitly — the crossbow is reloaded here,
     * because the visible "charge" is now the aim animation's hold rather than an item-use timer running
     * in parallel with the AI.
     */
    public void fireBolt(LivingEntity target) {
        if (target == null || this.getWorld().isClient()) {
            return;
        }
        this.lookAtEntity(target, 30, 30);
        this.getLookControl().lookAt(target);
        Hand hand = ProjectileUtil.getHandPossiblyHolding(this, Items.CROSSBOW);
        ItemStack stack = this.getStackInHand(hand);
        if (!(stack.getItem() instanceof CrossbowItem crossbow)) {
            return;
        }
        ArtilleristCrossbowAttackGoal.reload(this, stack);
        crossbow.shootAll(this.getWorld(), this, hand, stack, 1.6F,
                (float) (14 - this.getWorld().getDifficulty().getId() * 4), target);
        ArtilleristCrossbowAttackGoal.reload(this, stack);
        this.postShoot();
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

    /**
     * Relocation is now a real destination-seeking action owned by {@link MercenaryBrain} — it picks a
     * firing position with line of sight and a meaningfully different angle, sprints there, and arrests on
     * arrival. The loose "run somewhere random for eighty ticks while scattering traps" behaviour that used
     * to live here has been removed; {@link #startRunning} and the RUNNING flag are kept because the brain
     * still drives them and the client reads them.
     */
    @Override
    protected void mobTick() {
        super.mobTick();
        if (!this.getWorld().isClient()) {
            this.trapCooldown--;
        }
    }

    public boolean startRunning = false;
    public int runningTick = 0;



/*    @Override
    public float getPathfindingFavor(BlockPos pos) {
        var add = 1F;
        var mult = 0F;
        if(this.getTarget()  != null){
            var angle = angleBetween(this.getTarget().getRotationVector(),pos.toCenterPos().subtract(this.getTarget().getPos()));
            mult += angle/5F;
            if(pos.getY() > this.getTarget().getY()){
                add +=  (1F*(float)(pos.getY()-this.getTarget().getY()));
            }
            add += 4F - this.getWorld().getLightLevel(pos)/4F;
        }
        return add*mult*1F + super.getPathfindingFavor(pos);
    }*/

/*

    private PlayState predicateShoot(AnimationState<MinibossEntity> state) {
        state.setControllerSpeed((float) (state.isMoving() ? this.getVelocity().length()/0.1F : 1F));

        if(this.getDataTracker().get(DOWN)){
            return  state.setAndContinue(DOWNANIM);
        }
        if (state.isMoving()) {
            if(this.getDataTracker().get(RUNNING)){
                state.setControllerSpeed((float) (state.isMoving() ? this.getVelocity().length()/0.2F : 1F));

                return state.setAndContinue(RUN);

            }
            return this.isAttacking() ? state.setAndContinue(WALK) : this.getVelocity().length() > 0.1F ? state.setAndContinue(SPRINT):  state.setAndContinue(WALK_NO_AGGRO);
        }
        else{
            return this.isAttacking() ? state.setAndContinue(IDLE) : state.setAndContinue(IDLE_TRUE);

        }

    }
*/

    @Override
    public void onAttacking(Entity target) {
        target.timeUntilRegen = 0;
        if(target instanceof LivingEntity living){
            living.hurtTime = 0;
        }
        super.onAttacking(target);
    }

/*    @Override
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

    }*/

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
