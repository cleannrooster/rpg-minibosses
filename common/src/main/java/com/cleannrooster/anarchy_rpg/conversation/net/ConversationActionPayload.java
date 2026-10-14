package com.cleannrooster.anarchy_rpg.conversation.net;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Client to server: the player picked an option, or (with a null option) left the conversation. The
 * server re-checks everything before acting on it.
 */
public record ConversationActionPayload(int npcId, @Nullable ConversationOption option) implements CustomPayload {

    public static final CustomPayload.Id<ConversationActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "conversation_action"));

    public static final PacketCodec<RegistryByteBuf, ConversationActionPayload> CODEC =
            PacketCodec.of((payload, buffer) -> {
                        buffer.writeVarInt(payload.npcId);
                        buffer.writeVarInt(payload.option == null ? -1 : payload.option.ordinal());
                    },
                    buffer -> new ConversationActionPayload(buffer.readVarInt(), ConversationOption.fromIndex(buffer.readVarInt())));

    public static ConversationActionPayload leave(int npcId) {
        return new ConversationActionPayload(npcId, null);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
