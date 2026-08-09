package com.cleannrooster.rpg_minibosses.entity.combat.net;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackFrame;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * One swing, on the wire.
 *
 * <p>Deliberately tiny: the id of an action both sides already hold, the attacker's entity id, and the
 * captured {@link AttackFrame}. Everything about the attack's <em>shape</em> — arc span, reach, plane,
 * blade subdivisions, ribbon colour — is looked up locally from {@link
 * com.cleannrooster.rpg_minibosses.entity.combat.AttackRegistry}, so nothing that defines the geometry
 * travels over the network and the client cannot disagree with the server about it.
 *
 * @param attackId   the {@link com.cleannrooster.rpg_minibosses.entity.combat.CombatAction} id
 * @param attackerId entity id of the swinging mob, so a travelling volume can follow it
 */
public record SwingPayload(String attackId, int attackerId, Vec3d origin, float yaw, float pitch,
                           float rollOffset, boolean mirrored, float scale) implements CustomPayload {

    public static final CustomPayload.Id<SwingPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "attack_swing"));

    public static final PacketCodec<RegistryByteBuf, SwingPayload> CODEC =
            PacketCodec.of(SwingPayload::write, SwingPayload::read);

    public static SwingPayload of(String attackId, int attackerId, AttackFrame frame) {
        return new SwingPayload(attackId, attackerId, frame.origin(), frame.yaw(), frame.pitch(),
                frame.rollOffset(), frame.mirrored(), frame.scale());
    }

    public AttackFrame frame() {
        return new AttackFrame(this.origin, this.yaw, this.pitch, this.rollOffset, this.mirrored,
                this.scale);
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeString(this.attackId);
        buffer.writeVarInt(this.attackerId);
        buffer.writeDouble(this.origin.x);
        buffer.writeDouble(this.origin.y);
        buffer.writeDouble(this.origin.z);
        buffer.writeFloat(this.yaw);
        buffer.writeFloat(this.pitch);
        buffer.writeFloat(this.rollOffset);
        buffer.writeBoolean(this.mirrored);
        buffer.writeFloat(this.scale);
    }

    private static SwingPayload read(RegistryByteBuf buffer) {
        var attackId = buffer.readString();
        var attackerId = buffer.readVarInt();
        var origin = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new SwingPayload(attackId, attackerId, origin, buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readBoolean(), buffer.readFloat());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
