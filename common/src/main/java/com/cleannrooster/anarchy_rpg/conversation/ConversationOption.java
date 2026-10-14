package com.cleannrooster.anarchy_rpg.conversation;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Everything a player can ask of an adventurer in conversation. An option appears only once a handler
 * is registered for it ({@link ConversationOptions}); options for later milestones stay hidden until then.
 * Declaration order is display order.
 */
public enum ConversationOption {
    EXCHANGE,
    RUMORS,
    HIRE,
    COMPANION_GEAR,
    FOLLOW,
    STAY,
    GO_TO_BASE,
    SET_RENDEZVOUS_HERE,
    DISMISS;

    private static final ConversationOption[] VALUES = values();

    public Text label() {
        return Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".option." + this.name().toLowerCase(Locale.ROOT));
    }

    public static ConversationOption fromIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : null;
    }
}
