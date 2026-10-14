package com.cleannrooster.anarchy_rpg.faction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A world's three factions and the relations between them.
 *
 * <p>{@link #generate} is a pure function of the world seed and the pools, so a seed always rolls the
 * same roster. The roster is persisted the first time it is rolled ({@link FactionState}) and read from
 * the save after that, so changing the pools never renames an existing world's factions.
 *
 * <p>Invariants: three factions with distinct names and distinct flag colors; relations are symmetric;
 * a faction is always {@link FactionRelation#ALLIED} with itself; the three pairs are never all
 * {@link FactionRelation#AT_WAR}.
 */
public final class FactionRoster {
    /** Mixed into the world seed so the roster roll is independent of every other seed-driven roll. */
    private static final long SALT = 0x5EED_FAC7_1015L;

    private final Faction[] factions;
    /** Indexed by {@link #pairIndex}: FIRST-SECOND, FIRST-THIRD, SECOND-THIRD. */
    private final FactionRelation[] relations;

    private FactionRoster(Faction[] factions, FactionRelation[] relations) {
        this.factions = factions;
        this.relations = relations;
    }

    public Faction get(FactionSlot slot) {
        return this.factions[slot.index()];
    }

    public List<Faction> all() {
        return List.of(this.factions);
    }

    public FactionRelation between(FactionSlot a, FactionSlot b) {
        return a == b ? FactionRelation.ALLIED : this.relations[pairIndex(a, b)];
    }

    private static int pairIndex(FactionSlot a, FactionSlot b) {
        int low = Math.min(a.index(), b.index());
        int high = Math.max(a.index(), b.index());
        return low + high - 1;
    }

    public static FactionRoster generate(long worldSeed, FactionPools pools) {
        if (!pools.usable()) {
            pools = FactionPools.DEFAULT;
        }
        Random random = Random.create(worldSeed ^ SALT);
        List<String> names = shuffledDistinct(pools.names(), random);
        List<DyeColor> colors = shuffledDistinct(pools.colors(), random);

        FactionSlot[] slots = FactionSlot.values();
        Faction[] factions = new Faction[slots.length];
        for (FactionSlot slot : slots) {
            DyeColor base = colors.get(slot.index());
            Identifier pattern = pools.patterns().get(random.nextInt(pools.patterns().size()));
            factions[slot.index()] = new Faction(slot, names.get(slot.index()),
                    new FactionFlag(base, pattern, FactionFlag.contrastFor(base)));
        }
        return new FactionRoster(factions, rollRelations(random, pools.relationWeights()));
    }

    /**
     * Draws the three pair relations by weight, redrawing from the same random stream while all three
     * come out at war. If war is the only weighted relation, the last pair is forced neutral so the
     * constraint can always be met.
     */
    private static FactionRelation[] rollRelations(Random random, Map<FactionRelation, Integer> weights) {
        boolean warOnly = weights.entrySet().stream()
                .noneMatch(e -> e.getKey() != FactionRelation.AT_WAR && e.getValue() > 0);
        FactionRelation[] drawn = new FactionRelation[3];
        for (int attempt = 0; attempt < 64; attempt++) {
            for (int i = 0; i < drawn.length; i++) {
                drawn[i] = drawWeighted(random, weights);
            }
            if (!allAtWar(drawn)) {
                return drawn;
            }
        }
        if (warOnly || allAtWar(drawn)) {
            drawn[2] = FactionRelation.NEUTRAL;
        }
        return drawn;
    }

    private static boolean allAtWar(FactionRelation[] relations) {
        for (FactionRelation relation : relations) {
            if (relation != FactionRelation.AT_WAR) {
                return false;
            }
        }
        return true;
    }

    private static FactionRelation drawWeighted(Random random, Map<FactionRelation, Integer> weights) {
        int total = 0;
        for (FactionRelation relation : FactionRelation.values()) {
            total += Math.max(0, weights.getOrDefault(relation, 0));
        }
        if (total <= 0) {
            return FactionRelation.NEUTRAL;
        }
        int pick = random.nextInt(total);
        // Iterate in declaration order, never map order, so the draw is stable for a given seed.
        for (FactionRelation relation : FactionRelation.values()) {
            pick -= Math.max(0, weights.getOrDefault(relation, 0));
            if (pick < 0) {
                return relation;
            }
        }
        return FactionRelation.NEUTRAL;
    }

    private static <T> List<T> shuffledDistinct(List<T> pool, Random random) {
        List<T> distinct = new ArrayList<>(new java.util.LinkedHashSet<>(pool));
        // Fisher-Yates with Minecraft's Random, so the shuffle is seed-stable on every JVM.
        for (int i = distinct.size() - 1; i > 0; i--) {
            Collections.swap(distinct, i, random.nextInt(i + 1));
        }
        return distinct;
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        for (Faction faction : this.factions) {
            nbt.put(faction.slot().name(), faction.toNbt());
        }
        NbtCompound relationsNbt = new NbtCompound();
        relationsNbt.putString("FirstSecond", this.relations[0].name());
        relationsNbt.putString("FirstThird", this.relations[1].name());
        relationsNbt.putString("SecondThird", this.relations[2].name());
        nbt.put("Relations", relationsNbt);
        return nbt;
    }

    static FactionRoster fromNbt(NbtCompound nbt) {
        FactionSlot[] slots = FactionSlot.values();
        Faction[] factions = new Faction[slots.length];
        for (FactionSlot slot : slots) {
            factions[slot.index()] = Faction.fromNbt(slot, nbt.getCompound(slot.name()));
        }
        NbtCompound relationsNbt = nbt.getCompound("Relations");
        FactionRelation[] relations = {
                relationOf(relationsNbt.getString("FirstSecond")),
                relationOf(relationsNbt.getString("FirstThird")),
                relationOf(relationsNbt.getString("SecondThird"))};
        return new FactionRoster(factions, relations);
    }

    private static FactionRelation relationOf(String name) {
        try {
            return FactionRelation.valueOf(name);
        } catch (IllegalArgumentException e) {
            return FactionRelation.NEUTRAL;
        }
    }
}
