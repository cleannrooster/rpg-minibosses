package com.cleannrooster.anarchy_rpg.entity;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.magic.fx.FxEmitter;
import dev.eann.atlasvfx.cue.FxCues;
import com.cleannrooster.anarchy_rpg.magic.fx.CueArgs;
import com.cleannrooster.anarchy_rpg.magic.MagicSchool;
import com.cleannrooster.anarchy_rpg.magic.TickScheduler;
import com.cleannrooster.anarchy_rpg.client.entity.effect.Effects;
import com.cleannrooster.anarchy_rpg.entity.brain.impl.RogueBrain;
import com.cleannrooster.anarchy_rpg.entity.brain.impl.TemplarBrain;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class TemplarEntity extends MinibossEntity{
    List<Item> bonusList = List.of();

    protected TemplarEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        super.bonusList = Registries.ITEM.stream().filter(item -> {
            return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_5_weapons"))))
                            && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                            new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                    ;
        }).toList();
    }
    protected TemplarEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser) {
        super(entityType, world);

        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);
        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_3_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_4_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_5_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
        }

    }
    @Override
    public ItemStack getMainWeapon() {
        return Items.DIAMOND_SWORD.getDefaultStack();
    }
    protected TemplarEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser,float spawnCoeff) {
        super(entityType, world,spawnCoeff);

        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);
        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_3_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_4_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_5_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
        }

    }
    public boolean skipMainHand(){
        return false;
    }


/*
    public static final RawAnimation STAFF = RawAnimation.begin().thenPlay("animation.valkyrie.staff");
    public static final RawAnimation DASHRIGHT = RawAnimation.begin().thenPlay("animation.valkyrie.dashright");
    public static final RawAnimation DASHLEFT = RawAnimation.begin().thenPlay("animation.valkyrie.dashleft");
    public static final RawAnimation DASH_ATTACK = RawAnimation.begin().thenPlay("animation.valkyrie.dash_attack");

    public static final RawAnimation TWIRL = RawAnimation.begin().thenPlay("animation.valkyrie.twirl");
    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("animation.valkyrie.idle");
    public static final RawAnimation SWING1 = RawAnimation.begin().then("animation.mob.swing1_2h", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation SWING2 = RawAnimation.begin().then("animation.mob.swing2_2h", Animation.LoopType.PLAY_ONCE);
*/

    @Override
    public void registerAttackPrototypes() {
        // Discarded immediately; building it is what registers this mob's attack geometry
        // with AttackRegistry, which the client needs to draw incoming swings.
        new TemplarBrain(this);
    }

    @Override
    protected void initCustomGoals() {
        this.brain = new TemplarBrain(this);

        this.goalSelector.add(2, new com.cleannrooster.anarchy_rpg.entity.brain.MobBrainGoal(this, brain));
        super.initCustomGoals();
    }
    @Override
    public boolean isTwoHand() {
        return true;
    }


/*    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        super.registerControllers(animationData);




        animationData.add(
                new AnimationController<MinibossEntity>(this, "actions",
                        1, this::predicateTemplar)
                        .triggerableAnim("staff", STAFF).triggerableAnim("swing1", SWING1).triggerableAnim("swing2",SWING2)  );




    }*/



    public void applyIntroEffect(){
        super.applyIntroEffect();
    }
    public RegistryEntry<StatusEffect> getIntroEffect(){
        return Effects.PETRIFIED.registryEntry;
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
    @Override
    protected void mobTick() {
        super.mobTick();
    }

    @Override
    public float getMovementSpeed() {
        return super.getMovementSpeed();
    }

    private static BlockHitResult raycastObstacle(Entity entity, Vec3d start, Vec3d end) {
        return entity.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
    }

    public static boolean raycastObstacleFree(Entity entity, Vec3d start, Vec3d end) {
        BlockHitResult hit = raycastObstacle(entity, start, end);
        return hit.getType() != HitResult.Type.BLOCK;
    }
 /*   private PlayState predicateTemplar(AnimationState<MinibossEntity> state) {

        if(this.isAttacking() && !this.getDataTracker().get(DOWN)){
            if(this.getVelocity().length() > 0.2F){
                state.setAnimation(SPRINT_AGGRO);
                state.setControllerSpeed(this.getDataTracker().get(DOWN) ? 1F : (float) (state.isMoving() ? this.getVelocity().length()/0.2F : 1F));

                return PlayState.CONTINUE;

            }
            state.setControllerSpeed(this.getDataTracker().get(DOWN) ? 1F : (float) (state.isMoving() ? this.getVelocity().length()/0.1F : 1F));

            state.setAnimation(AGGRO_TEMPLAR);

            return PlayState.CONTINUE;
        }
        return PlayState.STOP;

    }*/
/*
    public static final RawAnimation IDLE_AGGRO = RawAnimation.begin().thenPlay("animation.unknown.idle");
    public static final RawAnimation SPRINT_AGGRO = RawAnimation.begin().thenPlay("animation.templar.heavy_run");

    public static final RawAnimation AGGRO_TEMPLAR = RawAnimation.begin().thenPlay("animation.mob.walk_templar");
*/

    public boolean   swingBool;
    protected Box getAttackBox() {
        Entity entity = this.getVehicle();
        Box box3;
        if (entity != null) {
            Box box = entity.getBoundingBox();
            Box box2 = this.getBoundingBox();
            box3 = new Box(Math.min(box2.minX, box.minX), box2.minY, Math.min(box2.minZ, box.minZ), Math.max(box2.maxX, box.maxX), box2.maxY, Math.max(box2.maxZ, box.maxZ));
        } else {
            box3 = this.getBoundingBox();
        }

        return box3.expand(3.5F, (double)0.0F, 3.5F);
    }
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!performing
                && source.getAttacker() != null
                && source.getAttacker() instanceof LivingEntity
                && source.isDirect()
                && source.getAttacker().distanceTo(this) < 5.5F
                && brain instanceof com.cleannrooster.anarchy_rpg.entity.brain.impl.TemplarBrain templarbrain
                && templarbrain.tryParry()) {
            this.tryAttack(source.getAttacker());
            amount *= 0.5F;
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_ANVIL_PLACE, this.getSoundCategory(), 1.0f, 1.0f);
        }
        return super.damage(source, amount);
    }

    /**
     * Contact damage.
     *
     * <p>While a combat action is running it owns the swing's presentation — its own clip is already
     * playing and its own slash particles have already been emitted — so this only adds the legacy
     * flourish for contacts that happen outside the action system. It must still deal the damage in
     * both cases: the brain's melee helper routes through here precisely so vanilla knockback, enchantment
     * effects and attack events all fire normally.
     */
    public boolean tryAttack(Entity target) {
        if (!performing) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                // The flourish for contacts outside the action system: a short blade trail across the
                // Templar's front, alternating sides with the swing animation.
                var facing = Vec3d.fromPolar(0.0f, this.getYaw());
                FxEmitter.emit(serverWorld, FxCues.SLASH, CueArgs.of(MagicSchool.GENERIC, this.getEyePos(), facing,
                        5.5f * this.getWidth(), 80.0f, swingBool ? 1 : 0, serverWorld.random.nextLong(), -1));
            }
            if (swingBool) {
                dispatcher.setSwing();
                swingBool = false;
            } else {
                dispatcher.setSwing2();
                swingBool = true;
            }
        }
        return super.tryAttack(target);
    }


}
