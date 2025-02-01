package com.cleannrooster.rpg_minibosses.entity.AI;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.CrossbowAttackGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ArtilleristCrossbowAttackGoal<T extends HostileEntity & RangedAttackMob & CrossbowUser> extends Goal {
    public static final UniformIntProvider COOLDOWN_RANGE = TimeHelper.betweenSeconds(1, 2);
    public final T actor;
    public ArtilleristCrossbowAttackGoal.Stage stage;
    public final double speed;
    public final float squaredRange;
    public int seeingTargetTicker;
    private int uses = 0;

    public ArtilleristCrossbowAttackGoal(T actor, double speed, float range) {
        this.stage = ArtilleristCrossbowAttackGoal.Stage.UNCHARGED;
        this.actor = actor;
        this.speed = speed;
        this.squaredRange = range * range;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    public boolean canStart() {
        return this.hasAliveTarget() && this.isEntityHoldingCrossbow();
    }

    private boolean isEntityHoldingCrossbow() {
        return this.actor.isHolding(Items.CROSSBOW);
    }

    public boolean shouldContinue() {
        return this.hasAliveTarget() && (this.canStart() || !this.actor.getNavigation().isIdle()) && this.isEntityHoldingCrossbow();
    }

    private boolean hasAliveTarget() {
        return this.actor.getTarget() != null && this.actor.getTarget().isAlive();
    }

    public void stop() {
        super.stop();
        this.actor.setAttacking(false);
        this.actor.setTarget((LivingEntity)null);
        this.seeingTargetTicker = 0;
        if (this.actor.isUsingItem()) {
            this.actor.clearActiveItem();
            ((CrossbowUser)this.actor).setCharging(false);
            this.actor.getActiveItem().set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.DEFAULT);
        }

    }

    public boolean shouldRunEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity livingEntity = this.actor.getTarget();
        if (livingEntity != null) {
            boolean bl = this.actor.getVisibilityCache().canSee(livingEntity);
            boolean bl2 = this.seeingTargetTicker > 0;
            if (bl != bl2) {
                this.seeingTargetTicker = 0;
            }

            if (bl) {
                ++this.seeingTargetTicker;
            } else {
                --this.seeingTargetTicker;
            }

            double d = this.actor.squaredDistanceTo(livingEntity);
            boolean bl3 = (d > (double)this.squaredRange || this.seeingTargetTicker < 5) ;
            if (bl3) {
                float modifier = 1;
                if(this.stage == Stage.CHARGING){
                    modifier *= 0.5F;
                }
                    this.actor.getNavigation().startMovingTo(livingEntity, this.speed*modifier);
            } else {
                this.actor.getNavigation().stop();
                if(livingEntity.distanceTo(this.actor) < 8 && this.stage != Stage.CHARGING){
                    this.actor.getMoveControl().strafeTo(-1,livingEntity.getPos().subtract(this.actor.getPos()).crossProduct(new Vec3d(0,1,0)).dotProduct(this.actor.getRotationVector()) > 0 ? -1 : 1);
                }

            }


            if (this.stage == ArtilleristCrossbowAttackGoal.Stage.UNCHARGED) {
                if (!bl3) {
                    this.actor.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(this.actor, Items.CROSSBOW));

                    this.stage = ArtilleristCrossbowAttackGoal.Stage.CHARGING;
                    ((CrossbowUser)this.actor).setCharging(true);
                }
            } else if (this.stage == ArtilleristCrossbowAttackGoal.Stage.CHARGING) {
                if (!this.actor.isUsingItem()) {
                    this.stage = ArtilleristCrossbowAttackGoal.Stage.UNCHARGED;
                }

                int i = this.actor.getItemUseTime();
                ItemStack itemStack = this.actor.getActiveItem();
                if (i >= CrossbowItem.getPullTime(itemStack, this.actor)) {
                    this.actor.stopUsingItem();
                    this.stage = ArtilleristCrossbowAttackGoal.Stage.CHARGED;
                    ((CrossbowUser)this.actor).setCharging(false);
                }
                if (bl3) {
                    return;
                }
            } else if (this.stage == ArtilleristCrossbowAttackGoal.Stage.CHARGED) {
                    this.stage = ArtilleristCrossbowAttackGoal.Stage.READY_TO_ATTACK;

            }
            Hand hand = ProjectileUtil.getHandPossiblyHolding(this.actor, Items.CROSSBOW);
            ItemStack itemStack = this.actor.getStackInHand(hand);
            if(this.uses >= 6) {
                this.stage = ArtilleristCrossbowAttackGoal.Stage.UNCHARGED;

            }
            if (CrossbowItem.isCharged(itemStack) && bl && this.actor.age % 4 == 0) {

                shoot(livingEntity, 1.6F);

            }
        }
    }
    private  void shoot(LivingEntity entity, float speed) {
        Hand hand = ProjectileUtil.getHandPossiblyHolding(this.actor, Items.CROSSBOW);
        ItemStack itemStack = this.actor.getStackInHand(hand);
        Item var6 = itemStack.getItem();
        if (var6 instanceof CrossbowItem crossbowItem) {
            crossbowItem.shootAll(this.actor.getWorld(), this.actor, hand, itemStack, speed, (float)(14 - this.actor.getWorld().getDifficulty().getId() * 4), entity);

            if(this.uses >= 6) {
                this.stage = ArtilleristCrossbowAttackGoal.Stage.UNCHARGED;

                uses = 0;
            }else   if(this.actor.getRandom().nextInt(13) == 0){
                loadProjectiles(this.actor, itemStack);
                uses = 0;

                teleportTo(entity);
            }
            else {
                loadProjectiles(this.actor, itemStack);

                uses++;
            }
        }

        this.actor.postShoot();
    }
    boolean teleportTo(Entity entity) {
        Vec3d vec3d = new Vec3d(this.actor.getX() - entity.getX(), this.actor.getBodyY(0.5) - entity.getEyeY(), this.actor.getZ() - entity.getZ());
        vec3d = vec3d.normalize();
        double d = 16.0;
        double e = this.actor.getX() + (this.actor.getRandom().nextDouble() - 0.5) * 8.0 - vec3d.x * 16.0;
        double f = this.actor.getY() + (double)(this.actor.getRandom().nextInt(16) - 8) - vec3d.y * 16.0;
        double g = this.actor.getZ() + (this.actor.getRandom().nextDouble() - 0.5) * 8.0 - vec3d.z * 16.0;
        return this.teleportTo(e, f, g);
    }
    private boolean teleportTo(double x, double y, double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

        while(mutable.getY() > this.actor.getWorld().getBottomY() && !this.actor.getWorld().getBlockState(mutable).blocksMovement()) {
            mutable.move(Direction.DOWN);
        }

        BlockState blockState = this.actor.getWorld().getBlockState(mutable);
        boolean bl = blockState.blocksMovement();
        boolean bl2 = blockState.getFluidState().isIn(FluidTags.WATER);
        if (bl && !bl2) {
            Vec3d vec3d = this.actor.getPos();
            boolean bl3 = this.actor.teleport(x, y, z, true);
            if (bl3) {
                this.actor.getWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(this.actor));
                if (!this.actor.isSilent()) {
                    this.actor.getWorld().playSound((PlayerEntity)null, this.actor.prevX, this.actor.prevY, this.actor.prevZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.actor.getSoundCategory(), 1.0F, 1.0F);
                    this.actor.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
            }

            return bl3;
        } else {
            return false;
        }
    }


    private static boolean loadProjectiles(LivingEntity shooter, ItemStack crossbow) {
        List<ItemStack> list = load(crossbow, shooter.getProjectileType(crossbow), shooter);
        if (!list.isEmpty()) {
            crossbow.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(list));
            return true;
        } else {
            return false;
        }
    }
    protected static List<ItemStack> load(ItemStack stack, ItemStack projectileStack, LivingEntity shooter) {
        if (projectileStack.isEmpty()) {
            return List.of();
        } else {
            World var5 = shooter.getWorld();
            int var10000;
            if (var5 instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld)var5;
                var10000 = EnchantmentHelper.getProjectileCount(serverWorld, stack, shooter, 1);
            } else {
                var10000 = 1;
            }

            int i = var10000;
            List<ItemStack> list = new ArrayList(i);
            ItemStack itemStack = projectileStack.copy();

            for(int j = 0; j < i; ++j) {
                ItemStack itemStack2 = getProjectile(stack, j == 0 ? projectileStack : itemStack, shooter, j > 0);
                if (!itemStack2.isEmpty()) {
                    list.add(itemStack2);
                }
            }

            return list;
        }
    }
    protected static ItemStack getProjectile(ItemStack stack, ItemStack projectileStack, LivingEntity shooter, boolean multishot) {
        int var10000;
        label28: {
            if (!multishot && !shooter.isInCreativeMode()) {
                World var6 = shooter.getWorld();
                if (var6 instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld)var6;
                    var10000 = EnchantmentHelper.getAmmoUse(serverWorld, stack, projectileStack, 1);
                    break label28;
                }
            }

            var10000 = 0;
        }

        int i = var10000;
        if (i > projectileStack.getCount()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemStack;
            if (i == 0) {
                itemStack = projectileStack.copyWithCount(1);
                itemStack.set(DataComponentTypes.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
                return itemStack;
            } else {
                itemStack = projectileStack.split(i);
                if (projectileStack.isEmpty() && shooter instanceof PlayerEntity) {
                    PlayerEntity playerEntity = (PlayerEntity)shooter;
                    playerEntity.getInventory().removeOne(projectileStack);
                }

                return itemStack;
            }
        }
    }
    private boolean isUncharged() {
        return this.stage == ArtilleristCrossbowAttackGoal.Stage.UNCHARGED;
    }

    static enum Stage {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK;

        private Stage() {
        }
    }
}
