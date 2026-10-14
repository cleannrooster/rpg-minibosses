package com.cleannrooster.anarchy_rpg.conversation.net;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server to client: the server ended this conversation (walked away, combat, a picked option finished it). */
public record ConversationClosedPayload(int npcId) implements CustomPayload {

    public static final CustomPayload.Id<ConversationClosedPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "conversation_closed"));

    public static final PacketCodec<RegistryByteBuf, ConversationClosedPayload> CODEC =
            PacketCodec.of((payload, buffer) -> buffer.writeVarInt(payload.npcId),
                    buffer -> new ConversationClosedPayload(buffer.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
