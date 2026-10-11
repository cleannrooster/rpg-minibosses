package com.cleannrooster.rpg_minibosses.entity;


import com.cleannrooster.rpg_minibosses.magic.AbilityExecutor;
import com.cleannrooster.rpg_minibosses.magic.AbilitySpec;
import com.cleannrooster.rpg_minibosses.magic.AbilityTargets;
import com.cleannrooster.rpg_minibosses.magic.CastTarget;
import com.cleannrooster.rpg_minibosses.magic.Relations;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

/**
 * The Mercenary's thrown trap: lands, arms after two seconds, and goes off under the first thing its
 * owner may harm that comes within two blocks. Expires after eight seconds either way.
 *
 * <p>Transient: it carries a live ability, so it is not saved with the chunk.</p>
 */
public class TrapCleann extends PersistentProjectileEntity {
    private static final int ARM_TICKS = 40;
    private static final int LIFETIME_TICKS = 160;
    private static final float TRIGGER_RADIUS = 2.0f;

    private @Nullable AbilitySpec ability;

    public TrapCleann(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public TrapCleann(EntityType<? extends PersistentProjectileEntity> entityType, Entity owner, World world, AbilitySpec ability) {
        super(entityType, world);
        this.setOwner(owner);
        this.ability = ability;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    protected ItemStack asItemStack() {
        return Items.ARROW.getDefaultStack();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return Items.ARROW.getDefaultStack();
    }

    @Nullable
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return ProjectileUtil.getEntityCollision(this.getWorld(), this, currentPosition, nextPosition, this.getBoundingBox().expand(1.0), this::canHit);
    }

    @Override
    protected boolean canHit(Entity entity) {
        return this.getOwner() instanceof LivingEntity owner && Relations.canHarm(owner, entity) && this.age > ARM_TICKS;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {

    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void tick() {
        if (firstUpdate && !this.getWorld().isClient()) {
            AzCommand.create("drop", "animation.model.new", AzPlayBehaviors.HOLD_ON_LAST_FRAME).sendForEntity(this);
        }
        if (this.age > ARM_TICKS && this.getWorld() instanceof ServerWorld world) {
            if (this.ability == null || !(this.getOwner() instanceof LivingEntity owner)) {
                this.discard();
                return;
            }
            var trigger = new TelegraphArea.Circle(this.getPos(), TRIGGER_RADIUS);
            if (!AbilityTargets.harmable(world, owner, trigger, TRIGGER_RADIUS).isEmpty()) {
                AbilityExecutor.cast(world, owner, this.ability, CastTarget.at(owner, this.getPos()), OptionalDouble.empty());
                this.discard();
                return;
            }
        }
        if (this.age > LIFETIME_TICKS && !this.getWorld().isClient()) {
            this.discard();
        }
        super.tick();
    }
}
