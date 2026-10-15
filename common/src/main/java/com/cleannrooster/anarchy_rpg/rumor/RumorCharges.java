package com.cleannrooster.anarchy_rpg.rumor;

import net.minecraft.nbt.NbtCompound;

/**
 * How many rumors an adventurer has left to tell. Saved on the adventurer.
 *
 * <p>A wandering adventurer starts full and slowly hears new things with world time. A companion hears new
 * things by adventuring with its owner: {@link #accrue} adds progress, a whole unit of which is one charge.
 */
public final class RumorCharges {
    /** -1 until first used, so adventurers from before Rumors start full. */
    private int charges = -1;
    /** World time the next time-based charge is counted from; -1 until first ticked. */
    private long regenFrom = -1;
    /** Companion progress toward the next charge, 0 to 1. */
    private double progress;

    public int current(int max) {
        if (this.charges < 0) {
            this.charges = max;
        }
        return Math.min(this.charges, max);
    }

    /** Spends one charge; false if there is none. */
    public boolean spend(int max) {
        if (this.current(max) <= 0) {
            return false;
        }
        this.charges--;
        return true;
    }

    public void refill(int max) {
        this.charges = max;
        this.progress = 0;
    }

    /** Time-based regeneration for wandering adventurers: one charge per {@code ticksPerCharge} of world time. */
    public void regenerate(long now, long ticksPerCharge, int max) {
        int current = this.current(max);
        if (current >= max || ticksPerCharge <= 0 || this.regenFrom < 0 || this.regenFrom > now) {
            this.regenFrom = now;
            return;
        }
        long earned = (now - this.regenFrom) / ticksPerCharge;
        if (earned > 0) {
            this.charges = (int) Math.min(max, current + earned);
            this.regenFrom += earned * ticksPerCharge;
        }
    }

    /** Companion regeneration: {@code amount} of a charge, from adventuring. */
    public void accrue(double amount, int max) {
        int current = this.current(max);
        if (current >= max) {
            this.progress = 0;
            return;
        }
        this.progress += amount;
        if (this.progress >= 1.0) {
            int whole = (int) this.progress;
            this.charges = Math.min(max, current + whole);
            this.progress -= whole;
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("Charges", this.charges);
        nbt.putLong("RegenFrom", this.regenFrom);
        nbt.putDouble("Progress", this.progress);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        this.charges = nbt.contains("Charges") ? nbt.getInt("Charges") : -1;
        this.regenFrom = nbt.contains("RegenFrom") ? nbt.getLong("RegenFrom") : -1;
        this.progress = nbt.getDouble("Progress");
    }
}
