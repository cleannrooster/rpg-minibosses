package com.cleannrooster.rpg_minibosses.magic.entity;

import com.cleannrooster.rpg_minibosses.magic.AbilityExecutor;
import com.cleannrooster.rpg_minibosses.magic.AbilityPresentation;
import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import com.cleannrooster.rpg_minibosses.magic.AbilitySpec;
import com.cleannrooster.rpg_minibosses.magic.CastTarget;
import com.cleannrooster.rpg_minibosses.magic.Delivery;
import com.cleannrooster.rpg_minibosses.magic.ImpactContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The carrier for {@link Delivery.Projectile} and {@link Delivery.Meteor}: flies, optionally homes and
 * falls, and applies its ability to what it hits (and to its burst area where it lands).
 *
 * <p>A plain entity rather than a vanilla projectile, so nothing vanilla does to projectiles (deflection,
 * reflection by maces, wind charges) applies unless an ability asks for it. Transient: it holds the live
 * ability and caster, so it is never saved, and a chunk unloading under one simply ends it. The client
 * only simulates its flight; every hit is decided on the server.</p>
 */
public class BossProjectile extends Entity implements Ownable {
    private @Nullable AbilitySpec ability;
    private @Nullable Delivery.ProjectileSpec spec;
    private @Nullable ImpactContext context;
    private @Nullable Entity homingTarget;
    private boolean meteor;
    private final Set<Entity> struck = new HashSet<>();

    private static final TrackedData<Byte> SCHOOL = DataTracker.registerData(BossProjectile.class, TrackedDataHandlerRegistry.BYTE);

    /** Client-side flight visual, installed by the client; does nothing on a server. */
    public static Consumer<BossProjectile> clientTick = projectile -> {
    };

    public BossProjectile(EntityType<? extends BossProjectile> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    /** Spawn one flying from {@code from} along {@code direction}. */
    public static BossProjectile launch(ServerWorld world, LivingEntity caster, AbilitySpec ability,
                                        Delivery.ProjectileSpec spec, ImpactContext context, Vec3d from,
                                        Vec3d direction, @Nullable Entity homingTarget, boolean meteor) {
        var projectile = new BossProjectile(MagicEntities.PROJECTILE, world);
        projectile.ability = ability;
        projectile.spec = spec;
        projectile.context = context;
        projectile.homingTarget = spec.homingDegrees() > 0.0f ? homingTarget : null;
        projectile.meteor = meteor;
        projectile.getDataTracker().set(SCHOOL, (byte) ability.lookSchool().ordinal());
        projectile.setPosition(from);
        var heading = direction.lengthSquared() < 1.0e-8 ? caster.getRotationVector() : direction.normalize();
        projectile.setVelocity(heading.multiply(spec.speed()));
        ProjectileUtil.setRotationFromVelocity(projectile, 1.0f);
        world.spawnEntity(projectile);
        return projectile;
    }

    @Override
    public @Nullable Entity getOwner() {
        return this.context == null ? null : this.context.caster();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SCHOOL, (byte) 0);
    }

    public MagicSchool school() {
        var schools = MagicSchool.values();
        return schools[Math.floorMod(this.getDataTracker().get(SCHOOL), schools.length)];
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        boolean server = !this.getWorld().isClient();
        if (!server) {
            clientTick.accept(this);
        }
        if (server) {
            if (this.ability == null || this.spec == null || this.context == null || this.age > this.spec.lifetimeTicks()) {
                this.discard();
                return;
            }
            steer();
        }

        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit.getType() != HitResult.Type.MISS && server) {
            if (hit instanceof EntityHitResult entityHit) {
                onEntityHit(entityHit);
            } else if (hit instanceof BlockHitResult blockHit) {
                onBlockHit(blockHit);
            }
            if (this.isRemoved()) {
                return;
            }
        }

        var velocity = this.getVelocity();
        if (this.spec != null && this.spec.gravity() != 0.0) {
            velocity = velocity.add(0.0, -this.spec.gravity(), 0.0);
            this.setVelocity(velocity);
        }
        this.setPosition(this.getPos().add(velocity));
        ProjectileUtil.setRotationFromVelocity(this, 0.5f);
    }

    /** Turn toward the homing target by at most the spec's degrees this tick, keeping speed. */
    private void steer() {
        var target = this.homingTarget;
        if (target == null || !target.isAlive() || this.spec == null || this.age < this.spec.homingDelay()) {
            return;
        }
        var velocity = this.getVelocity();
        double speed = velocity.length();
        if (speed < 1.0e-6) {
            return;
        }
        var current = velocity.multiply(1.0 / speed);
        var wanted = target.getBoundingBox().getCenter().subtract(this.getPos()).normalize();
        double angle = Math.acos(MathHelper.clamp(current.dotProduct(wanted), -1.0, 1.0));
        double max = Math.toRadians(this.spec.homingDegrees());
        Vec3d next;
        if (angle <= max || angle < 1.0e-6) {
            next = wanted;
        } else {
            // Slerp from current toward wanted by max radians.
            double t = max / angle;
            double sinAngle = Math.sin(angle);
            next = current.multiply(Math.sin((1.0 - t) * angle) / sinAngle)
                    .add(wanted.multiply(Math.sin(t * angle) / sinAngle));
        }
        this.setVelocity(next.normalize().multiply(speed));
        this.velocityModified = true;
    }

    private boolean canHit(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity.isSpectator()
                || entity == getOwner() || this.struck.contains(entity)) {
            return false;
        }
        // The client has no ability; its prediction may pass through, the server decides what is struck.
        if (this.ability == null || this.context == null) {
            return false;
        }
        return AbilityExecutor.reaches(this.ability, this.context.caster(), entity);
    }

    private void onEntityHit(EntityHitResult hitResult) {
        if (!(this.getWorld() instanceof ServerWorld world) || this.ability == null || this.spec == null || this.context == null) {
            return;
        }
        if (!(hitResult.getEntity() instanceof LivingEntity target)) {
            return;
        }
        this.struck.add(target);
        AbilityExecutor.applyAll(this.ability, this.context.withOrigin(this.getPos()).withDirect(this), target);
        if (this.struck.size() > this.spec.pierce()) {
            land(world, target);
        } else {
            AbilityPresentation.impact(world, this.ability, this.getPos(), 0.0f, false);
        }
    }

    private void onBlockHit(BlockHitResult hitResult) {
        if (this.getWorld() instanceof ServerWorld world) {
            this.setPosition(hitResult.getPos());
            land(world, null);
        }
    }

    /** It stops here: show the landing, burst if it bursts, and go. */
    private void land(ServerWorld world, @Nullable Entity alreadyStruck) {
        if (this.ability != null && this.spec != null) {
            float radius = this.spec.burst() == null ? 0.0f : (float) this.spec.burst().shape().extent();
            AbilityPresentation.impact(world, this.ability, this.getPos(), radius, this.meteor);
        }
        burst(world, alreadyStruck);
        this.discard();
    }

    private void burst(ServerWorld world, @Nullable Entity alreadyStruck) {
        if (this.spec == null || this.spec.burst() == null || this.ability == null || this.context == null) {
            return;
        }
        AbilityExecutor.applyArea(world, this.ability, this.context.withDirect(this), this.spec.burst(),
                this.getPos(), CastTarget.yawOf(this.getVelocity()), alreadyStruck);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
