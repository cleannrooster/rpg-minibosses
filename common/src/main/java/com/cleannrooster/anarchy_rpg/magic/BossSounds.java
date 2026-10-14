package com.cleannrooster.anarchy_rpg.magic;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Sound events for boss abilities. Code only ever names these keys; which files back each one is decided
 * in {@code sounds.json} alone (written by {@code tools/convert_sounds.py}, safe to hand-edit), so
 * swapping a sound never touches Java. Every event is {@code anarchy_rpg:boss.<key>}.
 *
 * <p>Each school has a wind-up, a release and an impact (and some a loop), mirroring the
 * casting / release / impact sounds Spell Engine's spells used, so an ability can ask for
 * {@code BossSounds.of(school).release()} without knowing the school's files.</p>
 */
public final class BossSounds {
    private static final List<SoundEvent> ALL = new ArrayList<>();

    /** The sounds of one school. {@code loop} is null for schools without a held-cast loop. */
    public record SchoolSounds(SoundEvent windup, SoundEvent release, SoundEvent impact, @Nullable SoundEvent loop) {
    }

    public static final SchoolSounds FIRE = school("fire", true);
    public static final SchoolSounds FROST = school("frost", true);
    public static final SchoolSounds LIGHTNING = school("lightning", true);
    public static final SchoolSounds ARCANE = school("arcane", false);
    public static final SchoolSounds SOUL = school("soul", true);
    public static final SchoolSounds HEALING = school("healing", false);

    public static final SoundEvent FIRE_IGNITE = event("fire.ignite");
    public static final SoundEvent FROST_FREEZE = event("frost.freeze");

    public static final SoundEvent SWING = event("physical.swing");
    public static final SoundEvent HEAVY_SWING = event("physical.heavy_swing");
    public static final SoundEvent HIT = event("physical.hit");
    public static final SoundEvent HEAVY_HIT = event("physical.heavy_hit");
    public static final SoundEvent DAGGER_THROW = event("physical.dagger_throw");
    public static final SoundEvent DAGGER_HIT = event("physical.dagger_hit");
    public static final SoundEvent BOW_DRAW = event("physical.bow_draw");
    public static final SoundEvent BOW_SHOOT = event("physical.bow_shoot");
    public static final SoundEvent BOW_HIT = event("physical.bow_hit");

    public static final SoundEvent TELEPORT_OUT = event("teleport.out");
    public static final SoundEvent TELEPORT_IN = event("teleport.in");
    public static final SoundEvent SHIELD = event("shield");
    public static final SoundEvent EXPLOSION = event("explosion");

    /** Physical schools reuse the weapon sounds in the same three slots. */
    public static final SchoolSounds MELEE = new SchoolSounds(SWING, HEAVY_SWING, HIT, null);
    public static final SchoolSounds RANGED = new SchoolSounds(BOW_DRAW, BOW_SHOOT, BOW_HIT, null);

    private static final Map<MagicSchool, SchoolSounds> BY_SCHOOL = new EnumMap<>(MagicSchool.class);

    static {
        BY_SCHOOL.put(MagicSchool.GENERIC, ARCANE);
        BY_SCHOOL.put(MagicSchool.ARCANE, ARCANE);
        BY_SCHOOL.put(MagicSchool.FIRE, FIRE);
        BY_SCHOOL.put(MagicSchool.FROST, FROST);
        BY_SCHOOL.put(MagicSchool.HEALING, HEALING);
        BY_SCHOOL.put(MagicSchool.LIGHTNING, LIGHTNING);
        BY_SCHOOL.put(MagicSchool.SOUL, SOUL);
        BY_SCHOOL.put(MagicSchool.PHYSICAL_MELEE, MELEE);
        BY_SCHOOL.put(MagicSchool.PHYSICAL_RANGED, RANGED);
    }

    private BossSounds() {
    }

    public static SchoolSounds of(MagicSchool school) {
        return BY_SCHOOL.get(school);
    }

    /** SOUND_EVENT phase. */
    public static void register() {
        for (SoundEvent event : ALL) {
            Registry.register(Registries.SOUND_EVENT, event.getId(), event);
        }
    }

    private static SchoolSounds school(String name, boolean hasLoop) {
        return new SchoolSounds(event(name + ".windup"), event(name + ".release"), event(name + ".impact"),
                hasLoop ? event(name + ".loop") : null);
    }

    private static SoundEvent event(String key) {
        var event = SoundEvent.of(RPGMinibosses.id("boss." + key));
        ALL.add(event);
        return event;
    }
}
