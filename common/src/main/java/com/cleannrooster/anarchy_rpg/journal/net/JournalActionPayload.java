package com.cleannrooster.anarchy_rpg.journal.net;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client to server: a button in the journal was pressed. The server re-checks it and sends the journal again. */
public record JournalActionPayload(Action action) implements CustomPayload {

    public enum Action {
        /** Free the companion slot outright, the same as {@code /companion release}. */
        RELEASE_COMPANION;

        static Action fromIndex(int index) {
            Action[] values = values();
            return values[Math.floorMod(index, values.length)];
        }
    }

    public static final CustomPayload.Id<JournalActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "journal_action"));

    public static final PacketCodec<RegistryByteBuf, JournalActionPayload> CODEC =
            PacketCodec.of((payload, buffer) -> buffer.writeVarInt(payload.action.ordinal()),
                    buffer -> new JournalActionPayload(Action.fromIndex(buffer.readVarInt())));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
