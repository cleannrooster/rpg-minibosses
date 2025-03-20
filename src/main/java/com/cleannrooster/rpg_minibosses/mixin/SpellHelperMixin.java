package com.cleannrooster.rpg_minibosses.mixin;

import com.google.common.base.Suppliers;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.effect.EntityImmunity;
import net.spell_engine.api.effect.StatusEffectClassification;
import net.spell_engine.api.entity.SpellEntity;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.api.tags.SpellEngineEntityTags;
import net.spell_engine.entity.ConfigurableKnockback;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.Ammo;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellTriggers;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCastSyncHelper;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.AnimationHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Next;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static net.spell_engine.internals.SpellHelper.*;

@Mixin(SpellHelper.class)
public class SpellHelperMixin {
    @Overwrite
    public static boolean performImpacts(World world, LivingEntity caster, @Nullable Entity target, Entity aoeSource, RegistryEntry<Spell> spellEntry, List<Spell.Impact> impacts, ImpactContext context, boolean additionalTargetLookup) {
        Collection<ServerPlayerEntity> trackers = target != null ? PlayerLookup.tracking(target) : null;
        Spell spell = (Spell)spellEntry.value();
        boolean anyPerformed = false;
        SpellTarget.Intent selectedIntent = null;
        Iterator var12 = impacts.iterator();

        while(true) {
            Spell.Impact impact;
            SpellTarget.Intent intent;
            do {
                do {
                    if (!var12.hasNext()) {
                        Spell.AreaImpact area_impact = spell.area_impact;
                        if (area_impact != null && additionalTargetLookup && (anyPerformed || target == null)) {
                            lookupAndPerformAreaImpact(area_impact, spellEntry, caster, target, aoeSource, impacts, context, false);
                        }
                        if(caster instanceof PlayerEntity) {
                            if (anyPerformed) {
                                SpellTriggers.onSpellImpactAny((PlayerEntity) caster, target, aoeSource, spellEntry);
                            }

                        }
                        return anyPerformed;
                    }

                    impact = (Spell.Impact)var12.next();
                    intent = impactIntent(impact.action);
                } while(!impact.action.apply_to_caster && selectedIntent != null && selectedIntent != intent);
            } while(target == null);

            boolean result = performImpact(world, caster, target, spellEntry, impact, context, trackers);
            anyPerformed = anyPerformed || result;
            if (result) {
                selectedIntent = intent;
            }
        }
    }

    private static boolean performImpact(World world, LivingEntity caster, Entity target, RegistryEntry<Spell> spellEntry, Spell.Impact impact, ImpactContext context, Collection<ServerPlayerEntity> trackers) {
        if (!((Entity)target).isAttackable()) {
            return false;
        } else {
            boolean success = false;
            boolean critical = false;
            boolean isKnockbackPushed = false;
            Spell spell = (Spell)spellEntry.value();

            try {
                if (impact.action.apply_to_caster) {
                    target = caster;
                } else {
                    SpellTarget.Intent intent = impactIntent(impact.action);
                    if (!EntityRelations.actionAllowed(context.focusMode(), intent, caster, (Entity)target)) {
                        return false;
                    }

                    if (intent == SpellTarget.Intent.HARMFUL && context.focusMode() == SpellTarget.FocusMode.AREA && ((EntityImmunity)target).isImmuneTo(net.spell_engine.api.effect.EntityImmunity.Type.AREA_EFFECT)) {
                        return false;
                    }
                }

                TargetConditionResult conditionResult = evaluateImpactConditions((Entity)target, caster, impact.target_modifiers);
                if (!conditionResult.allowed()) {
                    return false;
                }

                double particleMultiplier = (double)(1.0F * context.total());
                SpellPower.Result power = context.power();
                SpellSchool school = impact.school != null ? impact.school : spell.school;
                if (power == null || power.school() != school) {
                    power = SpellPower.getSpellPower(school, caster);
                }

                if (impact.attribute != null) {
                    RegistryEntry.Reference<EntityAttribute> attributeOverride = (RegistryEntry.Reference)Registries.ATTRIBUTE.getEntry(Identifier.of(impact.attribute)).get();
                    double value = caster.getAttributeValue(attributeOverride);
                    power = new SpellPower.Result(power.school(), value, power.criticalChance(), power.criticalDamage());
                }

                float bonusPower = 1.0F + (Float)conditionResult.modifiers().stream().map((modifier) -> {
                    return modifier.power_multiplier;
                }).reduce(0.0F, Float::sum);
                Float bonusCritChance = (Float)conditionResult.modifiers().stream().map((modifier) -> {
                    return modifier.critical_chance_bonus;
                }).reduce(0.0F, Float::sum);
                Float bonusCritDamage = (Float)conditionResult.modifiers().stream().map((modifier) -> {
                    return modifier.critical_damage_bonus;
                }).reduce(0.0F, Float::sum);
                power = new SpellPower.Result(power.school(), power.baseValue() * (double)bonusPower, power.criticalChance() + (double)bonusCritChance, power.criticalDamage() + (double)bonusCritDamage);
                if (power.baseValue() < (double)impact.action.min_power || power.baseValue() > (double)impact.action.max_power) {
                    double clampedValue = MathHelper.clamp(power.baseValue(), (double)impact.action.min_power, (double)impact.action.max_power);
                    power = new SpellPower.Result(power.school(), clampedValue, power.criticalChance(), power.criticalDamage());
                }

                Vec3d groundJustBelow;
                LivingEntity livingTarget;
                Identifier id;
                label266:
                switch (impact.action.type) {
                    case DAMAGE:
                        Spell.Impact.Action.Damage damageData = impact.action.damage;
                        float knockbackMultiplier = Math.max(0.0F, damageData.knockback * context.total());
                        SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
                        int timeUntilRegen = ((Entity)target).timeUntilRegen;
                        if (target instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity)target;
                            ((ConfigurableKnockback)livingEntity).pushKnockbackMultiplier_SpellEngine(context.hasOffset() ? 0.0F : knockbackMultiplier);
                            isKnockbackPushed = true;
                            if (damageData.bypass_iframes && SpellEngineMod.config.bypass_iframes) {
                                ((Entity)target).timeUntilRegen = 0;
                            }

                            vulnerability = SpellPower.getVulnerability(livingEntity, school);
                        }

                        SpellPower.Result.Value result = power.random(vulnerability);
                        critical = result.isCritical();
                        double amount = result.amount();
                        amount *= (double)damageData.spell_power_coefficient;
                        amount *= (double)context.total();
                        if (context.isChanneled()) {
                            amount *= (double)SpellPower.getHaste(caster, school);
                        }

                        particleMultiplier = power.criticalDamage() + (double)vulnerability.criticalDamageBonus();
                        caster.onAttacking((Entity)target);
                        ((Entity)target).damage(SpellDamageSource.create(school, caster), (float)amount);
                        if (target instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity)target;
                            ((ConfigurableKnockback)livingEntity).popKnockbackMultiplier_SpellEngine();
                            isKnockbackPushed = false;
                            ((Entity)target).timeUntilRegen = timeUntilRegen;
                            if (context.hasOffset()) {
                                groundJustBelow = context.knockbackDirection(livingEntity.getPos()).negate();
                                livingEntity.takeKnockback((double)(0.4F * knockbackMultiplier), groundJustBelow.x, groundJustBelow.z);
                            }
                        }

                        success = true;
                        break;
                    case HEAL:
                        if (target instanceof LivingEntity) {
                            LivingEntity livingTarget1 = (LivingEntity)target;
                            Spell.Impact.Action.Heal healData = impact.action.heal;
                            particleMultiplier = power.criticalDamage();
                            SpellPower.Result.Value result1 = power.random();
                            critical = result1.isCritical();
                            double amount1 = result1.amount();
                            amount1 *= (double)healData.spell_power_coefficient;
                            amount1 *= (double)context.total();
                            if (context.isChanneled()) {
                                amount1 *= (double)SpellPower.getHaste(caster, school);
                            }

                            livingTarget1.heal((float)amount1);
                            success = true;
                        }
                        break;
                    case STATUS_EFFECT:
                        Spell.Impact.Action.StatusEffect data = impact.action.status_effect;
                        if (!(target instanceof LivingEntity)) {
                            break;
                        }

                        livingTarget = (LivingEntity)target;
                        Optional<RegistryEntry<StatusEffect>> optionalEffect = Optional.empty();
                        if (data.remove != null) {
                            List<StatusEffectInstance> effects = livingTarget.getStatusEffects().stream().filter((instance) -> {
                                return ((StatusEffect)instance.getEffectType().value()).isBeneficial() == data.remove.select_beneficial;
                            }).toList();
                            switch (data.remove.selector) {
                                case RANDOM:
                                    optionalEffect = Optional.of((StatusEffectInstance)effects.get(world.random.nextInt(effects.size()))).map(StatusEffectInstance::getEffectType);
                                    break;
                                case FIRST:
                                    optionalEffect = Optional.of((StatusEffectInstance)effects.getFirst()).map(StatusEffectInstance::getEffectType);
                            }
                        } else {
                            id = Identifier.of(data.effect_id);
                            optionalEffect = Optional.of((RegistryEntry)Registries.STATUS_EFFECT.getEntry(id).get());
                        }

                        if (optionalEffect.isEmpty()) {
                            return false;
                        }

                        RegistryEntry<StatusEffect> effect = (RegistryEntry)optionalEffect.get();
                        if (!underApplyLimit(power, livingTarget, school, data.apply_limit)) {
                            return false;
                        }

                        int amplifier = data.amplifier + (int)((double)data.amplifier_power_multiplier * power.nonCriticalValue());
                        switch (data.apply_mode) {
                            case ADD:
                            case SET:
                                if (!((Entity)target).getType().isIn(SpellEngineEntityTags.bosses) || !StatusEffectClassification.isMovementImpairing(effect) && !StatusEffectClassification.disablesMobAI(effect)) {
                                    int duration = Math.round(data.duration * 20.0F);
                                    boolean showParticles = data.show_particles;
                                    StatusEffectInstance currentEffect;
                                    if (data.apply_mode == Spell.Impact.Action.StatusEffect.ApplyMode.ADD) {
                                        currentEffect = livingTarget.getStatusEffect(effect);
                                        int newAmplifier = 0;
                                        if (currentEffect != null) {
                                            int currentAmplifier = currentEffect.getAmplifier();
                                            int incrementedAmplifier = currentAmplifier + 1;
                                            newAmplifier = Math.min(incrementedAmplifier, amplifier);
                                            if (!data.refresh_duration) {
                                                if (currentAmplifier == newAmplifier) {
                                                    return false;
                                                }

                                                duration = currentEffect.getDuration();
                                            }
                                        }

                                        amplifier = newAmplifier;
                                    }

                                    currentEffect = new StatusEffectInstance(effect, duration, amplifier, false, showParticles, true);
                                    livingTarget.addStatusEffect(currentEffect, caster);
                                    success = true;
                                    break label266;
                                }

                                return false;
                            case REMOVE:
                                if (livingTarget.hasStatusEffect(effect)) {
                                    StatusEffectInstance currentEffect = livingTarget.getStatusEffect(effect);
                                    int newAmplifier = amplifier > 0 ? currentEffect.getAmplifier() - amplifier : -1;
                                    if (newAmplifier < 0) {
                                        livingTarget.removeStatusEffect(effect);
                                    } else {
                                        livingTarget.addStatusEffect(new StatusEffectInstance(effect, currentEffect.getDuration(), newAmplifier, currentEffect.isAmbient(), currentEffect.shouldShowParticles(), currentEffect.shouldShowIcon()), caster);
                                    }

                                    success = true;
                                }
                            default:
                                break label266;
                        }
                    case FIRE:
                        Spell.Impact.Action.Fire data1 = impact.action.fire;
                        ((Entity)target).setOnFireFor((float)data1.duration);
                        if (((Entity)target).getFireTicks() > 0) {
                            ((Entity)target).setFireTicks(((Entity)target).getFireTicks() + data1.tick_offset);
                        }
                        break;
                    case SPAWN:
                        List<Spell.Impact.Action.Spawn> spawns = impact.action.spawns;
                        if (spawns != null && !spawns.isEmpty()) {
                            Iterator var43 = spawns.iterator();

                            while(true) {
                                if (!var43.hasNext()) {
                                    break label266;
                                }

                                Spell.Impact.Action.Spawn data3 = (Spell.Impact.Action.Spawn)var43.next();
                                id = Identifier.of(data3.entity_type_id);
                                EntityType<?> type = (EntityType)Registries.ENTITY_TYPE.get(id);
                                Entity entity = type.create(world);
                                applyEntityPlacement(entity, caster, ((Entity)target).getPos(), data3.placement);
                                if (entity instanceof SpellEntity.Spawned) {
                                    SpellEntity.Spawned spellSpawnedEntity = (SpellEntity.Spawned)entity;
                                    SpellEntity.Spawned.Args args = new SpellEntity.Spawned.Args(caster, spellEntry, data3, context);
                                    spellSpawnedEntity.onSpawnedBySpell(args);
                                }

                                ((WorldScheduler)world).schedule(data3.delay_ticks, () -> {
                                    world.spawnEntity(entity);
                                });
                                success = true;
                            }
                        }

                        return false;
                    case TELEPORT:
                        Spell.Impact.Action.Teleport data2 = impact.action.teleport;
                        if (!(target instanceof LivingEntity)) {
                            break;
                        }

                        livingTarget = (LivingEntity)target;
                        LivingEntity teleportedEntity = null;
                        Vec3d destination = null;
                        Vec3d startingPosition = null;
                        Float applyRotation = null;
                        switch (data2.mode) {
                            case FORWARD:
                                teleportedEntity = livingTarget;
                                Spell.Impact.Action.Teleport.Forward forward = data2.forward;
                                Vec3d look = ((Entity)target).getRotationVector();
                                startingPosition = ((Entity)target).getPos();
                                destination = TargetHelper.findTeleportDestination(livingTarget, look, forward.distance, data2.required_clearance_block_y);
                                groundJustBelow = TargetHelper.findSolidBlockBelow(livingTarget, destination, ((Entity)target).getWorld(), -1.5F);
                                if (groundJustBelow != null) {
                                    destination = groundJustBelow;
                                }
                                break;
                            case BEHIND_TARGET:
                                if (livingTarget == caster) {
                                    return false;
                                }

                                Vec3d look1 = ((Entity)target).getRotationVector();
                                float distance = 1.0F;
                                if (data2.behind_target != null) {
                                    distance = data2.behind_target.distance;
                                }

                                teleportedEntity = caster;
                                startingPosition = caster.getPos();
                                destination = ((Entity)target).getPos().add(look1.multiply((double)(-distance)));
                                groundJustBelow = TargetHelper.findSolidBlockBelow(caster, destination, ((Entity)target).getWorld(), -1.5F);
                                if (groundJustBelow != null) {
                                    destination = groundJustBelow;
                                }

                                double x = look1.x;
                                double z = look1.z;
                                float yaw = (float)Math.toDegrees(Math.atan2(-x, z));
                                yaw = yaw < 0.0F ? yaw + 360.0F : yaw;
                                applyRotation = yaw;
                        }

                        if (destination == null || startingPosition == null || teleportedEntity == null) {
                            break;
                        }

                        label251: {
                            ParticleHelper.sendBatches(teleportedEntity, data2.depart_particles, false);
                            world.emitGameEvent(GameEvent.TELEPORT, startingPosition, GameEvent.Emitter.of(teleportedEntity));
                            if (applyRotation != null && teleportedEntity instanceof ServerPlayerEntity) {
                                ServerPlayerEntity serverPlayer = (ServerPlayerEntity)teleportedEntity;
                                if (world instanceof ServerWorld) {
                                    ServerWorld serverWorld = (ServerWorld)world;
                                    serverPlayer.teleport(serverWorld, destination.x, destination.y, destination.z, applyRotation, serverPlayer.getPitch());
                                    break label251;
                                }
                            }

                            teleportedEntity.teleport(destination.x, destination.y, destination.z, false);
                        }

                        success = true;
                        ParticleHelper.sendBatches(teleportedEntity, data2.arrive_particles, false);
                        break;
                    case CUSTOM:
                        if (impact.action.custom != null) {
                            SpellHandlers.CustomImpact handler = (SpellHandlers.CustomImpact)SpellHandlers.customImpact.get(impact.action.custom.handler);
                            if (handler != null) {
                                SpellHandlers.ImpactResult result1 = handler.onSpellImpact(spellEntry, power, caster, (Entity)target, context);
                                particleMultiplier = power.criticalDamage();
                                success = result1.success();
                                critical = result1.critical();
                            }
                        }
                }

                if (success) {
                    if (impact.particles != null) {
                        float countMultiplier = critical ? (float)particleMultiplier : 1.0F;
                        ParticleHelper.sendBatches((Entity)target, impact.particles, countMultiplier * caster.getScale(), trackers);
                    }

                    if (impact.sound != null) {
                        SoundHelper.playSound(world, (Entity)target, impact.sound);
                    }
                    if(caster instanceof PlayerEntity) {

                        SpellTriggers.onSpellImpactSpecific((PlayerEntity) caster, (Entity) target, spellEntry, impact, critical);
                    }
                }
            } catch (Exception var33) {
                System.err.println("Failed to perform impact effect");
                System.err.println(var33.getMessage());
                if (isKnockbackPushed) {
                    ((ConfigurableKnockback)target).popKnockbackMultiplier_SpellEngine();
                }
            }

            return success;
        }
    }
}
