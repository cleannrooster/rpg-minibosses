package com.cleannrooster.rpg_minibosses.compat;

/** Client-side counterpart of {@link SpellEngineCompat}. Only ever loaded on the physical client. */
public final class SpellEngineCompatClient {
    private static final String IMPL = "com.cleannrooster.rpg_minibosses.compat.spellengine.SpellEngineContentClient";

    private SpellEngineCompatClient() {
    }

    public static void init() {
        if (SpellEngineCompat.isPresent()) {
            SpellEngineCompat.load(IMPL, Runnable.class, () -> {}).run();
        }
    }
}
