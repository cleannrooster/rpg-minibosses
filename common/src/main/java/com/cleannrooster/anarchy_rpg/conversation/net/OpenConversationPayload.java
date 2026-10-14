package com.cleannrooster.anarchy_rpg.conversation.net;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
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
 * Server to client: open the conversation screen. Carries everything the screen shows, already decided
 * on the server: the client never works out which options exist or whether they can be picked.
 *
 * @param heardAboutYou gossip about someone asking after this player; always null until Rumors exist
 */
public record OpenConversationPayload(int npcId, Text npcName, Text greeting, Text factionName,
                                      DyeColor flagColor, Disposition standing, @Nullable Text heardAboutYou,
                                      List<Entry> options) implements CustomPayload {

    /** One option as shown: whether it can be picked and, if not, why. */
    public record Entry(ConversationOption option, boolean enabled, @Nullable Text reason) {
    }

    public static final CustomPayload.Id<OpenConversationPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "open_conversation"));

    public static final PacketCodec<RegistryByteBuf, OpenConversationPayload> CODEC =
            PacketCodec.of(OpenConversationPayload::write, OpenConversationPayload::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeVarInt(this.npcId);
        TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, this.npcName);
        TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, this.greeting);
        TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, this.factionName);
        buffer.writeVarInt(this.flagColor.getId());
        buffer.writeVarInt(this.standing.ordinal());
        writeOptionalText(buffer, this.heardAboutYou);
        buffer.writeVarInt(this.options.size());
        for (Entry entry : this.options) {
            buffer.writeVarInt(entry.option().ordinal());
            buffer.writeBoolean(entry.enabled());
            writeOptionalText(buffer, entry.reason());
        }
    }

    private static OpenConversationPayload read(RegistryByteBuf buffer) {
        int npcId = buffer.readVarInt();
        Text npcName = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
        Text greeting = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
        Text factionName = TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer);
        DyeColor flagColor = DyeColor.byId(buffer.readVarInt());
        Disposition standing = Disposition.values()[Math.floorMod(buffer.readVarInt(), Disposition.values().length)];
        Text heardAboutYou = readOptionalText(buffer);
        int count = buffer.readVarInt();
        List<Entry> options = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ConversationOption option = ConversationOption.fromIndex(buffer.readVarInt());
            boolean enabled = buffer.readBoolean();
            Text reason = readOptionalText(buffer);
            if (option != null) {
                options.add(new Entry(option, enabled, reason));
            }
        }
        return new OpenConversationPayload(npcId, npcName, greeting, factionName, flagColor, standing, heardAboutYou, options);
    }

    private static void writeOptionalText(RegistryByteBuf buffer, @Nullable Text text) {
        buffer.writeBoolean(text != null);
        if (text != null) {
            TextCodecs.REGISTRY_PACKET_CODEC.encode(buffer, text);
        }
    }

    private static @Nullable Text readOptionalText(RegistryByteBuf buffer) {
        return buffer.readBoolean() ? TextCodecs.REGISTRY_PACKET_CODEC.decode(buffer) : null;
    }
}
