package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.ArchmageFireEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineSounds;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.List;

/**
 * AI brain for the Fire Mage (ArchmageFireEntity).
 *
 * Personality: AOE zoner + projectile harassment + DoT pressure.
 * Punishes: standing still and tunnel vision.
 *
 * Stances:   HARASSMENT (default), ZONING (low HP)
 * States:    PRESSURING (range ≥ melee), REPOSITIONING (target too close)
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

    // Cooldowns in ticks (scaled by getCooldownCoeff() on trigger)
    private static final int FIREBALL_CD   = 40;
    private static final int FIRE_NOVA_CD  = 220;
    private static final int FIRE_VOLLEY_CD = 320;
    private static final int JUMP_BACK_CD  = 320;

    // Stationary tracking for AOE placement
    private int   targetStationaryTicks = 0;
    private Vec3d prevTargetPos;

    private boolean strafingAway = false;

    private final ArchmageFireEntity archmage;

    public FireMageBrain(ArchmageFireEntity entity) {
        super(entity, Stance.HARASSMENT, CombatState.PRESSURING);
        this.archmage = entity;
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
            new Transition(CombatState.PRESSURING,    s -> s.targetInMeleeRange,                               CombatState.REPOSITIONING, 10),
            new Transition(CombatState.REPOSITIONING, s -> !s.targetInMeleeRange && s.lineOfSightToTarget,     CombatState.PRESSURING,    10)
        );
    }

    @Override
    protected List<PhaseTransition> definePhaseTransitions() {
        return List.of();
    }

    @Override
    protected void executeCurrentCombatState() {
        if (entity.getTarget() == null) return;

        // Update stationary counter
        Vec3d targetPos = entity.getTarget().getPos();
        if (prevTargetPos != null && targetPos.squaredDistanceTo(prevTargetPos) < 0.01) {
            targetStationaryTicks++;
        } else {
            targetStationaryTicks = 0;
        }
        prevTargetPos = targetPos;

        if (currentCombatState == CombatState.PRESSURING) {
            tickPressuring();
        } else {
            tickRepositioning();
        }
    }

    // ── PRESSURING ────────────────────────────────────────────────────────────

    private void tickPressuring() {
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);

        float dist   = stimulus.targetDistance;
        boolean canSee = stimulus.lineOfSightToTarget;

        // Movement: approach if far, strafe if in optimal range
        strafingAway = false;
        if (dist > 8) {
            entity.getNavigation().startMovingTo(
                entity.getTarget().getX(),
                entity.getTarget().getY(),
                entity.getTarget().getZ(), 1.0);
        } else {
            strafeAround();
        }

        if (!canSee || entity.performing) return;

        // Ability priority
        if (dist < 6 && cooldowns.isReady("fire_nova")) {
            fireNova();
        } else if (dist > 4 && targetStationaryTicks >= 12 && cooldowns.isReady("fire_volley")) {
            fireVolley();
        } else if (dist > 4 && cooldowns.isReady("fireball") && !strafingAway) {
            fireFireball();
        }
    }

    // ── REPOSITIONING ─────────────────────────────────────────────────────────

    private void tickRepositioning() {
        if (entity.getTarget() == null) return;
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);

        // Jump back first — buys distance and punishes gap-close
        if (cooldowns.isReady("jump_back") && stimulus.targetInMeleeRange) {
            jumpBack();
        } else {
            // Strafe away from target
            Vec3d away = entity.getPos().subtract(entity.getTarget().getPos()).normalize();
            entity.getNavigation().startMovingTo(
                entity.getX() + away.x * 3,
                entity.getY(),
                entity.getZ() + away.z * 3,
                1.3);
        }
        // No AOE / projectile during repositioning
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    private void fireFireball() {
        int cd = Math.max(20, FIREBALL_CD - (int)(FIREBALL_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("fireball", cd);
        entity.performing = true;

        entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entity.getTarget().getEyePos());
        SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
        SpellHelper.shootProjectile(entity.getWorld(), entity, entity.getTarget(),
            SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "fireball")).get(),
            new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, entity)).position(entity.getPos()));
        ParticleHelper.sendBatches(entity,
            SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "fireball")).release.particles);
        entity.dispatcher.throw1();

        ((WorldScheduler) entity.getWorld()).schedule(10, () -> {
            entity.dispatcher.throw2();
            entity.performing = false;
            if (entity.getTarget() != null && entity.canSee(entity.getTarget())) {
                entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entity.getTarget().getEyePos());
                SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
                SpellHelper.shootProjectile(entity.getWorld(), entity, entity.getTarget(),
                    SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "fireball")).get(),
                    new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, entity)).position(entity.getPos()));
                ParticleHelper.sendBatches(entity,
                    SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "fireball")).release.particles);
            }
        });
    }

    private void fireVolley() {
        int cd = Math.max(160, FIRE_VOLLEY_CD - (int)(FIRE_VOLLEY_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("fire_volley", cd);
        entity.performing = true;
        entity.resetIndicator();

        if (entity.getMoveControl().isMoving()) {
            entity.dispatcher.setWalkwave();
        } else {
            entity.dispatcher.setWAVE_1h();
        }

        ((WorldScheduler) entity.getWorld()).schedule(20, () ->
            SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id())));
        ((WorldScheduler) entity.getWorld()).schedule(40, () ->
            SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id())));
        ((WorldScheduler) entity.getWorld()).schedule(60, () -> {
            if (entity.getTarget() != null && entity.canSee(entity.getTarget())) {
                SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
                SpellHelper.shootProjectile(entity.getWorld(), entity, entity.getTarget(),
                    SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "lesser_fire_volley")).get(),
                    new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, entity)).position(entity.getPos()));
                ParticleHelper.sendBatches(entity,
                    SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "lesser_fire_volley")).release.particles);
            }
            entity.performing = false;
        });
    }

    private void fireNova() {
        int cd = Math.max(110, FIRE_NOVA_CD - (int)(FIRE_NOVA_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("fire_nova", cd);
        entity.performing = true;
        entity.resetIndicator();

        ((WorldScheduler) entity.getWorld()).schedule(10, () -> {
            if (entity.getMoveControl().isMoving()) {
                entity.dispatcher.setWalkwave();
            } else {
                entity.dispatcher.setWAVE_1h();
            }
            ((WorldScheduler) entity.getWorld()).schedule(20, () -> {
                if (entity.getTarget() != null && entity.canSee(entity.getTarget())) {
                    SoundHelper.playSound(entity.getWorld(), entity, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
                    for (Entity target : TargetHelper.targetsFromArea(entity, 6, new Spell.Target.Area(), null)) {
                        SpellHelper.performImpacts(entity.getWorld(), entity, target, entity,
                            SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "fire_nova")).get(),
                            SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "fire_nova")).impacts,
                            new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, entity)).position(entity.getPos()));
                    }
                    ParticleHelper.sendBatches(entity,
                        SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "fire_nova")).release.particles);
                }
                entity.performing = false;
            });
        });
    }

    private void jumpBack() {
        int cd = Math.max(160, JUMP_BACK_CD - (int)(JUMP_BACK_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("jump_back", cd);

        entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entity.getTarget().getEyePos());
        Vec3d away = new Vec3d(
            entity.getX() - entity.getTarget().getX(),
            0,
            entity.getZ() - entity.getTarget().getZ()
        ).normalize();
        entity.setPosition(entity.getPos().add(0, 0.2, 0));
        entity.setOnGround(false);
        entity.setVelocity(away.x, 0.5, away.z);
    }

    private void strafeAround() {
        if (entity.getTarget() == null) return;
        strafingAway = true;
        Vec3d cross = entity.getTarget().getPos().subtract(entity.getPos())
            .crossProduct(new Vec3d(0, 1, 0));
        double dot = cross.dotProduct(entity.getRotationVector());
        (entity.getMoveControl())
            .strafeTo(-0.5F, dot > 0 ? -0.5F : 0.5F);
    }
}
