package com.cleannrooster.rpg_minibosses.compat;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;

/**
 * The only door from the core mod into Structure Pool API, which adds the wanted-poster house to
 * villages. Optional: without it villages simply have no poster house.
 *
 * <p>As with {@link SpellEngineCompat}, no class outside {@code compat.structurepool} may name a
 * {@code net.fabric_extras.structure_pool} type, and presence is tested by resource lookup, which
 * initialises nothing.</p>
 */
public final class StructurePoolCompat {
    /** A class shipped by Structure Pool API; its presence is the gate. */
    static final String PROBE = "net/fabric_extras/structure_pool/api/StructurePoolAPI.class";
    private static final String IMPL = "com.cleannrooster.rpg_minibosses.compat.structurepool.VillagePosters";

    private static Boolean present;
    private static Hooks hooks;

    private StructurePoolCompat() {
    }

    public static boolean isPresent() {
        if (present == null) {
            present = StructurePoolCompat.class.getClassLoader().getResource(PROBE) != null;
        }
        return present;
    }

    public static Hooks hooks() {
        if (hooks == null) {
            hooks = isPresent() ? SpellEngineCompat.load(IMPL, Hooks.class, NONE) : NONE;
            if (!isPresent()) {
                RPGMinibosses.LOGGER.info("[{}] Structure Pool API not installed; villages get no wanted-poster house",
                        RPGMinibosses.CONTENT_NAMESPACE);
            }
        }
        return hooks;
    }

    public interface Hooks {
        /** Load (or create) the {@code villages} config. */
        void initConfig();

        /** Add the poster house to the configured village pools. */
        void injectVillages();
    }

    private static final Hooks NONE = new Hooks() {
        @Override
        public void initConfig() {
        }

        @Override
        public void injectVillages() {
        }
    };
}
