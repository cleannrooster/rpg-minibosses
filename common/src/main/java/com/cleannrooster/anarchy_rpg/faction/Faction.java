package com.cleannrooster.anarchy_rpg.faction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

/** One of a world's three factions, as rolled for that world. */
public record Faction(FactionSlot slot, String nameKey, FactionFlag flag) {

    public Text name() {
        return Text.translatable(this.nameKey);
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Name", this.nameKey);
        nbt.put("Flag", this.flag.toNbt());
        return nbt;
    }

    static Faction fromNbt(FactionSlot slot, NbtCompound nbt) {
        return new Faction(slot, nbt.getString("Name"), FactionFlag.fromNbt(nbt.getCompound("Flag")));
    }
}
