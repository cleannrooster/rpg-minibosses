package com.cleannrooster.rpg_minibosses.compat;

/** Client-side counterpart of {@link SpellEngineCompat}. Only ever loaded on the physical client. */
public final class SpellEngineCompatClient {
    private static final String IMPL = "com.cleannrooster.rpg_minibosses.compat.spellengine.SpellEngineContentClient";
    private static final String EARLY_IMPL = "com.cleannrooster.rpg_minibosses.compat.spellengine.SpellEngineEntityRenderers";

    private SpellEngineCompatClient() {
    }

    /** Mod-construction work: entity renderers (see {@code RPGMinibossesClient.registerEntityRenderers}). */
    public static void earlyInit() {
        if (SpellEngineCompat.isPresent()) {
            SpellEngineCompat.load(EARLY_IMPL, Runnable.class, () -> {}).run();
        }
    }

    public static void init() {
        if (SpellEngineCompat.isPresent()) {
            SpellEngineCompat.load(IMPL, Runnable.class, () -> {}).run();
        }
    }
}
