package com.cleannrooster.rpg_minibosses.magic.fx;

import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.math.Vec3d;

/**
 * The arguments every cue gets. Each cue reads the ones it needs: a nova uses {@code at} and
 * {@code size}; a beam, cone or arc also uses {@code to} (an end point, or a direction for a cone);
 * {@code angle} is a cone's half-angle; {@code entityId} lets a cue follow an entity (-1 for none).
 *
 * @param school decides the colours, through the client's style for that school
 * @param tier   0 to 2, how much the shape throws off
 * @param seed   so every client draws the same random detail
 */
public record CueArgs(MagicSchool school, Vec3d at, Vec3d to, float size, float angle, int tier, long seed,
                      int entityId) {

    public static CueArgs at(MagicSchool school, Vec3d at, float size, int tier, long seed) {
        return new CueArgs(school, at, at, size, 0.0f, tier, seed, -1);
    }

    public static CueArgs between(MagicSchool school, Vec3d from, Vec3d to, float size, int tier, long seed) {
        return new CueArgs(school, from, to, size, 0.0f, tier, seed, -1);
    }

    public static CueArgs cone(MagicSchool school, Vec3d at, Vec3d axis, float halfAngle, float reach, int tier, long seed) {
        return new CueArgs(school, at, axis, reach, halfAngle, tier, seed, -1);
    }

    public CueArgs following(int entityId) {
        return new CueArgs(this.school, this.at, this.to, this.size, this.angle, this.tier, this.seed, entityId);
    }

    void write(RegistryByteBuf buffer) {
        buffer.writeByte(this.school.ordinal());
        writeVec(buffer, this.at);
        writeVec(buffer, this.to);
        buffer.writeFloat(this.size);
        buffer.writeFloat(this.angle);
        buffer.writeByte(this.tier);
        buffer.writeLong(this.seed);
        buffer.writeVarInt(this.entityId + 1);
    }

    static CueArgs read(RegistryByteBuf buffer) {
        var schools = MagicSchool.values();
        var school = schools[Math.floorMod(buffer.readByte(), schools.length)];
        return new CueArgs(school, readVec(buffer), readVec(buffer), buffer.readFloat(), buffer.readFloat(),
                buffer.readByte(), buffer.readLong(), buffer.readVarInt() - 1);
    }

    static void writeVec(RegistryByteBuf buffer, Vec3d vec) {
        buffer.writeDouble(vec.x);
        buffer.writeDouble(vec.y);
        buffer.writeDouble(vec.z);
    }

    static Vec3d readVec(RegistryByteBuf buffer) {
        return new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
