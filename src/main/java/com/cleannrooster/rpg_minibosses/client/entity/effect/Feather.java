package com.cleannrooster.rpg_minibosses.client.entity.effect;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;


public class Feather extends CustomEffect{
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
        Entity target;
        if(entity instanceof HostileEntity hostile && hostile.getTarget() != null && entity.getRandom().nextInt(3)==0){
            target = hostile.getTarget();

        }
        else{
           target = TargetHelper.targetFromRaycast(entity,SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"firefeather2")).range, entity1 -> true);

        }
        SpellHelper.shootProjectile(entity.getWorld(),entity,target
                        , SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID,"firefeather2")).get(),
                new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE,entity)).channeled(1.0F).distance(1.0F));
        entity.removeStatusEffect(Effects.FEATHER.registryEntry);
        if(amplifier >  0) {
            entity.addStatusEffect(new StatusEffectInstance(Effects.FEATHER.registryEntry, 3, amplifier-1, false, false));
        }
        return super.applyUpdateEffect(entity, amplifier);
    }
}
