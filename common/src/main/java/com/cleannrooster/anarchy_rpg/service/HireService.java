package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.companion.Companions;
import com.cleannrooster.anarchy_rpg.companion.Hiring;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import com.cleannrooster.anarchy_rpg.faction.StandingCause;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/** Hiring an adventurer as a companion: the rules from {@link Hiring}, sold through the paid-service window. */
final class HireService implements PaidService {

    @Override
    public double price(MinibossEntity npc, ServerPlayerEntity player) {
        return Hiring.price(npc, player);
    }

    @Override
    public @Nullable Text whyNot(MinibossEntity npc, ServerPlayerEntity player) {
        if (npc.getOwnerUuid() != null) {
            return Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".hire.taken");
        }
        return Hiring.whyNot(npc, player, Factions.disposition(npc, player));
    }

    @Override
    public boolean perform(MinibossEntity npc, ServerPlayerEntity player) {
        // Standing first, while the adventurer still answers to its faction; once hired it has none.
        Factions.recordDeed(npc, player, StandingCause.HIRED, RPGMinibossesEntities.config.standingHire);
        return Companions.tryRecruit(player, npc) == Companions.RecruitResult.BOUND;
    }
}
