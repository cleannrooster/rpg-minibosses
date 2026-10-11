package com.cleannrooster.rpg_minibosses.compat;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * The only door from the core mod into Spell Engine content.
 *
 * <p>Everything that needs Spell Engine or Spell Power lives under {@code compat.spellengine} and is
 * reached through {@link Hooks}, whose implementation is loaded reflectively by name. No class outside
 * {@code compat.spellengine} may name a {@code net.spell_engine} / {@code net.spell_power} type — not even
 * in a guarded branch, because verifying the enclosing method can load the referenced class.</p>
 *
 * <p>Presence is tested by resource lookup rather than a loader mod-id query: it answers correctly at
 * any lifecycle point (including mixin-plugin time, where NeoForge's mod list is not ready yet) and
 * does not initialise any Spell Engine class.</p>
 */
public final class SpellEngineCompat {
    /** A class shipped by Spell Engine; its presence is the gate. */
    static final String PROBE = "net/spell_engine/api/spell/Spell.class";
    private static final String IMPL = "com.cleannrooster.rpg_minibosses.compat.spellengine.SpellEngineContent";

    private static Boolean present;
    private static Hooks hooks;

    private SpellEngineCompat() {
    }

    public static boolean isPresent() {
        if (present == null) {
            present = SpellEngineCompat.class.getClassLoader().getResource(PROBE) != null;
        }
        return present;
    }

    /** The Spell Engine content hooks, or {@link #NONE} when Spell Engine is absent. */
    public static Hooks hooks() {
        if (hooks == null) {
            hooks = isPresent() ? load(IMPL, Hooks.class, NONE) : NONE;
        }
        return hooks;
    }

    static <T> T load(String className, Class<T> type, T fallback) {
        try {
            return type.cast(Class.forName(className).getDeclaredConstructor().newInstance());
        } catch (Throwable t) {
            RPGMinibosses.LOGGER.error("[{}] Spell Engine is installed but {} failed to load; its content is disabled",
                    RPGMinibosses.CONTENT_NAMESPACE, className, t);
            return fallback;
        }
    }

    /**
     * Calls are made from the matching {@link RPGMinibosses} phase, so on NeoForge each runs inside the
     * same {@code RegisterEvent} as the core content it sits beside.
     */
    public interface Hooks {
        /** Load Spell Engine-only config files. Runs before any registry phase. */
        void initConfig();

        /** Non-registry setup: spell handlers, model ids, events, config save. */
        void init();

        /** Construct Spell Engine-only boss entries so the core entity loop registers them. */
        void declareEntities();

        /** ENTITY_TYPE phase, after the core boss entries: helper entities and loot injection. */
        void registerEntities();

        /** STATUS_EFFECT phase, before the core effect loop: construct Spell Engine-only effect entries. */
        void declareEffects();

        /** ITEM phase: summon items, registered alongside the core summon items. */
        void registerSummonItems();

        /** ITEM phase: weapons, armor sets and their creative tab. */
        void registerEquipment();

        /** An uber Gemini died: drop its spell scrolls. */
        void onGeminiUberDeath(LivingEntity gemini, ServerWorld world);
    }

    public static final Hooks NONE = new Hooks() {
        @Override public void initConfig() {}
        @Override public void init() {}
        @Override public void declareEntities() {}
        @Override public void registerEntities() {}
        @Override public void declareEffects() {}
        @Override public void registerSummonItems() {}
        @Override public void registerEquipment() {}
        @Override public void onGeminiUberDeath(LivingEntity gemini, ServerWorld world) {}
    };
}
