package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.companion.CompanionSpecialty;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOptions;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.exchange.ExchangePricing;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import com.cleannrooster.anarchy_rpg.faction.FactionSlot;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import com.cleannrooster.anarchy_rpg.faction.StandingLedger;
import com.cleannrooster.anarchy_rpg.hub.HubAnchor;
import com.cleannrooster.anarchy_rpg.hub.HubState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Rules shared by every camp service: the paid, lesser version of an archetype's companion specialty that an
 * unrecruited adventurer living at a camp or tower offers to anyone on good enough terms.
 * <ul>
 *   <li>Offered only by a resident of a hub ({@link HubAnchor}), never by a wanderer or a companion.</li>
 *   <li>Priced in the currency item: the service's base price on the same standing curve as Exchange and
 *   Hire, so better standing pays less.</li>
 *   <li>Refused by an adventurer wary of you; limited to so many uses a day per adventurer, per player.</li>
 * </ul>
 */
public final class CampServices {
    private static final String NS = RPGMinibosses.CONTENT_NAMESPACE;
    private static final long TICKS_PER_DAY = 24000L;

    private CampServices() {
    }

    /** Non-registry init: each camp service's conversation option, and the debug command. */
    public static void register() {
        registerOption(ConversationOption.CAMP_LIFT_CURSE, PaidServiceKind.LIFT_CURSE, CompanionSpecialty.CLEANSE);
        registerOption(ConversationOption.CAMP_ENCHANT_ITEM, PaidServiceKind.ENCHANT_ITEM, CompanionSpecialty.DISENCHANT);
        registerOption(ConversationOption.CAMP_GAMBLE, PaidServiceKind.GAMBLE, CompanionSpecialty.TRADER,
                GambleService::beforeOpen);
        GambleService.registerCleanup();
        registerOption(ConversationOption.CAMP_SELL_DIRECTIONS, PaidServiceKind.SELL_DIRECTIONS, CompanionSpecialty.GUIDE,
                SellDirectionsService::beforeOpen);
        SellDirectionsService.registerCleanup();
        registerOption(ConversationOption.CAMP_GUARD, PaidServiceKind.GUARD_CAMP, CompanionSpecialty.PATROL);
        CampServiceDebugCommand.register();
    }

    /** The in-game day by the overworld clock; the same day companion specialties count by. */
    public static long today(MinecraftServer server) {
        return server.getOverworld().getTimeOfDay() / TICKS_PER_DAY;
    }

    /** The camp or tower this adventurer lives at, or null for a wanderer (or anyone's companion). */
    public static @Nullable HubAnchor hubOf(MinibossEntity npc) {
        if (npc.getOwnerUuid() != null || !(npc.getWorld() instanceof ServerWorld world)) {
            return null;
        }
        for (HubAnchor anchor : HubState.get(world).anchors().values()) {
            for (HubAnchor.Resident resident : anchor.residents()) {
                if (resident.uuid().equals(npc.getUuid())) {
                    return anchor;
                }
            }
        }
        return null;
    }

    /** Base price on the standing curve: dearer the less the adventurer's guild thinks of you. */
    public static double price(MinibossEntity npc, ServerPlayerEntity player, double basePrice) {
        FactionSlot faction = Factions.factionOf(npc);
        int standing = faction == null ? 0 : StandingLedger.get(player.getServer()).get(player.getUuid(), faction);
        return basePrice * ExchangePricing.dispositionFactor(standing);
    }

    public static int usesLeft(MinibossEntity npc, ServerPlayerEntity player, PaidServiceKind kind, int dailyUses) {
        int used = npc.getCampServiceUses().usesOn(player.getUuid(), kind, today(player.getServer()));
        return Math.max(0, dailyUses - used);
    }

    /** The shared reasons a camp service is refused right now, or null. */
    public static @Nullable Text whyNot(MinibossEntity npc, ServerPlayerEntity player, PaidServiceKind kind, int dailyUses) {
        if (hubOf(npc) == null) {
            return line("not_here");
        }
        if (Factions.disposition(npc, player) == Disposition.WARY) {
            return line("wary");
        }
        if (usesLeft(npc, player, kind, dailyUses) <= 0) {
            return line("done_today");
        }
        return null;
    }

    static void recordUse(MinibossEntity npc, ServerPlayerEntity player, PaidServiceKind kind) {
        npc.getCampServiceUses().record(player.getUuid(), kind, today(player.getServer()));
    }

    /**
     * Registers the conversation option that opens {@code kind}'s payment window. It shows only for a hub
     * resident of {@code archetype}; refused, it stays visible with the reason; otherwise its tooltip gives the
     * price (and the uses left today, when there are several).
     */
    public static void registerOption(ConversationOption option, PaidServiceKind kind, CompanionSpecialty archetype) {
        registerOption(option, kind, archetype, null);
    }

    /**
     * Runs when the option is picked, before the payment window opens. Returns true to open it; false to keep
     * the conversation going instead (having replied: a warning to confirm, say).
     */
    @FunctionalInterface
    public interface Gate {
        boolean beforeOpen(MinibossEntity npc, ServerPlayerEntity player);
    }

    /** As {@link #registerOption(ConversationOption, PaidServiceKind, CompanionSpecialty)}, with a gate first. */
    public static void registerOption(ConversationOption option, PaidServiceKind kind, CompanionSpecialty archetype,
                                      @Nullable Gate gate) {
        ConversationOptions.register(option,
                (npc, player, disposition) -> {
                    if (CompanionSpecialty.of(npc) != archetype || hubOf(npc) == null) {
                        return OptionAvailability.HIDDEN;
                    }
                    if (!(kind.service() instanceof CampService service)) {
                        return OptionAvailability.HIDDEN;
                    }
                    Text reason = service.whyNot(npc, player);
                    if (reason != null) {
                        return OptionAvailability.disabled(reason);
                    }
                    String amount = service.priceLabel(npc, player);
                    int left = usesLeft(npc, player, kind, service.dailyUses());
                    Text tooltip = service.dailyUses() > 1
                            ? line("price_uses", amount, left) : line("price", amount);
                    return new OptionAvailability(true, true, tooltip);
                },
                (npc, player, argument) -> gate == null || gate.beforeOpen(npc, player)
                        ? PaidServiceMenus.open(npc, player, kind) : true);
    }

    static Text line(String key, Object... args) {
        return Text.translatable("conversation." + NS + ".camp." + key, args);
    }

    static String key(PaidServiceKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }
}
