package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
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
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineSounds;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import net.spell_engine.internals.delivery.ProjectileLauncher;
import net.spell_engine.internals.delivery.CloudPlacer;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

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
                    SoundHelper.playSound(entity.getWorld(), entity,
                            new Sound(SpellEngineSounds.GENERIC_FIRE_CASTING.id()));
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
                    ParticleHelper.sendBatches(entity, SpellRegistry.from(entity.getWorld())
                            .get(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "fire_nova"))
                            .release.visuals.particles);
                    SoundHelper.playSound(entity.getWorld(), entity,
                            new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
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
        if (target == null || entity.getWorld().isClient() || !entity.canSee(target)) return;
        var id = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "fireball");
        entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
        SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
        ProjectileLauncher.shootProjectile(entity.getWorld(), entity, target,
            SpellRegistry.from(entity.getWorld()).getEntry(id).get(),
            new SpellExecution.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, entity))
                    .position(entity.getPos()));
        ParticleHelper.sendBatches(entity, SpellRegistry.from(entity.getWorld()).get(id).release.visuals.particles);
    }

    private void castVolley(com.cleannrooster.rpg_minibosses.entity.combat.ActionContext ctx) {
        var target = ctx.liveTarget();
        if (target == null || entity.getWorld().isClient() || !entity.canSee(target)) return;
        var id = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "lesser_fire_volley");
        SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
        ProjectileLauncher.shootProjectile(entity.getWorld(), entity, target,
            SpellRegistry.from(entity.getWorld()).getEntry(id).get(),
            new SpellExecution.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, entity))
                    .position(entity.getPos()));
        ParticleHelper.sendBatches(entity, SpellRegistry.from(entity.getWorld()).get(id).release.visuals.particles);
    }

    private void castNova() {
        if (entity.getWorld().isClient()) return;
        var world = entity.getWorld();
        var id = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "fire_nova");
        SoundHelper.playSound(world, entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
        // Same predicate as the mage's melee sweep; Nova used to catch allies and its owner too.
        for (Entity struck : TargetHelper.targetsFromArea(entity, 6, new Spell.Target.Area(),
                entity::canHarm)) {
            SpellImpacts.performImpacts(world, entity, struck, entity,
                SpellRegistry.from(world).getEntry(id).get(),
                SpellRegistry.from(world).get(id).impacts,
                new SpellExecution.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, entity))
                        .position(entity.getPos()));
        }
        ParticleHelper.sendBatches(entity, SpellRegistry.from(world).get(id).release.visuals.particles);
    }
}
