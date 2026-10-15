package com.cleannrooster.anarchy_rpg.journal.net;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client to server: the player pressed the journal key. The server answers with a {@link JournalPayload}. */
public record JournalRequestPayload() implements CustomPayload {

    public static final JournalRequestPayload INSTANCE = new JournalRequestPayload();

    public static final CustomPayload.Id<JournalRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "journal_request"));

    public static final PacketCodec<RegistryByteBuf, JournalRequestPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
