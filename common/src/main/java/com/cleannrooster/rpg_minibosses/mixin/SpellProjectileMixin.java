package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.entity.ArchmageFireEntity;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.entity.SpellProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SpellProjectile.class})
public class SpellProjectileMixin {

    @Shadow
    private RegistryEntry<Spell> spellEntry;

    @Inject(
            at = {@At("HEAD")},
            method = {"tick"},
            cancellable = true
    )
    public void tickMagusProj(CallbackInfo info) {
        SpellProjectile projectile = (SpellProjectile) (Object) this;
        Entity owner = projectile.getOwner();
        if (owner instanceof MagusPrimeEntity magus) {
            // Snap dark matter projectiles to the orb on first tick
            if (projectile.age <= 1
                    && spellEntry != null
                    && spellEntry.getIdAsString().equals("rpg_minibosses:magus_dark_matter_proj")
                    && magus.activeDarkMatterOrb != null
                    && !magus.activeDarkMatterOrb.isRemoved()) {
                OrbEntity orb = magus.activeDarkMatterOrb;
                projectile.setPosition(orb.getX(), orb.getY() + 0.5, orb.getZ());
            }
            if (projectile.age == 12
                    && (spellEntry == null || !spellEntry.getIdAsString().equals("rpg_minibosses:magus_dark_matter_proj"))) {
                projectile.setFollowedTarget(null);
            }
        }
        if (owner instanceof ArchmageFireEntity) {
            if (projectile.age == 12) {
                projectile.setFollowedTarget(null);
            }
        }
    }

    @Inject(
            at = {@At("HEAD")},
            method = {"onEntityHit"}
    )
    public void onDarkMatterProjHit(EntityHitResult hitResult, CallbackInfo info) {
        SpellProjectile projectile = (SpellProjectile) (Object) this;
        if ((spellEntry == null || !spellEntry.getIdAsString().equals("rpg_minibosses:magus_dark_matter_proj"))) return;
            Entity owner = projectile.getOwner();
        if (!(owner instanceof MagusPrimeEntity magus)) return;
        if (!(hitResult.getEntity() instanceof PlayerEntity)) return;
        OrbEntity orb = magus.activeDarkMatterOrb;
        if (orb != null && !orb.isRemoved()) {
            orb.onProjectileHit();
        }
    }
}
