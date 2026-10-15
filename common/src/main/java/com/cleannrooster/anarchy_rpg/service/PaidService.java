package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.text.Text;

/**
 * Something an adventurer does for payment, bought through the shared {@link PaidServiceScreenHandler} window:
 * the player puts the currency (or anything with an Exchange value) in, and confirms once it covers the price.
 *
 * <p>The window owns what every service shares: quoting, refusing an offer that falls short, consuming the
 * payment, and the trade sound. A service supplies only its own rules.
 */
public interface PaidService {

    /** The price, in currency, for this player. Always re-read at the moment of paying. */
    double price(MinibossEntity npc, ServerPlayerEntity player);

    /** Why this player cannot buy it right now, or null if they can. Checked again at the moment of paying. */
    @Nullable
    Text whyNot(MinibossEntity npc, ServerPlayerEntity player);

    /**
     * Does the thing, once the payment covers the price and {@link #whyNot} is clear. Returns false to refuse
     * after all, in which case nothing is taken.
     */
    boolean perform(MinibossEntity npc, ServerPlayerEntity player);

    /** The payment window's title: the adventurer's name, unless the service names what is being bought. */
    default Text windowTitle(MinibossEntity npc, ServerPlayerEntity player) {
        return npc.getName();
    }

    /** Whether the window closes once it is done (Hire does: the adventurer is yours and walks off with you). */
    default boolean closesAfter() {
        return true;
    }
}
