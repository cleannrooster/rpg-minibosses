package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import dev.emi.trinkets.mixin.LivingEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.mixin.client.render.EntityMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SpellProjectile.class})
public class SpellProjectileMixin {
    @Inject(
            at = {@At("HEAD")},
            method = {"tick"},
            cancellable = true
    )
    public void tickMagusProj(CallbackInfo info) {
        SpellProjectile projectile = (SpellProjectile) (Object) this;
        Entity var4 = projectile.getOwner();
        if (var4 instanceof MagusPrimeEntity magusPrimeEntity) {
            if (projectile.age == 12) {
                projectile.setFollowedTarget(null);
            }
        }

    }
}
