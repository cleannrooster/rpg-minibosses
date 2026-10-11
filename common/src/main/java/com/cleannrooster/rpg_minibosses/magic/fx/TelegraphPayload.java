package com.cleannrooster.rpg_minibosses.magic.fx;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import dev.eann.atlasvfx.telegraph.TelegraphTiming;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * "Warn of this area, landing at this world tick." Server to client. The area is the very object the
 * server will hit-test, already placed and turned; the timing is in world ticks, so a late packet still
 * lands on time.
 *
 * @param helpful drawn in the helpful colour rather than the danger colour
 */
public record TelegraphPayload(TelegraphArea area, TelegraphTiming timing, boolean helpful) implements CustomPayload {

    public static final CustomPayload.Id<TelegraphPayload> ID = new CustomPayload.Id<>(RPGMinibosses.id("telegraph"));

    public static final PacketCodec<RegistryByteBuf, TelegraphPayload> CODEC =
            PacketCodec.of(TelegraphPayload::write, TelegraphPayload::read);

    private static final int CIRCLE = 0;
    private static final int RING = 1;
    private static final int CONE = 2;
    private static final int LINE = 3;

    private void write(RegistryByteBuf buffer) {
        CueArgs.writeVec(buffer, this.area.origin());
        switch (this.area) {
            case TelegraphArea.Circle circle -> {
                buffer.writeByte(CIRCLE);
                buffer.writeFloat(circle.radius());
            }
            case TelegraphArea.Ring ring -> {
                buffer.writeByte(RING);
                buffer.writeFloat(ring.inner());
                buffer.writeFloat(ring.outer());
            }
            case TelegraphArea.Cone cone -> {
                buffer.writeByte(CONE);
                buffer.writeFloat(cone.yaw());
                buffer.writeFloat(cone.range());
                buffer.writeFloat(cone.halfAngle());
            }
            case TelegraphArea.Line line -> {
                buffer.writeByte(LINE);
                buffer.writeFloat(line.yaw());
                buffer.writeFloat(line.length());
                buffer.writeFloat(line.halfWidth());
            }
        }
        buffer.writeLong(this.timing.startTick());
        buffer.writeLong(this.timing.triggerTick());
        buffer.writeVarInt(this.timing.fadeTicks());
        buffer.writeBoolean(this.helpful);
    }

    private static TelegraphPayload read(RegistryByteBuf buffer) {
        var origin = CueArgs.readVec(buffer);
        TelegraphArea area = switch (buffer.readByte()) {
            case RING -> new TelegraphArea.Ring(origin, buffer.readFloat(), buffer.readFloat());
            case CONE -> new TelegraphArea.Cone(origin, buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
            case LINE -> new TelegraphArea.Line(origin, buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
            default -> new TelegraphArea.Circle(origin, buffer.readFloat());
        };
        var timing = new TelegraphTiming(buffer.readLong(), buffer.readLong(), buffer.readVarInt());
        return new TelegraphPayload(area, timing, buffer.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
