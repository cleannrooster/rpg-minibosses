package com.cleannrooster.anarchy_rpg.faction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

/**
 * A faction's banner: a base color, one banner pattern laid over it, and the pattern's color. The base
 * color is what tells the three factions apart, so no two factions in a world share one.
 */
public record FactionFlag(DyeColor base, Identifier pattern, DyeColor patternColor) {

    /** White on dark bases, black on light ones, so the pattern always reads. */
    public static DyeColor contrastFor(DyeColor base) {
        return switch (base) {
            case WHITE, YELLOW, LIGHT_GRAY, LIME, LIGHT_BLUE, PINK -> DyeColor.BLACK;
            default -> DyeColor.WHITE;
        };
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Base", this.base.asString());
        nbt.putString("Pattern", this.pattern.toString());
        nbt.putString("PatternColor", this.patternColor.asString());
        return nbt;
    }

    static FactionFlag fromNbt(NbtCompound nbt) {
        DyeColor base = DyeColor.byName(nbt.getString("Base"), DyeColor.WHITE);
        Identifier pattern = Identifier.tryParse(nbt.getString("Pattern"));
        DyeColor patternColor = DyeColor.byName(nbt.getString("PatternColor"), contrastFor(base));
        return new FactionFlag(base, pattern == null ? Identifier.ofVanilla("border") : pattern, patternColor);
    }
}
