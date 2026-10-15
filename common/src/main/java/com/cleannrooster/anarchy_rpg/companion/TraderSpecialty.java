package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Mercenary, Trader (3.0 M4d step 3): a recruited Mercenary keeps its Exchange stock instead of carrying
 * storage, and trades with its owner only. Each day its oldest stacks are swapped for fresh ones; the swap
 * runs lazily, the first time the owner looks at Exchange that day, so nothing happens on a tick.
 */
public final class TraderSpecialty {

    private TraderSpecialty() {
    }

    /** Whether {@code npc} is {@code player}'s own Mercenary, which trades with them. */
    public static boolean tradesWithOwner(MinibossEntity npc, ServerPlayerEntity player) {
        return CompanionSpecialty.of(npc) == CompanionSpecialty.TRADER && Companions.recordFor(player, npc) != null;
    }

    /** Whether {@code npc} is anyone's Mercenary: its inventory is trade stock, not the owner's storage. */
    static boolean isTrader(MinibossEntity npc) {
        return npc.getOwnerUuid() != null && CompanionSpecialty.of(npc) == CompanionSpecialty.TRADER;
    }

    /** Cycles the stock if it has not been cycled today. Several missed days still cycle only once. */
    public static void cycleIfDue(MinibossEntity npc, ServerPlayerEntity player) {
        CompanionRecord record = Companions.recordFor(player, npc);
        if (record == null || CompanionSpecialties.usedToday(record, player.getServer())) {
            return;
        }
        npc.getExchangeStock().cycle(npc, player, RPGMinibossesEntities.config.companionTraderCycleCount);
        CompanionSpecialties.markUsed(record, player.getServer());
    }
}
