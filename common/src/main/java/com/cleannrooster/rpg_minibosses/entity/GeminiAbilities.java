package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.magic.AbilitySpec;
import com.cleannrooster.rpg_minibosses.magic.Delivery;
import com.cleannrooster.rpg_minibosses.magic.Impact;
import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

/**
 * The Gemini twins' kit and the Deatomization Storm, ported from their Spell Engine spells with the same
 * numbers. Every damage impact hits through invulnerability frames, as Spell Engine's did.
 */
public final class GeminiAbilities {
    private GeminiAbilities() {
    }

    /**
     * {@code beam} / {@code beam_cold}: everything within 22.5° of the line to the target out to 48 blocks
     * takes three hits of 1.6 × the djinn's <em>lightning</em> power — its own element, physical and
     * lightning — with full knockback. Lightning power for all three is how the spell was registered.
     */
    public static final AbilitySpec BEAM_FIRE = beam("beam", MagicSchool.FIRE);
    public static final AbilitySpec BEAM_FROST = beam("beam_cold", MagicSchool.FROST);

    /** {@code comet}: a frost shard from ten blocks up, bursting over four for 2 × frost power, no knockback. */
    public static final AbilitySpec COMET = AbilitySpec.builder(RPGMinibosses.id("gemini/comet"), MagicSchool.FROST)
            .delivery(new Delivery.Meteor(Delivery.ProjectileSpec.straight(0.4).withLifetime(60)
                    .bursting(new TelegraphArea.Circle(Vec3d.ZERO, 4.0f), 3.0), 10.0))
            .impact(new Impact.Damage(2.0f, 0.0f, true))
            .build();
    public static final double COMET_SPREAD = 1.3;

    /**
     * {@code starfall}: one star from twenty blocks up, bursting over three for 1 × arcane power dealt as
     * fire and again as arcane, light knockback. The twin calls four, each of which falls as four stars
     * twenty ticks apart, as the spell's extra launches did.
     */
    public static final AbilitySpec STARFALL = AbilitySpec.builder(RPGMinibosses.id("gemini/starfall"), MagicSchool.ARCANE)
            .delivery(new Delivery.Meteor(Delivery.ProjectileSpec.straight(0.4).withLifetime(80)
                    .bursting(new TelegraphArea.Circle(Vec3d.ZERO, 3.0f), 3.0), 20.0))
            .impact(Impact.Damage.as(MagicSchool.FIRE, 1.0f, 0.5f))
            .impact(Impact.Damage.as(MagicSchool.ARCANE, 1.0f, 0.5f))
            .build();
    public static final int STARFALL_STARS = 4;
    public static final int STARFALL_INTERVAL = 20;
    public static final double STARFALL_SPREAD = 5.8;

    /** Ticks between a cloud being called and it appearing (the spell's delivery delay). */
    public static final int CLOUD_DELAY = 10;

    /** {@code flame_cloud}: two blocks of fire for 24 s, 0.8 × fire power every 8 ticks. */
    public static final AbilitySpec FLAME_CLOUD = AbilitySpec.builder(RPGMinibosses.id("gemini/flame_cloud"), MagicSchool.FIRE)
            .delivery(cloud())
            .impact(new Impact.Damage(0.8f, 0.0f, true))
            .build();

    /** {@code frost_cloud}: two blocks of frost for 24 s, slowing for 2 s and dealing 0.4 × frost power every 8 ticks. */
    public static final AbilitySpec FROST_CLOUD = AbilitySpec.builder(RPGMinibosses.id("gemini/frost_cloud"), MagicSchool.FROST)
            .delivery(cloud())
            .impact(new Impact.ApplyEffect(StatusEffects.SLOWNESS, 40, 0))
            .impact(new Impact.Damage(0.4f, 0.0f, true))
            .build();

    /**
     * {@code deatomization_storm}: one pulse of the storm, 0.8 × arcane power dealt as arcane and again as
     * lightning, no knockback. The storm picks its own targets; this is what each of them takes.
     */
    public static final AbilitySpec DEATOMIZATION = AbilitySpec.builder(RPGMinibosses.id("gemini/deatomization_storm"), MagicSchool.ARCANE)
            .impact(Impact.Damage.as(MagicSchool.ARCANE, 0.8f, 0.0f))
            .impact(Impact.Damage.as(MagicSchool.LIGHTNING, 0.8f, 0.0f))
            .build();

    public static AbilitySpec beam(boolean frost) {
        return frost ? BEAM_FROST : BEAM_FIRE;
    }

    private static AbilitySpec beam(String name, MagicSchool element) {
        return AbilitySpec.builder(RPGMinibosses.id("gemini/" + name), MagicSchool.LIGHTNING)
                .delivery(new Delivery.Beam(48.0, 22.5f))
                .impact(Impact.Damage.as(element, 1.6f, 1.0f))
                .impact(Impact.Damage.as(MagicSchool.PHYSICAL_MELEE, 1.6f, 1.0f))
                .impact(Impact.Damage.as(MagicSchool.LIGHTNING, 1.6f, 1.0f))
                .looksLike(element)
                .build();
    }

    private static Delivery.Cloud cloud() {
        return new Delivery.Cloud(new Delivery.CloudSpec(new TelegraphArea.Circle(Vec3d.ZERO, 2.0f), 0.6, 24 * 20, 8),
                Delivery.Anchor.TARGET);
    }
}
