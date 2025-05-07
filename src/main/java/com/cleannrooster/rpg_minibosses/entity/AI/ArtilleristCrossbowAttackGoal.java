package com.cleannrooster.rpg_minibosses.entity.AI;

import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
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

public class ArtilleristCrossbowAttackGoal<T extends ArtilleristEntity & RangedAttackMob & CrossbowUser> extends Goal {
    public static final UniformIntProvider COOLDOWN_RANGE = TimeHelper.betweenSeconds(1, 2);
    private final T actor;
    private ArtilleristCrossbowAttackGoal.Stage stage;
    private final double speed;
    private final float squaredRange;
    private int seeingTargetTicker;
    private int chargedTicksLeft;
    private int cooldown;

    public int clip = 7;
    private boolean unload;

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
            CrossbowItem.setCharged(this.actor.getActiveItem(), false);
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
            this.actor.getLookControl().lookAt(livingEntity, 30.0F, 30.0F);
            if (this.stage == ArtilleristCrossbowAttackGoal.Stage.UNCHARGED) {
                if (!bl3) {
                    this.actor.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(this.actor, Items.CROSSBOW));
                    this.stage = ArtilleristCrossbowAttackGoal.Stage.CHARGING;
                    ((CrossbowUser)this.actor).setCharging(true);
                    this.unload = this.actor.getRandom().nextInt(3) == 0;
                    if(unload) {
                        this.actor.resetIndicator();
                    }
                }
            } else if (this.stage == ArtilleristCrossbowAttackGoal.Stage.CHARGING) {
                if (!this.actor.isUsingItem()) {
                    this.stage = ArtilleristCrossbowAttackGoal.Stage.UNCHARGED;
                }

                int i = this.actor.getItemUseTime();
                ItemStack itemStack = this.actor.getActiveItem();
                if (i >= CrossbowItem.getPullTime(itemStack)) {
                    this.actor.stopUsingItem();
                    this.stage = ArtilleristCrossbowAttackGoal.Stage.CHARGED;
                    this.chargedTicksLeft = 10 + this.actor.getRandom().nextInt(10);
                    ((CrossbowUser)this.actor).setCharging(false);
                }
            } else if (this.stage == ArtilleristCrossbowAttackGoal.Stage.CHARGED) {
                --this.chargedTicksLeft;
                if (this.chargedTicksLeft == 0) {
                    this.stage = ArtilleristCrossbowAttackGoal.Stage.READY_TO_ATTACK;
                    this.clip = 7;
                }
            } else if (this.stage == ArtilleristCrossbowAttackGoal.Stage.READY_TO_ATTACK && bl) {
                if(this.actor.age % 20 == 0 || (this.unload && this.actor.age % 2 == 0)) {
                    ((RangedAttackMob) this.actor).attack(livingEntity, 1.0F);
                    ItemStack itemStack2 = this.actor.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this.actor, Items.CROSSBOW));
                    if(this.actor.getRandom().nextInt(13) == 0){
                        loadProjectiles(this.actor, itemStack2);
                        CrossbowItem.setCharged(itemStack2, true);

                        clip = 7;

                        teleportTo(livingEntity);
                    }else {
                        if (this.clip <= 0) {

                            CrossbowItem.setCharged(itemStack2, false);
                            this.stage = ArtilleristCrossbowAttackGoal.Stage.UNCHARGED;
                            this.unload = false;
                        } else {
                            loadProjectiles(this.actor, itemStack2);

                            CrossbowItem.setCharged(itemStack2, true);
                        }
                    }
                    clip--;
                }
            }

        }
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
        int i = EnchantmentHelper.getLevel(Enchantments.MULTISHOT, crossbow);
        int j = i == 0 ? 1 : 3;
        boolean bl = shooter instanceof PlayerEntity && ((PlayerEntity)shooter).getAbilities().creativeMode;
        ItemStack itemStack = shooter.getProjectileType(crossbow);
        ItemStack itemStack2 = itemStack.copy();

        for(int k = 0; k < j; ++k) {
            if (k > 0) {
                itemStack = itemStack2.copy();
            }

            if (itemStack.isEmpty() && bl) {
                itemStack = new ItemStack(Items.ARROW);
                itemStack2 = itemStack.copy();
            }

            if (!loadProjectile(shooter, crossbow, itemStack, k > 0, bl)) {
                return false;
            }
        }

        return true;
    }
    private static boolean loadProjectile(LivingEntity shooter, ItemStack crossbow, ItemStack projectile, boolean simulated, boolean creative) {
        if (projectile.isEmpty()) {
            return false;
        } else {
            boolean bl = creative && projectile.getItem() instanceof ArrowItem;
            ItemStack itemStack;
            if (!bl && !creative && !simulated) {
                itemStack = projectile.split(1);
                if (projectile.isEmpty() && shooter instanceof PlayerEntity) {
                    ((PlayerEntity)shooter).getInventory().removeOne(projectile);
                }
            } else {
                itemStack = projectile.copy();
            }

            putProjectile(crossbow, itemStack);
            return true;
        }
    }
    private static void putProjectile(ItemStack crossbow, ItemStack projectile) {
        NbtCompound nbtCompound = crossbow.getOrCreateNbt();
        NbtList nbtList;
        if (nbtCompound.contains("ChargedProjectiles", 9)) {
            nbtList = nbtCompound.getList("ChargedProjectiles", 10);
        } else {
            nbtList = new NbtList();
        }

        NbtCompound nbtCompound2 = new NbtCompound();
        projectile.writeNbt(nbtCompound2);
        nbtList.add(nbtCompound2);
        nbtCompound.put("ChargedProjectiles", nbtList);
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
