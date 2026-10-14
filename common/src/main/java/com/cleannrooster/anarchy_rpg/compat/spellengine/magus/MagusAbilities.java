package com.cleannrooster.anarchy_rpg.compat.spellengine.magus;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.compat.spellengine.SpellEngineEffects;
import com.cleannrooster.anarchy_rpg.magic.AbilitySpec;
import com.cleannrooster.anarchy_rpg.magic.Delivery;
import com.cleannrooster.anarchy_rpg.magic.Impact;
import com.cleannrooster.anarchy_rpg.magic.MagicSchool;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Forsaken Magus's kit, ported from its Spell Engine spells with their numbers. Magus still only exists
 * with Spell Engine (players' spells breaking its barrier is the fight), so its damage still lands as Spell
 * Power's types; it just no longer needs the spell JSONs.
 *
 * <p>Novas used only their impacts, struck on everything within six blocks; projectiles used their full
 * delivery. Every Magus projectile but the dark-matter bolt gave up homing at tick 12. Dropped from the
 * spells: bounce, ricochet, chain-reaction and distance falloff perks.</p>
 */
public final class MagusAbilities {
    private MagusAbilities() {
    }

    // ── Novas: everything within six blocks ─────────────────────────────────────────────────────

    private static final Delivery NOVA_AREA = new Delivery.Area(new TelegraphArea.Circle(Vec3d.ZERO, 6.0f), Delivery.Anchor.CASTER, 4.0);

    public static final AbilitySpec FIRE_NOVA = nova("fire_nova", MagicSchool.FIRE)
            .damage(0.6f).impact(new Impact.Ignite(3.0f)).build();
    /** Hits, and armours Magus in frost for 8 s (the armour that answers melee with ice bolts). */
    public static final AbilitySpec ARCTIC_ARMOR = nova("arctic_armor", MagicSchool.FROST)
            .damage(1.0f).impact(new Impact.ApplyToCaster(SpellEngineEffects.ARCTICARMOR.registryEntry, 160, 0)).build();
    /** Hits, burns, and starts Magus's feather barrage (Feather, amplifier 7, 4 s). */
    public static final AbilitySpec PHOENIX_NOVA = nova("phoenix_nova", MagicSchool.FIRE)
            .damage(1.2f).impact(new Impact.ApplyToCaster(SpellEngineEffects.FEATHER.registryEntry, 80, 7))
            .impact(new Impact.Ignite(3.0f)).build();
    public static final AbilitySpec ARCANE_NOVA = nova("arcane_nova", MagicSchool.ARCANE).damage(1.0f).build();
    /** Hits, and slows Magus itself (amplifier 4, 4 s) — as the spell was written. */
    public static final AbilitySpec DEATHCHILL_NOVA = nova("deathchill_nova", MagicSchool.FROST)
            .damage(0.8f).impact(new Impact.ApplyToCaster(StatusEffects.SLOWNESS, 80, 4)).build();
    public static final AbilitySpec FROSTFERNO = nova("frostferno", MagicSchool.FROST)
            .impact(Impact.Damage.as(MagicSchool.FROST, 0.8f, 0.0f))
            .impact(Impact.Damage.as(MagicSchool.FIRE, 0.4f, 0.0f))
            .impact(new Impact.Ignite(80.0f)).build();
    public static final AbilitySpec SELF_IMMOLATE = nova("self_immolate", MagicSchool.FIRE)
            .impact(new Impact.Damage(2.0f, 2.0f, true)).build();
    public static final AbilitySpec RAIN_OF_FIRE = nova("rain_of_fire", MagicSchool.FIRE)
            .impact(new Impact.Damage(0.8f, 2.0f, true)).build();
    public static final AbilitySpec SUPERNOVA = nova("supernova", MagicSchool.ARCANE)
            .impact(Impact.Damage.as(MagicSchool.FIRE, 1.0f, 2.0f))
            .impact(Impact.Damage.as(MagicSchool.ARCANE, 1.0f, 2.0f)).build();

    public static final List<AbilitySpec> SHORT_NOVAS = List.of(FIRE_NOVA, ARCTIC_ARMOR);
    public static final List<AbilitySpec> LONG_NOVAS = List.of(PHOENIX_NOVA, ARCANE_NOVA, DEATHCHILL_NOVA, FROSTFERNO,
            SELF_IMMOLATE, RAIN_OF_FIRE, SUPERNOVA);

    // ── Shockwaves: a 4-block burst under the target, 25 ticks after it is marked ──────────────
    // Both were scaled by Magus's *healing* power (which it does not have), as the spells were cast.

    public static final AbilitySpec LIGHTNING_FALL = shockwave("lightning_fall", MagicSchool.HEALING);
    public static final AbilitySpec SOUL_BURST = shockwave("soul_burst", MagicSchool.SOUL);
    public static final List<AbilitySpec> SHOCKWAVES = List.of(LIGHTNING_FALL, SOUL_BURST);
    public static final int SHOCKWAVE_DELAY = 25;

    // ── Projectiles ─────────────────────────────────────────────────────────────────────────────

    /** Eight bolts fanned up and around, homing 20°/tick from tick 4 to 12, 4-block bursts. */
    public static final AbilitySpec FIRE_VOLLEY = AbilitySpec.builder(id("fire_volley"), MagicSchool.FIRE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.0).homing(20.0f, 4).homingUntil(12)
                    .withLifetime(120).bursting(new TelegraphArea.Circle(Vec3d.ZERO, 4.0f), 3.0)))
            .impact(new Impact.Damage(0.25f, 2.0f, true)).build();
    /** Eight bolts out to either side, homing 30°/tick from tick 4 to 12. */
    public static final AbilitySpec ARCANE_PROJECTILE = AbilitySpec.builder(id("arcane_projectile"), MagicSchool.ARCANE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.0).homing(30.0f, 4).homingUntil(12).withLifetime(120)))
            .impact(new Impact.Damage(0.2f, 0.0f, true)).build();
    /** Sixteen bolts as above, each dealing arcane and frost. Also what Arctic Armor answers melee with. */
    public static final AbilitySpec ICE_BOLT = AbilitySpec.builder(id("ice_bolt"), MagicSchool.FROST)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.0).homing(30.0f, 4).homingUntil(12).withLifetime(120)))
            .impact(Impact.Damage.as(MagicSchool.ARCANE, 0.2f, 0.0f))
            .impact(Impact.Damage.as(MagicSchool.FROST, 0.2f, 0.0f)).build();
    public static final List<AbilitySpec> SHORT_PROJECTILES = List.of(FIRE_VOLLEY, ARCANE_PROJECTILE, ICE_BOLT);

    public static final AbilitySpec GREATER_FIREBALL = AbilitySpec.builder(id("greater_fireball"), MagicSchool.FIRE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(3.0).homing(30.0f, 2).homingUntil(12)
                    .withLifetime(40).bursting(new TelegraphArea.Circle(Vec3d.ZERO, 8.0f), 4.0)))
            .impact(new Impact.Damage(1.8f, 2.0f, true)).build();
    public static final AbilitySpec AMETHYST_CHUNK = AbilitySpec.builder(id("amethyst_chunk"), MagicSchool.ARCANE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.0).withLifetime(80)))
            .impact(new Impact.Damage(0.6f, 0.0f, true)).build();
    /** Bursts over eight blocks, heavy knockback, and slows Magus (amplifier 2, 8 s) as the spell did. */
    public static final AbilitySpec ICE_CHUNK = AbilitySpec.builder(id("ice_chunk"), MagicSchool.FROST)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.6).withLifetime(30)
                    .bursting(new TelegraphArea.Circle(Vec3d.ZERO, 8.0f), 4.0)))
            .impact(new Impact.Damage(1.4f, 3.0f, true))
            .impact(new Impact.ApplyToCaster(StatusEffects.SLOWNESS, 160, 2)).build();
    /** Lobbed up at 45°, homing 20°/tick from tick 2 to 12, fire and arcane over four blocks. */
    public static final AbilitySpec PLASMA_BLAST = AbilitySpec.builder(id("plasma_blast"), MagicSchool.ARCANE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(2.0).homing(20.0f, 2).homingUntil(12)
                    .withLifetime(60).bursting(new TelegraphArea.Circle(Vec3d.ZERO, 4.0f), 3.0)))
            .impact(Impact.Damage.as(MagicSchool.FIRE, 0.8f, 0.5f))
            .impact(Impact.Damage.as(MagicSchool.ARCANE, 0.8f, 0.5f)).build();
    public static final List<AbilitySpec> LONG_PROJECTILES = List.of(GREATER_FIREBALL, AMETHYST_CHUNK, ICE_CHUNK, PLASMA_BLAST);

    /**
     * How a projectile spell is launched: {@code count} shots, the {@code i}th turned by
     * {@code offsets[i % offsets.length]} ({yaw, pitch} in degrees) off the line to the target.
     */
    public record Volley(float[][] offsets, int count) {
    }

    private static final float[][] AIMED = {{0.0f, 0.0f}};
    private static final float[][] FAN_UP = {{45.0f, -45.0f}, {-45.0f, -45.0f}, {135.0f, -45.0f}, {-135.0f, -45.0f}};
    private static final float[][] FAN_SIDE = {{-120.0f, 0.0f}, {-120.0f, -60.0f}, {120.0f, -60.0f}, {120.0f, 0.0f}};

    public static Volley volleyOf(AbilitySpec spec) {
        if (spec == FIRE_VOLLEY) return new Volley(FAN_UP, 8);
        if (spec == ARCANE_PROJECTILE) return new Volley(FAN_SIDE, 8);
        if (spec == ICE_BOLT) return new Volley(FAN_SIDE, 16);
        if (spec == PLASMA_BLAST) return new Volley(new float[][]{{0.0f, -45.0f}}, 1);
        return new Volley(AIMED, 1);
    }

    // ── Other ───────────────────────────────────────────────────────────────────────────────────

    /** One Fire Dominion patch: 2.5 blocks of fire for 16 s, 0.5 × fire power every 10 ticks. */
    public static final AbilitySpec FIRE_DOMINION = AbilitySpec.builder(id("fire_dominion"), MagicSchool.FIRE)
            .delivery(new Delivery.Cloud(new Delivery.CloudSpec(new TelegraphArea.Circle(Vec3d.ZERO, 2.5f), 0.75, 16 * 20, 10),
                    Delivery.Anchor.TARGET))
            .impact(new Impact.Damage(0.5f, 0.0f, true)).build();

    /** A dark-matter bolt from the orb: homes 10°/tick after two ticks for its whole flight, 0.8 × soul. */
    public static final AbilitySpec DARK_MATTER_BOLT = AbilitySpec.builder(id("dark_matter_bolt"), MagicSchool.SOUL)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.0).homing(10.0f, 2).withLifetime(120)))
            .impact(new Impact.Damage(0.8f, 0.0f, true)).build();

    /** One feather from the Feather effect: 1.6 b/t, homing 60°/tick from tick 5 to 12, 0.2 × fire, 3 s burn. */
    public static final AbilitySpec FEATHER_SHOT = AbilitySpec.builder(id("feather"), MagicSchool.FIRE)
            .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.6).homing(60.0f, 5).homingUntil(12).withLifetime(40)))
            .impact(new Impact.Damage(0.2f, 0.0f, true))
            .impact(new Impact.Ignite(3.0f)).build();

    private static AbilitySpec.Builder nova(String name, MagicSchool school) {
        return AbilitySpec.builder(id(name), school).delivery(NOVA_AREA);
    }

    private static AbilitySpec shockwave(String name, MagicSchool damage) {
        return AbilitySpec.builder(id(name), MagicSchool.HEALING)
                .delivery(new Delivery.Area(new TelegraphArea.Circle(Vec3d.ZERO, 4.0f), Delivery.Anchor.TARGET, 3.0))
                .impact(Impact.Damage.as(damage, 0.6f, 1.0f))
                .looksLike(damage == MagicSchool.SOUL ? MagicSchool.SOUL : MagicSchool.LIGHTNING)
                .build();
    }

    private static net.minecraft.util.Identifier id(String name) {
        return RPGMinibosses.id("magus/" + name);
    }
}
