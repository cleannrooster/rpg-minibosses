package com.cleannrooster.anarchy_rpg.hub;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.gen.structure.Structure;

/**
 * The kinds of hub. Towers behave exactly like camps; the kind only says which structure tag found it.
 * Which structures count is data: the {@code anarchy_rpg:hub_camps} and {@code anarchy_rpg:hub_towers}
 * structure tags.
 */
public enum HubKind {
    CAMP("hub_camps"),
    TOWER("hub_towers");

    private final TagKey<Structure> tag;

    HubKind(String tag) {
        this.tag = TagKey.of(RegistryKeys.STRUCTURE, RPGMinibosses.id(tag));
    }

    public TagKey<Structure> tag() {
        return this.tag;
    }
}
