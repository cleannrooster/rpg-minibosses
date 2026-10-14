package com.cleannrooster.anarchy_rpg.compat.spellengine.effect;



import com.cleannrooster.anarchy_rpg.client.entity.effect.CustomEffect;
import com.cleannrooster.anarchy_rpg.compat.spellengine.magus.MagusAbilities;
import com.cleannrooster.anarchy_rpg.magic.AbilityExecutor;
import com.cleannrooster.anarchy_rpg.magic.CastTarget;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.world.ServerWorld;
import com.cleannrooster.anarchy_rpg.compat.spellengine.SpellEngineEffects;
import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Identifier;


public class Feather extends CustomEffect{
    private static final double RANGE = 64.0;

    public Feather(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        if(duration==1){
            return  true;
        }
        return super.canApplyUpdateEffect(duration, amplifier);
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getWorld() instanceof ServerWorld world) {
            // A third of the time at the bearer's target; otherwise at whatever it is looking at, out to 64.
            LivingEntity target = null;
            if (entity instanceof HostileEntity hostile && hostile.getTarget() != null && entity.getRandom().nextInt(3) == 0) {
                target = hostile.getTarget();
            } else {
                var eye = entity.getEyePos();
                var reach = entity.getRotationVector().multiply(RANGE);
                var hit = ProjectileUtil.raycast(entity, eye, eye.add(reach), entity.getBoundingBox().stretch(reach).expand(1.0),
                        candidate -> candidate instanceof LivingEntity && !candidate.isSpectator(), RANGE * RANGE);
                if (hit != null && hit.getEntity() instanceof LivingEntity living) {
                    target = living;
                }
            }
            var aim = target != null ? CastTarget.at(entity, target)
                    : CastTarget.at(entity, entity.getEyePos().add(entity.getRotationVector().multiply(RANGE)));
            AbilityExecutor.cast(world, entity, MagusAbilities.FEATHER_SHOT, aim);
        }
        entity.removeStatusEffect(SpellEngineEffects.FEATHER.registryEntry);
        if(amplifier >  0) {
            entity.addStatusEffect(new StatusEffectInstance(SpellEngineEffects.FEATHER.registryEntry, 3, amplifier-1, false, false));
        }
        return super.applyUpdateEffect(entity, amplifier);
    }
}
