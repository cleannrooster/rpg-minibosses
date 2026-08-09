package com.cleannrooster.rpg_minibosses.entity.combat;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Authored grounded combat movement for the minibosses.
 *
 * <p>This is the piece that replaces "start a path to the player's feet and hope". Inside the combat
 * envelope the mob is steered directly: a brain states an <em>intent</em> — press, orbit, retreat, cross,
 * stop — and this class turns it into a velocity with a deliberate acceleration curve, a hard arrest, and
 * a capped turn rate. Vanilla path navigation is still used, but only for what it is good at: long
 * approaches and getting around geometry, either because the brain asked for it or because direct
 * steering stopped making progress.
 *
 * <p><b>Ownership.</b> At any moment exactly one {@link MovementOwner} drives horizontal motion. When the
 * owner is not {@link MovementOwner#NAVIGATION} the path is stopped every tick, so navigation can never
 * drag a mob through its own attack. When an attack finishes, ownership always returns — see
 * {@link #releaseAttackMotion()}, which the runner calls unconditionally on finish <em>and</em> on cancel.
 *
 * <p><b>What is preserved.</b> Only the horizontal components are written. Vertical velocity is left to
 * vanilla, so gravity, terrain collision, step height and block interaction all behave normally. A
 * committed motion may set an initial vertical impulse (a leap), after which vanilla owns Y again.
 *
 * <p><b>Order of operations.</b> A brain calls {@link #beginTick()}, issues at most one intent, then calls
 * {@link #endTick()}. Intents are requests, not commands: a running committed motion outranks them, and if
 * no intent is issued at all the mob arrests rather than coasting.
 */
public final class GroundLocomotion {

    /** Below this, a horizontal velocity is treated as a standstill. */
    private static final double REST_EPSILON = 1.0e-4;
    /** Ticks of near-zero displacement, while steering hard, before we hand over to navigation. */
    private static final int STALL_TICKS = 6;
    /** Squared displacement per tick under which a tick counts as "made no progress". */
    private static final double STALL_DISTANCE_SQ = 0.0016;
    /** How long a navigation fallback lasts before direct steering is tried again. */
    private static final int FALLBACK_TICKS = 30;

    private final MinibossEntity entity;

    private MovementOwner owner = MovementOwner.NAVIGATION;
    /** Our own integrator. Deliberately independent of entity velocity, which vanilla decays by friction. */
    private Vec3d steering = Vec3d.ZERO;
    /** Velocity a running attack wants to contribute this tick. */
    private Vec3d attackMotion = Vec3d.ZERO;
    private boolean intentThisTick;
    private MovementProfile lastProfile = MovementProfile.HARD_STOP;

    // Committed motion.
    private Vec3d committedVelocity = Vec3d.ZERO;
    private int committedTicks;
    private boolean committedAirborne;
    /** Ticks a committed airborne motion has been extended waiting to land. */
    private int committedExtension;
    /** Fraction of the committed heading that may still be re-aimed per tick, 0 = none. */
    private double committedCorrection;
    private @Nullable Vec3d committedAim;

    // Stall detection / navigation fallback.
    private Vec3d previousPosition = Vec3d.ZERO;
    private int stalledTicks;
    private int fallbackTicks;
    private @Nullable Vec3d fallbackGoal;
    private double fallbackSpeed = 1.0;

    // External impulses (knockback) are allowed to survive for a few ticks.
    private int impulseTicks;

    public GroundLocomotion(MinibossEntity entity) {
        this.entity = entity;
        this.previousPosition = entity.getPos();
    }

    // --- frame ------------------------------------------------------------------------------------

    /** Call once at the top of the brain's tick, before any intent is issued. */
    public void beginTick() {
        this.attackMotion = Vec3d.ZERO;
        this.intentThisTick = false;

        if (this.impulseTicks > 0) {
            this.impulseTicks--;
        }
        if (this.fallbackTicks > 0) {
            this.fallbackTicks--;
        }
        trackProgress();
    }

    /**
     * Call once at the bottom of the brain's tick. Resolves committed motion, writes the horizontal
     * velocity, and arrests if the brain asked for nothing — a mob that has not decided to move should be
     * standing still, not coasting.
     */
    public void endTick() {
        if (this.committedTicks > 0) {
            tickCommitted();
            return;
        }
        if (this.owner == MovementOwner.NAVIGATION) {
            // Not our movement. Keep the integrator in step with reality so a later hand-over to direct
            // steering starts from the speed the mob actually has rather than from a stale value.
            this.steering = horizontal(this.entity.getVelocity());
            return;
        }
        if (!this.intentThisTick) {
            driveTo(Vec3d.ZERO, MovementProfile.HARD_STOP);
        }
        writeVelocity();
    }

    // --- intents ----------------------------------------------------------------------------------

    /**
     * Close on the target and keep closing. Inside {@code preferredRange} the mob eases off but never
     * fully stops, and at contact range it slides around the target instead of grinding into the hitbox —
     * which is what stops a body-blocking player from pinning it in place.
     */
    public void pressure(LivingEntity target, double preferredRange, MovementProfile profile) {
        var toTarget = horizontal(target.getPos().subtract(this.entity.getPos()));
        var distance = toTarget.length();
        var forward = distance < REST_EPSILON ? facing() : toTarget.normalize();

        double speed;
        var lateral = Vec3d.ZERO;
        var contact = Math.max(1.2, preferredRange * 0.45);
        if (distance > preferredRange * 2.0) {
            speed = profile.maxSpeed();
        } else if (distance > preferredRange) {
            speed = profile.maxSpeed() * 0.85;
        } else if (distance > contact) {
            speed = profile.maxSpeed() * 0.35;
        } else {
            // Contact range: stop pushing forward, glide around them.
            speed = profile.maxSpeed() * 0.05;
            var side = new Vec3d(-forward.z, 0.0, forward.x);
            lateral = side.multiply(slipDirection(target) * profile.maxSpeed() * 0.45 * profile.lateralAuthority());
        }

        request(forward.multiply(speed).add(lateral), profile);
    }

    /**
     * Circle the target at a radius. The radial correction is what keeps the ring honest; the tangential
     * component is the actual orbit. {@code direction} is +1 or -1.
     */
    public void orbit(LivingEntity target, double radius, int direction, MovementProfile profile) {
        var fromTarget = horizontal(this.entity.getPos().subtract(target.getPos()));
        var distance = fromTarget.length();
        var outward = distance < REST_EPSILON ? facing().multiply(-1.0) : fromTarget.normalize();
        var tangent = new Vec3d(-outward.z, 0.0, outward.x).multiply(direction);

        // Radial error drives a correction that saturates, so a mob far off the ring closes it at a
        // sensible pace instead of sprinting straight in.
        var radialError = MathHelper.clamp((radius - distance) / Math.max(1.0, radius * 0.5), -1.0, 1.0);
        var radial = outward.multiply(radialError * profile.maxSpeed() * 0.8);
        var along = tangent.multiply(profile.maxSpeed() * profile.lateralAuthority());
        request(clampLength(along.add(radial), profile.maxSpeed()), profile);
    }

    /** Head for a world position. Used for firing positions and ambush lanes. */
    public void seekPosition(Vec3d position, MovementProfile profile) {
        var toGoal = horizontal(position.subtract(this.entity.getPos()));
        var distance = toGoal.length();
        if (distance < 0.25) {
            arrest(profile);
            return;
        }
        // Ease down over the last block or so, so arrival is a stop rather than an overshoot.
        var speed = profile.maxSpeed() * MathHelper.clamp(distance / 1.5, 0.2, 1.0);
        this.fallbackGoal = position;
        this.fallbackSpeed = 1.2;
        request(toGoal.normalize().multiply(speed), profile);
    }

    /** Open distance. Prefers a diagonal over a straight backpedal — see {@link #retreatVector}. */
    public void retreatFrom(LivingEntity target, double preferredDistance, MovementProfile profile) {
        var distance = horizontal(target.getPos().subtract(this.entity.getPos())).length();
        if (distance >= preferredDistance) {
            arrest(profile);
            return;
        }
        var away = retreatVector(target);
        var urgency = MathHelper.clamp((preferredDistance - distance) / preferredDistance, 0.25, 1.0);
        request(away.multiply(profile.maxSpeed() * urgency), profile);
    }

    /**
     * Move along a line that passes the target's shoulder rather than at the target.
     *
     * <p>{@code side} is <b>+1 for the mob's own right, -1 for its left</b> — the same convention as
     * {@link #crossVector} and {@link #slipDirection}. Attacks that pair a crossing clip with a committed
     * heading must use it, or the body travels one way while the animation reads the other.
     *
     * <p>This is the steering half of a crossing attack: it exists so the mob's exit is already chosen
     * before the swing lands.
     */
    public void crossPast(LivingEntity target, int side, MovementProfile profile) {
        request(crossVector(target, side).multiply(profile.maxSpeed()), profile);
    }

    /** Stop, decisively. Under the profile's snap threshold this is an exact stop, not an approach. */
    public void arrest(MovementProfile profile) {
        request(Vec3d.ZERO, profile);
    }

    /** Hand movement back to vanilla path navigation for macro traversal. */
    public void navigateTo(double x, double y, double z, double speed) {
        this.intentThisTick = true;
        if (this.owner == MovementOwner.ATTACK_MOTION) {
            return; // an attack outranks this
        }
        this.owner = MovementOwner.NAVIGATION;
        this.entity.getNavigation().startMovingTo(x, y, z, speed);
    }

    private void request(Vec3d desired, MovementProfile profile) {
        this.intentThisTick = true;
        this.lastProfile = profile;
        if (this.owner == MovementOwner.ATTACK_MOTION || this.committedTicks > 0) {
            return; // the attack owns the body; the request is simply dropped
        }
        if (this.fallbackTicks > 0 && this.fallbackGoal != null) {
            // Direct steering was not making progress. Let navigation solve the geometry for a while.
            this.owner = MovementOwner.NAVIGATION;
            this.entity.getNavigation().startMovingTo(
                    this.fallbackGoal.x, this.fallbackGoal.y, this.fallbackGoal.z, this.fallbackSpeed);
            return;
        }
        takeSteering();
        driveTo(clampLength(desired, profile.maxSpeed()), profile);
    }

    // --- committed motion -------------------------------------------------------------------------

    /**
     * Throw the body along a heading for a fixed number of ticks. Ordinary steering is suppressed for the
     * duration and the heading is captured now — the player can dodge after commitment, which is the
     * entire point.
     *
     * @param direction        horizontal heading; normalised here
     * @param speed            blocks/tick along that heading
     * @param ticks            how long the motion runs
     * @param verticalImpulse  one-off Y velocity at launch (a leap); 0 keeps the mob grounded
     * @param lateCorrection   0 for no mid-flight correction, up to ~0.1 for a slight lean toward the aim
     */
    public void beginCommittedMotion(Vec3d direction, double speed, int ticks,
                                     double verticalImpulse, double lateCorrection) {
        var heading = horizontal(direction);
        if (heading.lengthSquared() < REST_EPSILON) {
            heading = facing();
        }
        heading = heading.normalize();

        this.owner = MovementOwner.ATTACK_MOTION;
        this.entity.getNavigation().stop();
        this.committedVelocity = heading.multiply(speed);
        this.committedTicks = Math.max(1, ticks);
        this.committedAirborne = verticalImpulse > 0.0;
        this.committedCorrection = MathHelper.clamp(lateCorrection, 0.0, 0.25);
        this.committedAim = null;
        this.committedExtension = 0;
        this.steering = this.committedVelocity;
        this.intentThisTick = true;
        this.stalledTicks = 0;

        this.entity.setVelocity(this.committedVelocity.x,
                verticalImpulse > 0.0 ? verticalImpulse : this.entity.getVelocity().y,
                this.committedVelocity.z);
        if (verticalImpulse > 0.0) {
            this.entity.setOnGround(false);
            this.entity.velocityDirty = true;
        }
    }

    /** Optional aim for the small mid-flight correction a committed motion is allowed. */
    public void setCommittedAim(@Nullable Vec3d aim) {
        this.committedAim = aim == null ? null : horizontal(aim);
    }

    public boolean isCommitted() {
        return this.committedTicks > 0;
    }

    /** Cut a committed motion short — an obstruction, a stagger, a phase change. */
    public void cancelCommitted() {
        this.committedTicks = 0;
        this.committedAirborne = false;
        this.committedExtension = 0;
        this.committedAim = null;
        this.steering = Vec3d.ZERO;
        if (this.owner == MovementOwner.ATTACK_MOTION) {
            this.owner = MovementOwner.COMBAT_STEERING;
        }
        this.entity.setVelocity(0.0, this.entity.getVelocity().y, 0.0);
    }

    private void tickCommitted() {
        this.committedTicks--;

        if (this.committedCorrection > 0.0 && this.committedAim != null) {
            var toAim = this.committedAim.subtract(this.entity.getPos());
            toAim = horizontal(toAim);
            if (toAim.lengthSquared() > REST_EPSILON) {
                var speed = this.committedVelocity.length();
                var blended = this.committedVelocity.normalize()
                        .multiply(1.0 - this.committedCorrection)
                        .add(toAim.normalize().multiply(this.committedCorrection));
                if (blended.lengthSquared() > REST_EPSILON) {
                    this.committedVelocity = blended.normalize().multiply(speed);
                }
            }
        }

        // A committed motion that has stopped making progress has hit something. Ending it here is what
        // keeps a leap or a dash from grinding a mob into a wall for its full duration.
        if (this.stalledTicks >= 3 && !this.committedAirborne) {
            cancelCommitted();
            return;
        }

        this.steering = this.committedVelocity;
        this.entity.getNavigation().stop();
        this.entity.setVelocity(this.committedVelocity.x, this.entity.getVelocity().y,
                this.committedVelocity.z);
        this.entity.velocityDirty = true;

        if (this.committedTicks <= 0) {
            // An airborne motion is not over until the mob is back on the ground. Cutting the horizontal
            // velocity in mid-air would drop it straight down out of its own leap, which reads as the arc
            // being interrupted rather than completed. The extension is capped so an obstructed jump still
            // recovers instead of hanging.
            if (this.committedAirborne && !this.entity.isOnGround() && this.committedExtension < 20) {
                this.committedExtension++;
                this.committedTicks = 1;
                return;
            }
            // Hard arrest on landing/expiry. No coast, no settle.
            this.committedAirborne = false;
            this.committedExtension = 0;
            this.committedAim = null;
            this.steering = Vec3d.ZERO;
            this.entity.setVelocity(0.0, this.entity.getVelocity().y, 0.0);
            if (this.owner == MovementOwner.ATTACK_MOTION) {
                this.owner = MovementOwner.COMBAT_STEERING;
            }
        }
    }

    // --- attack ownership -------------------------------------------------------------------------

    /** Claim ownership for a running attack. Idempotent; called every tick by the runner. */
    public void claimAttackMotion() {
        this.owner = MovementOwner.ATTACK_MOTION;
        this.entity.getNavigation().stop();
    }

    /**
     * Give movement back to vanilla navigation entirely. Called when a mob leaves combat, so it does not
     * keep holding a combat facing it has no reason to hold.
     */
    public void releaseToNavigation() {
        this.committedTicks = 0;
        this.committedAirborne = false;
        this.committedExtension = 0;
        this.committedAim = null;
        this.owner = MovementOwner.NAVIGATION;
        this.steering = Vec3d.ZERO;
        this.fallbackTicks = 0;
        this.fallbackGoal = null;
        this.stalledTicks = 0;
    }

    /**
     * Hand movement back after an attack. Called unconditionally when an execution finishes or is
     * cancelled, so an attack can never strand a mob in {@link MovementOwner#ATTACK_MOTION}.
     */
    public void releaseAttackMotion() {
        this.committedTicks = 0;
        this.committedAirborne = false;
        this.committedExtension = 0;
        this.committedAim = null;
        if (this.owner == MovementOwner.ATTACK_MOTION) {
            this.owner = MovementOwner.COMBAT_STEERING;
        }
    }

    /**
     * Velocity a running attack contributes this tick — the step inside a heavy swing, the press inside a
     * Templar sweep. Folded into the steering rather than fighting it.
     */
    public void applyAttackMotion(Vec3d motion) {
        this.attackMotion = this.attackMotion.add(horizontal(motion));
        this.intentThisTick = true;
        if (this.committedTicks <= 0) {
            // Attack-owned steps ride on top of a body that is otherwise being brought to rest.
            driveTo(Vec3d.ZERO, MovementProfile.HARD_STOP);
        }
    }

    // --- core curve -------------------------------------------------------------------------------

    /**
     * Drive the steering velocity toward {@code desired}.
     *
     * <p>Three departures from a plain exponential blend, each aimed at one way a deliberate mob can
     * accidentally read as mushy: asymmetric rates (accelerate slowly, stop quickly), a launch floor so
     * the first ticks of a burst have real authority, and a snap threshold so a stop is an actual stop
     * rather than an infinitely long tail of drift.
     */
    private void driveTo(Vec3d desired, MovementProfile profile) {
        var delta = desired.subtract(this.steering);
        var gap = delta.length();
        if (gap < 1.0e-6) {
            this.steering = desired;
            return;
        }
        var slowing = desired.lengthSquared() < this.steering.lengthSquared();
        if (slowing) {
            if (gap < profile.snap()) {
                this.steering = desired;
                return;
            }
            this.steering = this.steering.add(delta.multiply(profile.arrest()));
            return;
        }
        var step = Math.max(gap * profile.accel(), Math.min(gap, profile.launchFloor()));
        this.steering = this.steering.add(delta.multiply(step / gap));
    }

    private void writeVelocity() {
        // A recent knockback is allowed to land. Overwriting it every tick would make these mobs immovable
        // in a way the player would read as a bug rather than as weight.
        if (this.impulseTicks > 0) {
            this.steering = horizontal(this.entity.getVelocity());
            return;
        }
        var combined = this.steering.add(this.attackMotion);
        this.entity.setVelocity(combined.x, this.entity.getVelocity().y, combined.z);
        this.entity.velocityDirty = true;
    }

    private void takeSteering() {
        if (this.owner != MovementOwner.COMBAT_STEERING) {
            this.owner = MovementOwner.COMBAT_STEERING;
        }
        if (!this.entity.getNavigation().isIdle()) {
            this.entity.getNavigation().stop();
        }
    }

    // --- stall / fallback -------------------------------------------------------------------------

    private void trackProgress() {
        var position = this.entity.getPos();
        var travelled = position.squaredDistanceTo(this.previousPosition);
        this.previousPosition = position;

        var wantsToMove = this.steering.lengthSquared() > 0.004; // ~0.06 blocks/tick
        if (this.owner == MovementOwner.COMBAT_STEERING && wantsToMove && travelled < STALL_DISTANCE_SQ) {
            this.stalledTicks++;
            if (this.stalledTicks >= STALL_TICKS && this.fallbackGoal != null) {
                // Direct steering is driving into something. Rather than keep pushing, let the pathfinder
                // solve it — and come back to authored steering once it has.
                this.fallbackTicks = FALLBACK_TICKS;
                this.stalledTicks = 0;
                this.steering = Vec3d.ZERO;
            }
        } else if (this.committedTicks > 0 && travelled < STALL_DISTANCE_SQ) {
            this.stalledTicks++;
        } else {
            this.stalledTicks = 0;
        }
    }

    /**
     * The position direct steering should fall back to pathing toward when it gets stuck. Brains set this
     * whenever their intent has an obvious destination.
     */
    public void setFallbackGoal(@Nullable Vec3d goal, double speed) {
        this.fallbackGoal = goal;
        this.fallbackSpeed = speed;
    }

    /** Told about an external shove so steering yields for a moment instead of erasing it. */
    public void notifyExternalImpulse(int ticks) {
        this.impulseTicks = Math.max(this.impulseTicks, ticks);
    }

    // --- facing -----------------------------------------------------------------------------------

    /** Turn the body toward the target, capped per tick. Yaw is the authority; head and body follow it. */
    public void faceTarget(LivingEntity target, float maxDegrees) {
        facePosition(target.getX(), target.getZ(), maxDegrees);
    }

    public void facePosition(double x, double z, float maxDegrees) {
        if (maxDegrees <= 0.0f) {
            return;
        }
        var dx = x - this.entity.getX();
        var dz = z - this.entity.getZ();
        if (Math.abs(dx) < 1.0e-6 && Math.abs(dz) < 1.0e-6) {
            return;
        }
        var desired = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875) - 90.0f;
        var step = MathHelper.clamp(MathHelper.wrapDegrees(desired - this.entity.getYaw()),
                -maxDegrees, maxDegrees);
        var yaw = MathHelper.wrapDegrees(this.entity.getYaw() + step);
        this.entity.setYaw(yaw);
        this.entity.setBodyYaw(yaw);
        this.entity.setHeadYaw(yaw);
    }

    /** Snap facing to a heading outright — used at the moment a committed motion captures its direction. */
    public void snapFacing(Vec3d direction) {
        var heading = horizontal(direction);
        if (heading.lengthSquared() < REST_EPSILON) {
            return;
        }
        var yaw = (float) (MathHelper.atan2(heading.z, heading.x) * 57.2957763671875) - 90.0f;
        this.entity.setYaw(yaw);
        this.entity.setBodyYaw(yaw);
        this.entity.setHeadYaw(yaw);
    }

    // --- queries ----------------------------------------------------------------------------------

    public MovementOwner owner() {
        return this.owner;
    }

    public Vec3d steering() {
        return this.steering;
    }

    public double speed() {
        return this.steering.length();
    }

    public MovementProfile lastProfile() {
        return this.lastProfile;
    }

    /** Facing as a horizontal unit vector. */
    public Vec3d facing() {
        var look = horizontal(this.entity.getRotationVector());
        return look.lengthSquared() < REST_EPSILON ? new Vec3d(0.0, 0.0, 1.0) : look.normalize();
    }

    /**
     * A retreat that reads as a decision rather than a panic. Straight-back retreats look like fleeing and
     * are trivially chased; angling the exit relative to the target's own facing makes it a disengage.
     */
    public Vec3d retreatVector(LivingEntity target) {
        var away = horizontal(this.entity.getPos().subtract(target.getPos()));
        if (away.lengthSquared() < REST_EPSILON) {
            away = facing().multiply(-1.0);
        }
        away = away.normalize();
        var side = new Vec3d(-away.z, 0.0, away.x).multiply(slipDirection(target));
        return away.multiply(0.78).add(side.multiply(0.62)).normalize();
    }

    /**
     * A heading that passes the target's shoulder instead of ending at their hitbox.
     *
     * @param side +1 for the mob's own right, -1 for its left
     */
    public Vec3d crossVector(LivingEntity target, int side) {
        var toTarget = horizontal(target.getPos().subtract(this.entity.getPos()));
        if (toTarget.lengthSquared() < REST_EPSILON) {
            return facing();
        }
        var forward = toTarget.normalize();
        var lateral = new Vec3d(-forward.z, 0.0, forward.x).multiply(side);
        return forward.multiply(0.82).add(lateral.multiply(0.57)).normalize();
    }

    /**
     * Which way to slide around a target: +1 for the mob's own right, -1 for its left.
     *
     * <p>Chosen from the side the mob is already drifting toward so the curve continues its motion instead
     * of reversing it, with a stable tiebreak so it does not flip every tick when the drift is near zero.
     */
    public int slipDirection(LivingEntity target) {
        var toTarget = horizontal(target.getPos().subtract(this.entity.getPos()));
        if (toTarget.lengthSquared() < REST_EPSILON) {
            return 1;
        }
        var forward = toTarget.normalize();
        var side = new Vec3d(-forward.z, 0.0, forward.x);
        var drift = this.steering.dotProduct(side);
        if (Math.abs(drift) > 0.015) {
            return drift > 0 ? 1 : -1;
        }
        return (this.entity.getId() & 1) == 0 ? 1 : -1;
    }

    /** Signed angle, in degrees, between facing and the target. */
    public float angleTo(LivingEntity target) {
        var dx = target.getX() - this.entity.getX();
        var dz = target.getZ() - this.entity.getZ();
        var desired = (float) (MathHelper.atan2(dz, dx) * 57.2957763671875) - 90.0f;
        return Math.abs(MathHelper.wrapDegrees(desired - this.entity.getYaw()));
    }

    // --- helpers ----------------------------------------------------------------------------------

    public static Vec3d horizontal(Vec3d vector) {
        return new Vec3d(vector.x, 0.0, vector.z);
    }

    public static Vec3d clampLength(Vec3d vector, double max) {
        var lengthSq = vector.lengthSquared();
        return lengthSq > max * max && lengthSq > 0.0
                ? vector.multiply(max / Math.sqrt(lengthSq))
                : vector;
    }
}
