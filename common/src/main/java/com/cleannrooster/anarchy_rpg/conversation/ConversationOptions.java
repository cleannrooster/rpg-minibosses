package com.cleannrooster.anarchy_rpg.conversation;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumMap;
import java.util.Map;

/**
 * The options conversations offer, each with the rule that decides whether it shows and the handler that
 * runs when it is picked. Later milestones register their options here (Exchange, Hire, Rumors, the
 * companion orders); an option with nothing registered never appears.
 */
public final class ConversationOptions {

    /** Decides, on the server, whether an option shows for this adventurer and player. */
    @FunctionalInterface
    public interface Rule {
        OptionAvailability check(MinibossEntity npc, ServerPlayerEntity player, Disposition disposition);
    }

    /**
     * Runs a picked option. {@code argument} is the option's setting where it has one (Wait: hours), as
     * sent by the client: clamp it. Returns whether the conversation stays open afterwards.
     */
    @FunctionalInterface
    public interface Handler {
        boolean choose(MinibossEntity npc, ServerPlayerEntity player, int argument);
    }

    record Registration(Rule rule, Handler handler) {
    }

    private static final Map<ConversationOption, Registration> REGISTRY = new EnumMap<>(ConversationOption.class);

    private ConversationOptions() {
    }

    public static void register(ConversationOption option, Rule rule, Handler handler) {
        REGISTRY.put(option, new Registration(rule, handler));
    }

    /** In display order, since the map is keyed by the enum. */
    static Map<ConversationOption, Registration> all() {
        return REGISTRY;
    }

    static Registration get(ConversationOption option) {
        return REGISTRY.get(option);
    }
}
