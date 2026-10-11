package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.server.world.ServerWorld;
import com.cleannrooster.rpg_minibosses.magic.AbilityExecutor;
import com.cleannrooster.rpg_minibosses.magic.AbilitySpec;
import com.cleannrooster.rpg_minibosses.magic.BossSounds;
import com.cleannrooster.rpg_minibosses.magic.CastTarget;
import com.cleannrooster.rpg_minibosses.magic.Delivery;
import com.cleannrooster.rpg_minibosses.magic.Impact;
import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import com.cleannrooster.rpg_minibosses.magic.TickScheduler;
import com.cleannrooster.rpg_minibosses.magic.fx.CueArgs;
import com.cleannrooster.rpg_minibosses.magic.fx.FxCues;
import com.cleannrooster.rpg_minibosses.magic.fx.FxEmitter;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossAnimationProvider;
import com.cleannrooster.rpg_minibosses.entity.ArchmageFireEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import com.cleannrooster.rpg_minibosses.entity.combat.AttackMotion;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.GroundLocomotion;
import com.cleannrooster.rpg_minibosses.entity.combat.SlashProfile;
import com.cleannrooster.rpg_minibosses.entity.combat.SlashVisual;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackPlane;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackShape;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackVolume;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.SwingDynamics;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.SwingPath;
import net.minecraft.particle.ParticleTypes;
import com.cleannrooster.rpg_minibosses.entity.combat.MovementProfile;
import com.cleannrooster.rpg_minibosses.entity.combat.TrackingMode;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * AI brain for the Fire Mage.
 *
 * <p><b>Identity: elastic spacing and mobile casting.</b> It lives in a band around six to nine blocks and
 * fights to stay there — but it holds that band with <em>tangential</em> movement, not by backing away in
 * a straight line, which is both more readable and much harder to corner.
 *
 * <p>The casting kit is graded by how much mobility each spell costs. Fireball is nearly free and can be
 * thrown mid-drift. The Volley is a long cast that restricts radial movement but still allows a slow
 * orbit. Nova costs everything — it is fully planted, and the mage trades all of its spacing for it,
 * which is exactly why breaching its range is a real decision rather than a free win.
 *
 * <p>Preserved: HARASSMENT / ZONING, PRESSURING / REPOSITIONING, Fireball, Fire Volley, Fire Nova and
 * Jump Back.
 */
public class FireMageBrain extends MobBrain {

    /** The old {@code fireball} spell: a 1.6 b/t bolt, 0.4 × fire power, light knockback, 1.5 s of burning. */
    static final AbilitySpec FIREBALL = AbilitySpec.builder(RPGMinibosses.id("fire_mage/fireball"), MagicSchool.FIRE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.6).withLifetime(20)))
            .impact(new Impact.Damage(0.4f, 0.2f, false))
            .impact(new Impact.Ignite(1.5f))
            .build();

    /**
     * One bolt of the old {@code lesser_fire_volley}: speed 1, homing 20°/tick once four blocks out,
     * bursting over four blocks for 0.2 × fire power with heavy knockback. (The spell let each bolt pierce
     * and bounce indefinitely; here each bursts once, on first contact.)
     */
    static final AbilitySpec VOLLEY_BOLT = AbilitySpec.builder(RPGMinibosses.id("fire_mage/volley_bolt"), MagicSchool.FIRE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.0).homing(20.0f, 4).withLifetime(100)
                    .bursting(new TelegraphArea.Circle(Vec3d.ZERO, 4.0f), 3.0)))
            .impact(new Impact.Damage(0.2f, 2.0f, false))
            .build();

    private static final float[] VOLLEY_YAW_OFFSETS = {45.0f, -45.0f, 135.0f, -135.0f};

    /** The old {@code fire_nova} spell: everything within six blocks takes 0.6 × fire power and burns for 3 s. */
    static final AbilitySpec NOVA = AbilitySpec.builder(RPGMinibosses.id("fire_mage/nova"), MagicSchool.FIRE)
            .delivery(Delivery.Area.around(new TelegraphArea.Circle(Vec3d.ZERO, 6.0f)))
            .damage(0.6f)
            .impact(new Impact.Ignite(3.0f))
            .build();

    public enum Stance implements BrainState {
        HARASSMENT, ZONING;
        @Override public String id() { return name(); }
    }

    public enum CombatState implements BrainState {
        PRESSURING, REPOSITIONING;
        @Override public String id() { return name(); }
    }

    // Distance thresholds
    private static final float MELEE_THRESHOLD      = 4.0f;
    private static final float ENGAGEMENT_THRESHOLD  = 20.0f;
    private static final float FLEE_THRESHOLD        = 24.0f;

    /** The elastic band the mage fights in. */
    private static final double SPACING_NEAR = 6.0;
    private static final double SPACING_FAR  = 9.0;
    /** Inside this, the mage stops casting and starts creating separation. */
    private static final double BREACH_RANGE = 4.5;

    // Stationary tracking for AOE placement
    private int   targetStationaryTicks = 0;
    private Vec3d prevTargetPos;
    /** Which way it is currently drifting around the target. */
    private int orbitDirection = 1;
    private int orbitFlipTimer = 0;

    private final ArchmageFireEntity archmage;

    private final CombatAction fireball;
    private final CombatAction volley;
    private final CombatAction nova;
    private final CombatAction jumpBack;
    private final CombatAction flameSweep;

    public FireMageBrain(ArchmageFireEntity entity) {
        super(entity, Stance.HARASSMENT, CombatState.PRESSURING);
        this.archmage = entity;

        var family = MinibossAnimationProvider.MinibossAnimationDispatcher.MAGE;

        // Fireball: lightweight and mobile. The action never claims the body, so the lateral drift
        // continues underneath it and the mage is not a stationary target for the sake of a cheap spell.
        this.fireball = CombatAction.builder("mage_fireball")
                .animation(family + "cast_quick")
                .timing(4, 9, 4)
                .motion(AttackMotion.MOBILE)
                .tracking(TrackingMode.FULL, TrackingMode.REDUCED)
                .cooldown(80)
                .onStart(ctx -> archmage.startTalking(ArchmageFireEntity.TALK_SHORT))
                .onActiveStart(ctx -> castFireball())
                .onActiveTick(ctx -> {
                    if (ctx.phaseTick() == 5) {
                        ctx.entity().dispatcher.play("attacks", family + "cast_quick", false, 1.0f);
                        castFireball();
                    }
                })
                .build();

        // Jump Back: a committed evasive movement, angled rather than straight away, ending with the mage
        // already facing the target and free to throw.
        this.jumpBack = CombatAction.builder("mage_jump_back")
                .animation(family + "jump_back")
                .timing(4, 12, 3)
                .motion(AttackMotion.RETREATING)
                .committed(0.42, 12, 0.46, 0.0)
                .tracking(TrackingMode.FULL, TrackingMode.MINIMAL)
                .cooldown(400)
                .onStart(ctx -> ctx.entity().playSound(net.minecraft.sound.SoundEvents.ENTITY_BLAZE_SHOOT,
                        0.7f, 1.4f))
                .onFinish(ctx -> {
                    var target = ctx.liveTarget();
                    if (target != null) {
                        ctx.locomotion().faceTarget(target, 90f);
                    }
                    // Landing is immediately followed by ranged pressure rather than by a reset.
                    if (cooldowns.isReady("mage_fireball")) {
                        chain(this.fireball);
                    }
                })
                .build();

        // Fire Volley: a slow mobile cast. It keeps its distance ring but is only allowed to orbit at a
        // crawl, so the long charge stays readable and the player has somewhere to be.
        this.volley = CombatAction.builder("mage_volley")
                .animation(family + "cast_long")
                .timing(54, 6, 14)
                .motion(AttackMotion.MOBILE)
                .tracking(TrackingMode.REDUCED, TrackingMode.REDUCED)
                .cooldown(640)
                .onStart(ctx -> {
                    if (entity.getWorld() instanceof ServerWorld world) {
                        FxEmitter.sound(world, entity.getPos(), BossSounds.FIRE.windup(), 1.5f, 1.0f);
                    }
                    // The long charge is the one the player most needs to recognise early, so the
                    // incantation runs the length of the wind-up rather than just its opening.
                    archmage.startTalking(ArchmageFireEntity.TALK_LONG);
                })
                .onActiveStart(this::castVolley)
                .onFinish(ctx -> cooldowns.trigger("volley_recovered", 1))
                .build();

        // Fire Nova: fully planted. The mage arrests, winds up, detonates, then immediately buys its
        // spacing back — a jump back if one is available, otherwise a hard tangential break.
        this.nova = CombatAction.builder("mage_nova")
                .animation(family + "nova")
                .timing(16, 3, 11)
                .motion(AttackMotion.PLANTED)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(440)
                .onStart(ctx -> {
                    ctx.locomotion().arrest(MovementProfile.HARD_STOP);
                    archmage.startTalking(ArchmageFireEntity.TALK_SHORT);
                })
                .onActiveStart(ctx -> castNova())
                .onFinish(ctx -> {
                    // Nova always resolves into separation — that is the whole trade it just made.
                    if (cooldowns.isReady("mage_jump_back")) {
                        chain(this.jumpBack);
                    }
                })
                .build();

        // New: a short directional flame sweep. The kit had nothing between "you are too close for me to
        // cast" and "I spend my Nova", so a player who simply walked into contact could stall it. This is
        // cheap, short-ranged and carries a small backward step — separation, not damage.
        this.flameSweep = CombatAction.builder("mage_flame_sweep")
                .animation(family + "flame_sweep")
                .timing(8, 4, 8)
                .motion(AttackMotion.PRESSING)
                .windupDrift(-0.3)
                .advance(-0.45)   // the sweep carries it backwards out of contact
                .tracking(TrackingMode.FULL, TrackingMode.REDUCED)
                .cooldown(180)
                // A wide shallow fan across the mage's front - the one melee shape in its kit, and the arc
                // that damages is the arc that is drawn.
                .volume(AttackVolume.sweep(
                        SwingPath.rightToLeft(140f).withDynamics(SwingDynamics.FORCEFUL), 3.2, 2.4)
                        .withInnerRadius(0.3))
                .scaledDamage(0.55f)
                .knockback(0.45, 0.15)
                .slash(SlashVisual.light(ParticleTypes.FLAME).withCount(24))
                .ribbon(SlashProfile.crescent(1.0f, 0.55f, 0.20f))
                .onActiveStart(ctx -> {
                    var target = ctx.liveTarget();
                    if (target != null && entity.distanceTo(target) < 3.6f) {
                        target.setOnFireFor(3);
                    }
                    if (entity.getWorld() instanceof ServerWorld world) {
                        // The flames thrown across the fan: the sweep's own arc, not a nova round the mage.
                        var mouth = entity.getPos().add(0.0, entity.getHeight() * 0.6, 0.0);
                        FxEmitter.emit(world, FxCues.CONE, CueArgs.cone(MagicSchool.FIRE, mouth,
                                ctx.committedForward(), 70.0f, 3.2f, 1, world.random.nextLong()));
                        FxEmitter.sound(world, entity.getPos(), BossSounds.FIRE.release(), 1.5f, 1.0f);
                    }
                })
                .build();
    }

    @Override
    protected AIStimulus createStimulus() {
        return new AIStimulus(entity, MELEE_THRESHOLD, ENGAGEMENT_THRESHOLD, FLEE_THRESHOLD, 10.0f);
    }

    @Override
    protected List<Transition> stanceTransitions() {
        return List.of(
            new Transition(Stance.HARASSMENT, s -> s.ownHealthPct < 0.5f,     Stance.ZONING,     1),
            new Transition(Stance.ZONING,     s -> s.timeSinceLastHit > 200,  Stance.HARASSMENT, 1)
        );
    }

    @Override
    protected List<Transition> combatStateTransitions() {
        return List.of(
            new Transition(CombatState.PRESSURING,    s -> s.targetDistance < BREACH_RANGE,                CombatState.REPOSITIONING, 10),
            new Transition(CombatState.REPOSITIONING, s -> s.targetDistance >= SPACING_NEAR && s.lineOfSightToTarget, CombatState.PRESSURING,    10)
        );
    }

    @Override
    protected List<PhaseTransition> definePhaseTransitions() {
        return List.of();
    }

    @Override
    protected void executeCurrentCombatState() {
        var target = entity.getTarget();
        if (target == null) return;

        // Update stationary counter
        Vec3d targetPos = target.getPos();
        if (prevTargetPos != null && targetPos.squaredDistanceTo(prevTargetPos) < 0.01) {
            targetStationaryTicks++;
        } else {
            targetStationaryTicks = 0;
        }
        prevTargetPos = targetPos;

        if (--orbitFlipTimer <= 0) {
            orbitFlipTimer = 70 + entity.getRandom().nextInt(70);
            if (entity.getRandom().nextFloat() < 0.45f) {
                orbitDirection = -orbitDirection;
            }
        }

        if (actionOwnsMovement()) {
            return;
        }
        if (tryChain()) {
            return;
        }

        if (currentCombatState == CombatState.PRESSURING) {
            tickPressuring(target);
        } else {
            tickRepositioning(target);
        }
    }

    // ── PRESSURING ────────────────────────────────────────────────────────────

    private void tickPressuring(LivingEntity target) {
        boolean longCasting = actions.isBusy() && actions.current() == this.volley;
        // During the long cast the legs may still drift, but only slowly and only sideways: radial
        // movement is what would make the charge unreadable, so that is the part that is taken away.
        var profile = longCasting ? MovementProfile.CASTING_DRIFT : spacingProfile();

        locomotion.faceTarget(target, profile.turnDegrees());
        float distance = stimulus.targetDistance;

        if (!stimulus.lineOfSightToTarget && !longCasting) {
            locomotion.navigateTo(target.getX(), target.getY(), target.getZ(), 1.0);
        } else if (longCasting) {
            locomotion.orbit(target, (SPACING_NEAR + SPACING_FAR) * 0.5, orbitDirection, profile);
        } else if (distance > SPACING_FAR + 3.0) {
            locomotion.setFallbackGoal(target.getPos(), 1.0);
            locomotion.pressure(target, SPACING_FAR, profile);
        } else {
            // Inside the band, movement is tangential by preference. Retreating radially from a player who
            // is faster than you is a losing move; circling is not.
            locomotion.setFallbackGoal(target.getPos(), 1.0);
            locomotion.orbit(target, (SPACING_NEAR + SPACING_FAR) * 0.5, orbitDirection, profile);
        }

        if (actions.isBusy() || !stimulus.lineOfSightToTarget) {
            return;
        }

        if (distance < BREACH_RANGE + 1.5 && cooldowns.isReady("mage_nova")) {
            startAction(this.nova);
        } else if (distance < BREACH_RANGE + 1.0 && cooldowns.isReady("mage_flame_sweep")) {
            startAction(this.flameSweep);
        } else if (distance > 4 && targetStationaryTicks >= 12 && cooldowns.isReady("mage_volley")) {
            startAction(this.volley);
        } else if (distance > 4 && cooldowns.isReady("mage_fireball")) {
            startAction(this.fireball);
        }
    }

    // ── REPOSITIONING ─────────────────────────────────────────────────────────

    private void tickRepositioning(LivingEntity target) {
        var profile = spacingProfile();
        locomotion.faceTarget(target, profile.turnDegrees());

        if (!actions.isBusy()) {
            // Order of separation tools, cheapest last: Nova if it is up and they are on top of us, the
            // sweep to make contact unprofitable, the jump back to actually leave.
            if (cooldowns.isReady("mage_nova") && stimulus.targetDistance < BREACH_RANGE) {
                startAction(this.nova);
                return;
            }
            if (cooldowns.isReady("mage_jump_back")) {
                startAction(this.jumpBack);
                return;
            }
            if (cooldowns.isReady("mage_flame_sweep") && stimulus.targetDistance < 3.8f) {
                startAction(this.flameSweep);
                return;
            }
        }

        // No tool available: break tangentially rather than backing away in a straight line.
        var exit = entity.getPos().add(locomotion.retreatVector(target).multiply(8.0));
        locomotion.setFallbackGoal(exit, 1.3);
        locomotion.retreatFrom(target, SPACING_NEAR, profile.withMaxSpeed(profile.maxSpeed() * 1.2));
    }

    /** ZONING is a hurt mage: it holds its spacing harder rather than throwing more. */
    private MovementProfile spacingProfile() {
        return currentStance == Stance.ZONING
                ? MovementProfile.ELASTIC.urgent(1.12, 1.15, 1.05f)
                : MovementProfile.ELASTIC;
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    private void castFireball() {
        var target = entity.getTarget();
        if (target == null || !(entity.getWorld() instanceof ServerWorld world) || !entity.canSee(target)) return;
        entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
        AbilityExecutor.cast(world, entity, FIREBALL, CastTarget.at(entity, target));
    }

    /**
     * Four bolts, two ticks apart, thrown up and out to either side and behind (the old spell's direction
     * offsets), each curling back onto the target once it is clear of the mage.
     */
    private void castVolley(com.cleannrooster.rpg_minibosses.entity.combat.ActionContext ctx) {
        var target = ctx.liveTarget();
        if (target == null || !(entity.getWorld() instanceof ServerWorld world) || !entity.canSee(target)) return;
        var aim = CastTarget.at(entity, target);
        for (int i = 0; i < VOLLEY_YAW_OFFSETS.length; i++) {
            var direction = Vec3d.fromPolar(-45.0f, aim.yaw() + VOLLEY_YAW_OFFSETS[i]);
            int delay = i * 2;
            if (delay == 0) {
                AbilityExecutor.castProjectile(world, entity, VOLLEY_BOLT, aim, direction);
            } else {
                TickScheduler.schedule(world, delay, () -> {
                    if (entity.isAlive()) {
                        AbilityExecutor.castProjectile(world, entity, VOLLEY_BOLT, aim, direction);
                    }
                });
            }
        }
    }

    private void castNova() {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;
        // Relations.canHarm is the mage's melee-sweep predicate too; Nova used to catch allies and its owner.
        AbilityExecutor.cast(world, entity, NOVA, CastTarget.at(entity, entity.getPos()));
    }
}
