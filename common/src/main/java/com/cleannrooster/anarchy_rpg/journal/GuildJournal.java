package com.cleannrooster.anarchy_rpg.journal;

import com.cleannrooster.anarchy_rpg.companion.Companions;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import com.cleannrooster.anarchy_rpg.faction.Faction;
import com.cleannrooster.anarchy_rpg.faction.FactionRoster;
import com.cleannrooster.anarchy_rpg.faction.FactionSlot;
import com.cleannrooster.anarchy_rpg.faction.FactionState;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import com.cleannrooster.anarchy_rpg.faction.StandingLedger;
import com.cleannrooster.anarchy_rpg.faction.StandingThresholds;
import com.cleannrooster.anarchy_rpg.journal.net.JournalActionPayload;
import com.cleannrooster.anarchy_rpg.journal.net.JournalPayload;
import com.cleannrooster.anarchy_rpg.journal.net.JournalRequestPayload;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The Guild Journal: opened with a key, it shows the rumors and gossip a player has been told, their
 * companion (with a button to release it), their standing with each guild, and how the guilds regard each
 * other. The server builds the whole page and sends it; the client only lays it out.
 */
public final class GuildJournal {
    private static final long TICKS_PER_DAY = 24000L;

    private GuildJournal() {
    }

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            // A client declares this by registering its receiver; declaring it twice is an error.
            NetworkManager.registerS2CPayloadType(JournalPayload.ID, JournalPayload.CODEC);
        }
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, JournalRequestPayload.ID, JournalRequestPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        send(player);
                    }
                }));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, JournalActionPayload.ID, JournalActionPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        onAction(player, payload.action());
                    }
                }));
    }

    /** A rumor an adventurer just told, written down as it was said. */
    public static void recordRumor(ServerPlayerEntity player, MinibossEntity teller, Text line) {
        record(player, JournalLog.Kind.RUMOR, teller, line);
    }

    /** Gossip about the player that an adventurer just passed on. */
    public static void recordGossip(ServerPlayerEntity player, MinibossEntity teller, Text line) {
        record(player, JournalLog.Kind.GOSSIP, teller, line);
    }

    private static void record(ServerPlayerEntity player, JournalLog.Kind kind, MinibossEntity teller, Text line) {
        MinecraftServer server = player.getServer();
        JournalLog.get(server).record(player.getUuid(),
                new JournalLog.Entry(kind, teller.getName(), Factions.factionOf(teller), line, today(server)));
    }

    private static void onAction(ServerPlayerEntity player, JournalActionPayload.Action action) {
        switch (action) {
            case RELEASE_COMPANION -> {
                Text feedback = Companions.release(player);
                if (feedback != null) {
                    player.sendMessage(feedback);
                }
            }
        }
        send(player);
    }

    /** Builds this player's journal and sends it, opening the screen or refreshing an open one. */
    public static void send(ServerPlayerEntity player) {
        if (!NetworkManager.canPlayerReceive(player, JournalPayload.ID)) {
            return;
        }
        MinecraftServer server = player.getServer();
        FactionRoster roster = FactionState.roster(server);
        StandingLedger ledger = StandingLedger.get(server);
        StandingThresholds thresholds = StandingThresholds.from(RPGMinibossesEntities.config);

        List<JournalPayload.Guild> guilds = new ArrayList<>();
        for (FactionSlot slot : FactionSlot.values()) {
            guilds.add(guild(roster, ledger, thresholds, player, slot));
        }

        List<JournalLog.Entry> logged = JournalLog.get(server).of(player.getUuid());
        List<JournalPayload.Entry> entries = new ArrayList<>(logged.size());
        for (int i = logged.size() - 1; i >= 0; i--) {
            JournalLog.Entry entry = logged.get(i);
            JournalPayload.Guild guild = entry.faction() == null ? null : guilds.get(entry.faction().index());
            entries.add(new JournalPayload.Entry(entry.kind(), entry.teller(), guild, entry.line(), entry.day()));
        }

        List<JournalPayload.Relation> relations = new ArrayList<>();
        FactionSlot[] slots = FactionSlot.values();
        for (int a = 0; a < slots.length; a++) {
            for (int b = a + 1; b < slots.length; b++) {
                relations.add(new JournalPayload.Relation(a, b, roster.between(slots[a], slots[b])));
            }
        }

        Companions.Summary summary = Companions.summaryOf(player);
        JournalPayload.Companion companion = summary == null ? null
                : new JournalPayload.Companion(summary.name(), summary.status(), summary.where());

        NetworkManager.sendToPlayer(player, new JournalPayload(entries, today(server), companion, guilds, relations));
    }

    private static JournalPayload.Guild guild(FactionRoster roster, StandingLedger ledger, StandingThresholds thresholds,
                                              ServerPlayerEntity player, FactionSlot slot) {
        Faction faction = roster.get(slot);
        int standing = ledger.get(player.getUuid(), slot);
        Disposition disposition = Disposition.ofStanding(standing, thresholds);
        // As in Factions.disposition: true anarchy makes every guild hostile, and with guildsNeverHostile bad
        // standing bottoms out at wary.
        if (RPGMinibossesEntities.config.trueAnarchy) {
            disposition = Disposition.HOSTILE;
        } else if (disposition == Disposition.HOSTILE && RPGMinibossesEntities.config.guildsNeverHostile) {
            disposition = Disposition.WARY;
        }
        return new JournalPayload.Guild(faction.name(), faction.flag().base(), standing, disposition);
    }

    private static long today(MinecraftServer server) {
        return server.getOverworld().getTimeOfDay() / TICKS_PER_DAY;
    }
}
