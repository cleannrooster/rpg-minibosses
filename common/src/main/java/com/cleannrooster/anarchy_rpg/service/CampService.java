package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import com.cleannrooster.anarchy_rpg.faction.StandingCause;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * A camp service: one archetype's paid, lesser specialty. Subclasses give their kind, base price, daily uses,
 * any rules of their own, and the effect; this class adds the shared camp rules ({@link CampServices}) and,
 * once the effect has happened, counts the day's use and the trade toward the guild's standing.
 */
public abstract class CampService implements PaidService {

    protected abstract PaidServiceKind kind();

    protected abstract double basePrice(ServerConfig config);

    /** How many times a day each player may buy it from one adventurer. */
    protected int dailyUses() {
        return 1;
    }

    /** Rules of this service's own (an item to work on, say), checked after the shared ones. Null if none. */
    protected @Nullable Text ownWhyNot(MinibossEntity npc, ServerPlayerEntity player) {
        return null;
    }

    /** The effect, once paid for. Returns false to refuse after all; nothing is then taken or counted. */
    protected abstract boolean deliver(MinibossEntity npc, ServerPlayerEntity player);

    @Override
    public double price(MinibossEntity npc, ServerPlayerEntity player) {
        return CampServices.price(npc, player, this.basePrice(RPGMinibossesEntities.config));
    }

    /** The price as the conversation's tooltip gives it: one amount, unless the service varies it (a range). */
    protected String priceLabel(MinibossEntity npc, ServerPlayerEntity player) {
        return String.format(java.util.Locale.ROOT, "%.1f", this.price(npc, player));
    }

    @Override
    public final @Nullable Text whyNot(MinibossEntity npc, ServerPlayerEntity player) {
        Text shared = CampServices.whyNot(npc, player, this.kind(), this.dailyUses());
        return shared != null ? shared : this.ownWhyNot(npc, player);
    }

    @Override
    public final boolean perform(MinibossEntity npc, ServerPlayerEntity player) {
        if (!this.deliver(npc, player)) {
            return false;
        }
        CampServices.recordUse(npc, player, this.kind());
        Factions.recordDeed(npc, player, StandingCause.TRADED, RPGMinibossesEntities.config.standingTrade);
        return true;
    }
}
