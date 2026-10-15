package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.entity.ArchmageFireEntity;
import com.cleannrooster.anarchy_rpg.entity.ArtilleristEntity;
import com.cleannrooster.anarchy_rpg.entity.JuggernautEntity;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.TemplarEntity;
import com.cleannrooster.anarchy_rpg.entity.TricksterEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The one thing each recruited archetype does for its owner beyond fighting (3.0 M4d). Daily specialties
 * share one cooldown per companion: once per in-game day.
 */
public enum CompanionSpecialty {
    /** Juggernaut: guards the perimeter around the owner's spawn point. A standing order, not daily. */
    PATROL(false, ConversationOption.PATROL),
    /** Templar: removes every curse from the held item. */
    CLEANSE(true, ConversationOption.CLEANSE),
    /** Mercenary: keeps its Exchange stock and cycles its four oldest stacks each day. No option of its own. */
    TRADER(true, null),
    /** Rogue: names a rumorable place, then leads the owner to its surface. */
    GUIDE(true, ConversationOption.GUIDE),
    /** Fire Mage: turns each enchantment of the held item into a book, destroying the item. */
    DISENCHANT(true, ConversationOption.DISENCHANT),
    NONE(false, null);

    private final boolean daily;
    @Nullable
    private final ConversationOption option;

    CompanionSpecialty(boolean daily, @Nullable ConversationOption option) {
        this.daily = daily;
        this.option = option;
    }

    public boolean daily() {
        return this.daily;
    }

    /** The conversation option that uses it; null for one that works on its own (Trader) or for none. */
    public @Nullable ConversationOption option() {
        return this.option;
    }

    public static CompanionSpecialty of(MinibossEntity npc) {
        if (npc instanceof JuggernautEntity) {
            return PATROL;
        }
        if (npc instanceof TemplarEntity) {
            return CLEANSE;
        }
        if (npc instanceof ArtilleristEntity) {
            return TRADER;
        }
        if (npc instanceof TricksterEntity) {
            return GUIDE;
        }
        if (npc instanceof ArchmageFireEntity) {
            return DISENCHANT;
        }
        return NONE;
    }
}
