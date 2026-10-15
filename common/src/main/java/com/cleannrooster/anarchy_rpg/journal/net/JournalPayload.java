package com.cleannrooster.anarchy_rpg.journal.net;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import com.cleannrooster.anarchy_rpg.faction.FactionRelation;
import com.cleannrooster.anarchy_rpg.journal.JournalLog;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: open (or refresh) the Guild Journal. Everything is worded on the server, as with the
 * conversation screen; the client only lays it out.
 *
 * @param entries   rumors and gossip, newest first
 * @param today     the current in-game day, for "today" / "N days ago"
 * @param companion the player's companion, or null when they have none
 * @param guilds    the world's three guilds and the player's standing with each
 * @param relations how each pair of guilds regards the other
 */
public record JournalPayload(List<Entry> entries, long today, @Nullable Companion companion, List<Guild> guilds,
                             List<Relation> relations) implements CustomPayload {

    /** {@code guild} is null when a companion told it. */
    public record Entry(JournalLog.Kind kind, Text teller, @Nullable Guild guild, Text line, long day) {
    }

    /** {@code where} is null when nothing useful is known about where it is. */
    public record Companion(Text name, Text status, @Nullable Text where) {
    }

    public record Guild(Text name, DyeColor flagColor, int standing, Disposition disposition) {
    }

    /** {@code first} and {@code second} index into {@link #guilds}. */
    public record Relation(int first, int second, FactionRelation relation) {
    }

    public static final CustomPayload.Id<JournalPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "journal"));

    public static final PacketCodec<RegistryByteBuf, JournalPayload> CODEC =
            PacketCodec.of(JournalPayload::write, JournalPayload::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeVarInt(this.entries.size());
        for (Entry entry : this.entries) {
            buffer.writeVarInt(entry.kind().ordinal());
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, entry.teller());
            buffer.writeBoolean(entry.guild() != null);
            if (entry.guild() != null) {
                writeGuild(buffer, entry.guild());
            }
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, entry.line());
            buffer.writeVarLong(entry.day());
        }
        buffer.writeVarLong(this.today);
        buffer.writeBoolean(this.companion != null);
        if (this.companion != null) {
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, this.companion.name());
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, this.companion.status());
            buffer.writeBoolean(this.companion.where() != null);
            if (this.companion.where() != null) {
                TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, this.companion.where());
            }
        }
        buffer.writeVarInt(this.guilds.size());
        for (Guild guild : this.guilds) {
            writeGuild(buffer, guild);
        }
        buffer.writeVarInt(this.relations.size());
        for (Relation relation : this.relations) {
            buffer.writeVarInt(relation.first());
            buffer.writeVarInt(relation.second());
            buffer.writeVarInt(relation.relation().ordinal());
        }
    }

    private static JournalPayload read(RegistryByteBuf buffer) {
        int entryCount = buffer.readVarInt();
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            JournalLog.Kind kind = JournalLog.Kind.values()[Math.floorMod(buffer.readVarInt(), JournalLog.Kind.values().length)];
            Text teller = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
            Guild guild = buffer.readBoolean() ? readGuild(buffer) : null;
            Text line = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
            entries.add(new Entry(kind, teller, guild, line, buffer.readVarLong()));
        }
        long today = buffer.readVarLong();
        Companion companion = null;
        if (buffer.readBoolean()) {
            Text name = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
            Text status = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
            Text where = buffer.readBoolean() ? TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer) : null;
            companion = new Companion(name, status, where);
        }
        int guildCount = buffer.readVarInt();
        List<Guild> guilds = new ArrayList<>(guildCount);
        for (int i = 0; i < guildCount; i++) {
            guilds.add(readGuild(buffer));
        }
        int relationCount = buffer.readVarInt();
        List<Relation> relations = new ArrayList<>(relationCount);
        FactionRelation[] kinds = FactionRelation.values();
        for (int i = 0; i < relationCount; i++) {
            relations.add(new Relation(buffer.readVarInt(), buffer.readVarInt(), kinds[Math.floorMod(buffer.readVarInt(), kinds.length)]));
        }
        return new JournalPayload(entries, today, companion, guilds, relations);
    }

    private static void writeGuild(RegistryByteBuf buffer, Guild guild) {
        TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, guild.name());
        buffer.writeVarInt(guild.flagColor().getId());
        buffer.writeVarInt(guild.standing());
        buffer.writeVarInt(guild.disposition().ordinal());
    }

    private static Guild readGuild(RegistryByteBuf buffer) {
        Text name = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
        DyeColor color = DyeColor.byId(buffer.readVarInt());
        int standing = buffer.readVarInt();
        Disposition disposition = Disposition.values()[Math.floorMod(buffer.readVarInt(), Disposition.values().length)];
        return new Guild(name, color, standing, disposition);
    }
}
