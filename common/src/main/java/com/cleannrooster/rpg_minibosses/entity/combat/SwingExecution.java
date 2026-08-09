package com.cleannrooster.rpg_minibosses.entity.combat;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackFrame;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackGeometry;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackVolume;
import com.cleannrooster.rpg_minibosses.entity.combat.net.AttackNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * One running swing: the geometry half of an executing {@link CombatAction}.
 *
 * <p>This is where the arc system earns its keep. Damage and the visible slash are resolved from the same
 * swept surface on the same tick — {@link AttackGeometry#hits} decides who is inside the slice covered
 * since last tick, and {@link AttackGeometry#sample} draws the particles on that identical slice. There is
 * no separate hitbox to keep in step with a separate effect, so a player who sidesteps what they saw has
 * sidestepped what actually existed.
 *
 * <p>What this replaced was a cone test around {@code getRotationVector()} plus an unrelated particle
 * flourish: the cone had no direction of travel, so a right-to-left clip and a left-to-right clip damaged
 * identically, and the particles were drawn from a hand-rolled parametric curve that matched neither.
 */
public final class SwingExecution {

    private final MinibossEntity attacker;
    private final CombatAction action;
    private final AttackVolume volume;

    private AttackFrame frame;
    private @Nullable Vec3d previousOrigin;
    private float previousProgress;
    /** Victims already struck this swing, mapped to the active tick they were last struck on. */
    private final Map<Entity, Integer> hits = new HashMap<>();
    private boolean weaponSwingSpent;
    private int tick;

    SwingExecution(MinibossEntity attacker, CombatAction action, AttackVolume volume, AttackFrame frame) {
        this.attacker = attacker;
        this.action = action;
        this.volume = volume;
        this.frame = frame;
        this.previousOrigin = frame.origin();
    }

    public AttackFrame frame() {
        return this.frame;
    }

    /**
     * Advance one active tick: sweep the surface from where it was to where it is, damage what that slice
     * contains, and draw it.
     *
     * @param phaseTick 1-based tick within the active window
     */
    void tick(int phaseTick) {
        this.tick = phaseTick;
        if (!(this.attacker.getWorld() instanceof ServerWorld world)) {
            return;
        }

        // A travelling volume re-anchors to its owner; everything else stays frozen where it committed,
        // which is what lets the player step out of it.
        if (this.volume.shape().followsAttacker()) {
            this.previousOrigin = this.frame.origin();
            this.frame = this.frame.withOrigin(this.attacker.getPos()
                    .add(0.0, this.volume.offsetVertical(), 0.0));
        }

        if (phaseTick == 1 && this.action.ribbon() != null) {
            // One packet, on the tick the damage window opens, so the ribbon and the hitbox start together.
            AttackNetwork.broadcastSwing(this.attacker, this.action, this.frame);
        }

        var active = Math.max(1, this.action.activeTicks());
        var progress = Math.min(1.0f, (float) phaseTick / active);
        // Shapes that do not sweep resolve their whole span on the first tick.
        var from = this.volume.shape().isSwept() ? this.previousProgress : 0.0f;
        var to = this.volume.shape().isSwept() ? progress : 1.0f;

        resolveDamage(world, from, to);
        drawSlash(world, from, to);

        this.previousProgress = progress;
        if (!this.volume.shape().isSwept() && !this.volume.shape().followsAttacker()) {
            // A cone or impact has now resolved; nothing more to do for the rest of the window.
            this.previousProgress = 1.0f;
        }
    }

    // --- damage ----------------------------------------------------------------------------------

    private void resolveDamage(ServerWorld world, float from, float to) {
        var bounds = AttackGeometry.bounds(this.volume, this.frame, this.previousOrigin);
        for (var victim : world.getEntitiesByClass(LivingEntity.class, bounds, this::isValidTarget)) {
            if (!canHitAgain(victim)) {
                continue;
            }
            if (!AttackGeometry.hits(this.volume, this.frame, from, to, victim.getBoundingBox(),
                    this.previousOrigin)) {
                continue;
            }
            strike(victim, to);
        }
    }

    private boolean isValidTarget(LivingEntity candidate) {
        // One predicate for every attack this mod has, shared with the spells — see MinibossEntity#canHarm.
        // A blanket "never another miniboss" lived here and was wrong: it stopped a boss from ever hitting
        // an unallied, unowned or actively hostile miniboss, which the relations model already permits.
        return this.attacker.canHarm(candidate);
    }

    private boolean canHitAgain(LivingEntity victim) {
        var last = this.hits.get(victim);
        if (last == null) {
            return true;
        }
        return this.action.multiHit() && this.tick - last >= this.action.multiHitCooldown();
    }

    private void strike(LivingEntity victim, float progress) {
        this.hits.put(victim, this.tick);

        boolean landed;
        if (this.action.usesWeaponDamage() && !this.weaponSwingSpent) {
            // The first contact of a swing goes through tryAttack, so enchantments, fire aspect and the
            // mod's own attack hooks all fire exactly once per swing rather than once per victim.
            this.weaponSwingSpent = true;
            landed = this.attacker.tryAttack(victim);
        } else {
            var base = (float) this.attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                    * this.action.damageScale();
            landed = victim.damage(this.attacker.getDamageSources().mobAttack(this.attacker), base);
        }
        if (!landed || this.action.knockback() <= 0.0) {
            return;
        }

        // Knockback follows the strike, not just the line between the two bodies: a cleave throws you
        // along its travel, a shockwave throws you outward. The blend is a property of the shape.
        var radial = this.frame.origin().subtract(victim.getPos());
        var radialDir = radial.horizontalLengthSquared() < 1.0e-6
                ? this.frame.forward().multiply(-1.0)
                : new Vec3d(radial.x, 0.0, radial.z).normalize();
        var strike = AttackGeometry.strikeDirection(this.volume, this.frame, progress);
        var alignment = AttackGeometry.knockbackAlignment(this.volume);
        var push = radialDir.multiply(-(1.0 - alignment))
                .add(new Vec3d(strike.x, 0.0, strike.z).multiply(alignment));
        if (push.lengthSquared() < 1.0e-6) {
            push = this.frame.forward();
        }
        push = push.normalize();
        // takeKnockback pushes away from the position given, so the direction is negated.
        victim.takeKnockback(this.action.knockback(), -push.x, -push.z);
        if (this.action.knockbackVertical() > 0.0) {
            victim.addVelocity(0.0, this.action.knockbackVertical(), 0.0);
            victim.velocityModified = true;
        }
    }

    // --- the visible arc -------------------------------------------------------------------------

    /**
     * Draw the slice that was just resolved.
     *
     * <p>Every particle sits on {@link AttackGeometry#surfacePoint}, so the crescent on screen is a
     * literal picture of the damage volume — it cannot reach further than the attack does, and it cannot
     * lag behind the sweep, because both come out of the same call with the same {@code s} range.
     */
    private void drawSlash(ServerWorld world, float from, float to) {
        var visual = this.action.slash();
        if (visual == null || visual.perTick() <= 0) {
            return;
        }
        var random = this.attacker.getRandom();
        for (int i = 0; i < visual.perTick(); i++) {
            var sample = AttackGeometry.sample(this.volume, this.frame, from, to, random,
                    visual.speed(), visual.scatter());
            world.spawnParticles(visual.particle(),
                    sample.position().x, sample.position().y, sample.position().z,
                    // Count 0 with an explicit velocity is vanilla's "one particle, moving this way".
                    0,
                    sample.velocity().x, sample.velocity().y, sample.velocity().z,
                    1.0);
        }
    }
}
