package com.cleannrooster.rpg_minibosses.client.entity.effect;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.WorldProperties;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;


public class DarkMatter extends CustomEffect{
    public DarkMatter(StatusEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {

        return super.canApplyUpdateEffect(duration, amplifier);
    }

    @Override
    public void playApplySound(LivingEntity entity, int amplifier) {
        if (!entity.getWorld().isClient) {
            entity.getWorld().playSound(
                    null, // Player - if non-null, will play sound for every nearby player *except* the specified player
                    entity.getBlockPos(), // The position of where the sound will come from
                    RPGMinibosses.EXPLOSION_SOUND, // The sound that will play, in this case, the sound the anvil plays when it lands.
                    SoundCategory.HOSTILE, // This determines which of the volume sliders affect this sound
                    1f, //Volume multiplier, 1 is normal, 0.5 is half volume, etc
                    1f // Pitch multiplier, 1 is normal, 0.5 is half pitch, etc
            );
        }
        super.playApplySound(entity, amplifier);
    }



}
