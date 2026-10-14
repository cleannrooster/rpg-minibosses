package com.cleannrooster.anarchy_rpg.hub;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: the hubs near this player, each as the point its banner floats at and its packed
 * faction flag. Replaces the client's previous list; an empty list clears it.
 */
public record HubMarkersPayload(List<Marker> markers) implements CustomPayload {

    public record Marker(BlockPos pos, int flag) {
    }

    public static final CustomPayload.Id<HubMarkersPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "hub_markers"));

    public static final PacketCodec<RegistryByteBuf, HubMarkersPayload> CODEC = PacketCodec.of(
            (payload, buffer) -> {
                buffer.writeVarInt(payload.markers.size());
                for (Marker marker : payload.markers) {
                    buffer.writeBlockPos(marker.pos());
                    buffer.writeInt(marker.flag());
                }
            },
            buffer -> {
                int count = buffer.readVarInt();
                List<Marker> markers = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    markers.add(new Marker(buffer.readBlockPos(), buffer.readInt()));
                }
                return new HubMarkersPayload(markers);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
