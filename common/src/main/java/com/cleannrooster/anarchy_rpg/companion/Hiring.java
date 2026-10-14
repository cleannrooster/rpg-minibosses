package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.exchange.ExchangePricing;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import com.cleannrooster.anarchy_rpg.faction.FactionSlot;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import com.cleannrooster.anarchy_rpg.faction.StandingLedger;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Recruiting by paying, the other way to gain a companion besides sparing a defeated one.
 *
 * <p>The price is {@code companionHirePrice} scaled by the same standing curve Exchange prices use: more
 * expensive the less the adventurer's faction thinks of you, cheaper toward devoted. Wary adventurers will
 * not be hired at all, and hostile ones only by defeat (they will not talk).
 */
public final class Hiring {

    private Hiring() {
    }

    public static double price(MinibossEntity npc, ServerPlayerEntity player) {
        FactionSlot faction = Factions.factionOf(npc);
        int standing = faction == null ? 0 : StandingLedger.get(player.getServer()).get(player.getUuid(), faction);
        return RPGMinibossesEntities.config.companionHirePrice * ExchangePricing.dispositionFactor(standing);
    }

    /** Why this player cannot hire this adventurer right now, or null if they can. */
    public static @Nullable Text whyNot(MinibossEntity npc, ServerPlayerEntity player, Disposition disposition) {
        if (npc.getCantHire()) {
            return reason("not_for_hire");
        }
        if (disposition == Disposition.WARY) {
            return reason("wary");
        }
        if (CompanionRegistry.get(player.getServer()).ofOwner(player.getUuid()) != null) {
            return Text.translatable("companion." + RPGMinibosses.CONTENT_NAMESPACE + ".already_have");
        }
        return null;
    }

    private static Text reason(String key) {
        return Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".hire." + key);
    }
}
