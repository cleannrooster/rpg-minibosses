package com.cleannrooster.rpg_minibosses.magic.fx;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** "Play cue {@code cue} with these arguments." Server to client. */
public record FxCuePayload(Identifier cue, CueArgs args) implements CustomPayload {

    public static final CustomPayload.Id<FxCuePayload> ID = new CustomPayload.Id<>(RPGMinibosses.id("fx_cue"));

    public static final PacketCodec<RegistryByteBuf, FxCuePayload> CODEC =
            PacketCodec.of(FxCuePayload::write, FxCuePayload::read);

    private void write(RegistryByteBuf buffer) {
        buffer.writeIdentifier(this.cue);
        this.args.write(buffer);
    }

    private static FxCuePayload read(RegistryByteBuf buffer) {
        return new FxCuePayload(buffer.readIdentifier(), CueArgs.read(buffer));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
