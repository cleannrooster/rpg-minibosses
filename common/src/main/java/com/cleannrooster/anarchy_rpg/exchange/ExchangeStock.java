package com.cleannrooster.anarchy_rpg.exchange;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * What an adventurer has to trade: more than it drops on death, and never what it is wearing or holding.
 *
 * <p>Generated once, the first time anyone asks to trade, and saved with the adventurer from then on. It
 * never refills: fresh stock comes from fresh adventurers (camp and tower turnover). It is not dropped on
 * death — only equipped gear drops.
 *
 * <p>Sources, in priority order (stock holds nine stacks; later sources only fill what is left):
 * <ol>
 *   <li>rarely, one boss key from the {@code anarchy_rpg:boss_keys} item tag;</li>
 *   <li>rolls of the adventurer's own drop pool: its loot table as loaded, so it includes the Spell Engine
 *   compat drops and the RPG Series loot-tier injection. Rolled as if the asking player had made the kill,
 *   so player-kill drops can appear. Boss keys are left out of these rolls so they stay rare here;</li>
 *   <li>sometimes, a few stacks of generic dungeon loot from one of the configured chest tables;</li>
 *   <li>weapons from the adventurer's weapon pool;</li>
 *   <li>rolls of the {@code anarchy_rpg:exchange/supplies} loot table;</li>
 *   <li>one roll of the Ender Dragon's table, which modpacks use to inject rare loot (the old guild hall
 *   trades drew from it too).</li>
 * </ol>
 *
 * <p>A recruited Mercenary is the exception to "never refills": each day it {@link #cycle cycles} its oldest
 * stacks for fresh ones. Each slot remembers the day it was stocked for that.
 */
public final class ExchangeStock {
    public static final int SIZE = 9;
    public static final RegistryKey<LootTable> SUPPLIES =
            RegistryKey.of(RegistryKeys.LOOT_TABLE, RPGMinibosses.id("exchange/supplies"));
    public static final TagKey<Item> BOSS_KEYS = TagKey.of(RegistryKeys.ITEM, RPGMinibosses.id("boss_keys"));

    private final SimpleInventory inventory = new SimpleInventory(SIZE);
    private boolean generated;
    /**
     * The in-game day each slot was filled. A stack returned from the request pane lands in a free slot and
     * keeps that slot's old day: close enough, since it can only make that stack cycle a little sooner.
     */
    private final long[] stockedDay = new long[SIZE];

    /** The in-game day by the overworld clock; the same day companion specialties count by. */
    private static long today(ServerWorld world) {
        return world.getServer().getOverworld().getTimeOfDay() / 24000L;
    }

    public SimpleInventory inventory() {
        return this.inventory;
    }

    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    /**
     * Empties the stock and marks it generated, so it is never filled later. Called on recruitment: the
     * inventory becomes the companion's storage, and the trade goods it carried are not the player's to take.
     */
    public void clearForRecruit() {
        this.inventory.clear();
        this.generated = true;
    }

    /**
     * The Mercenary's daily turnover: the {@code count} oldest stacks go, and fresh ones are rolled into empty
     * slots until {@code count} have been added, so a bought-out stock refills {@code count} a day. Fresh
     * stacks come from the same sources as generation, except boss keys, which stay first-stock only.
     */
    public void cycle(MinibossEntity npc, @Nullable PlayerEntity asker, int count) {
        if (!(npc.getWorld() instanceof ServerWorld world) || count <= 0) {
            return;
        }
        this.ensureGenerated(npc, asker);
        Integer[] slots = new Integer[SIZE];
        for (int i = 0; i < SIZE; i++) {
            slots[i] = i;
        }
        Arrays.sort(slots, Comparator.comparingLong(slot -> this.stockedDay[slot]));
        int removed = 0;
        for (int slot : slots) {
            if (removed >= count) {
                break;
            }
            if (!this.inventory.getStack(slot).isEmpty()) {
                this.inventory.setStack(slot, ItemStack.EMPTY);
                removed++;
            }
        }

        Random random = Random.create(world.getRandom().nextLong());
        var config = RPGMinibossesEntities.config;
        List<ItemStack> fresh = new ArrayList<>();
        Consumer<ItemStack> collect = stack -> {
            if (!stack.isEmpty() && !stack.isIn(BOSS_KEYS)) {
                fresh.add(stack.copy());
            }
        };
        List<Item> weapons = npc.getWeaponPool();
        for (int round = 0; round < 8 && fresh.size() < count; round++) {
            switch (random.nextInt(4)) {
                case 0 -> this.roll(world, npc, npc.getType().getLootTableId(), random.nextLong(), asker, collect);
                case 1 -> {
                    if (!weapons.isEmpty()) {
                        collect.accept(weapons.get(random.nextInt(weapons.size())).getDefaultStack());
                    }
                }
                case 2 -> this.roll(world, npc, SUPPLIES, random.nextLong(), asker, collect);
                default -> {
                    if (!config.exchangeDungeonLootTables.isEmpty()) {
                        this.dungeonLoot(world, npc, asker, random, collect);
                    }
                }
            }
        }
        long day = today(world);
        for (int i = 0; i < Math.min(count, fresh.size()); i++) {
            this.add(fresh.get(i), day);
        }
    }

    /**
     * Fills the stock on first use; afterwards does nothing.
     *
     * @param asker the player asking to trade, credited with the kill when rolling the drop pool
     */
    public void ensureGenerated(MinibossEntity npc, @Nullable PlayerEntity asker) {
        if (this.generated || !(npc.getWorld() instanceof ServerWorld world)) {
            return;
        }
        this.generated = true;
        long day = today(world);
        Consumer<ItemStack> add = stack -> this.add(stack, day);
        long seed = npc.getUuid().getLeastSignificantBits() ^ world.getSeed();
        Random random = Random.create(seed);

        var config = RPGMinibossesEntities.config;
        if (random.nextDouble() < config.exchangeBossKeyChance) {
            List<Item> keys = Registries.ITEM.stream().filter(item -> item.getDefaultStack().isIn(BOSS_KEYS)).toList();
            if (!keys.isEmpty()) {
                add.accept(keys.get(random.nextInt(keys.size())).getDefaultStack());
            }
        }
        for (int i = 0; i < config.exchangeStockDropRolls; i++) {
            this.roll(world, npc, npc.getType().getLootTableId(), random.nextLong(), asker,
                    stack -> {
                        if (!stack.isIn(BOSS_KEYS)) {
                            add.accept(stack);
                        }
                    });
        }
        if (random.nextDouble() < config.exchangeDungeonLootChance && !config.exchangeDungeonLootTables.isEmpty()) {
            this.dungeonLoot(world, npc, asker, random, add);
        }
        List<Item> weapons = npc.getWeaponPool();
        for (int i = 0; i < config.exchangeStockWeapons && !weapons.isEmpty(); i++) {
            add.accept(weapons.get(random.nextInt(weapons.size())).getDefaultStack());
        }
        for (int i = 0; i < config.exchangeStockSupplyRolls; i++) {
            this.roll(world, npc, SUPPLIES, random.nextLong(), asker, add);
        }
        this.roll(world, npc, EntityType.ENDER_DRAGON.getLootTableId(), random.nextLong(), asker, add);
    }

    private void roll(ServerWorld world, MinibossEntity npc, RegistryKey<LootTable> key, long seed,
                      @Nullable PlayerEntity asker, Consumer<ItemStack> sink) {
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(key);
        LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.THIS_ENTITY, npc)
                .add(LootContextParameters.ORIGIN, npc.getPos())
                .add(LootContextParameters.DAMAGE_SOURCE, asker == null
                        ? npc.getDamageSources().generic() : npc.getDamageSources().playerAttack(asker));
        if (asker != null) {
            builder.add(LootContextParameters.ATTACKING_ENTITY, asker)
                    .add(LootContextParameters.DIRECT_ATTACKING_ENTITY, asker)
                    .add(LootContextParameters.LAST_DAMAGE_PLAYER, asker)
                    .luck(asker.getLuck());
        }
        table.generateLoot(builder.build(LootContextTypes.ENTITY), seed, sink);
    }

    /**
     * One roll of a random configured chest table. A chest fills many stacks, so only a few, picked at
     * random, are kept: enough to make the stock feel found rather than flooded.
     */
    private void dungeonLoot(ServerWorld world, MinibossEntity npc, @Nullable PlayerEntity asker, Random random,
                             Consumer<ItemStack> sink) {
        var config = RPGMinibossesEntities.config;
        String name = config.exchangeDungeonLootTables.get(random.nextInt(config.exchangeDungeonLootTables.size()));
        Identifier id = Identifier.tryParse(name);
        if (id == null) {
            return;
        }
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, id));
        LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, npc.getPos());
        if (asker != null) {
            builder.add(LootContextParameters.THIS_ENTITY, asker).luck(asker.getLuck());
        }
        List<ItemStack> found = new ArrayList<>();
        table.generateLoot(builder.build(LootContextTypes.CHEST), random.nextLong(), found::add);
        for (int i = found.size() - 1; i > 0; i--) {
            Collections.swap(found, i, random.nextInt(i + 1));
        }
        found.stream().filter(stack -> !stack.isEmpty()).limit(Math.max(0, config.exchangeDungeonLootMaxStacks)).forEach(sink);
    }

    /**
     * Merges into matching stacks, then fills empty slots; whatever does not fit is left out. Slots it fills
     * are stamped with {@code day}; a stack merged into an older one keeps that one's day.
     */
    private void add(ItemStack stack, long day) {
        if (stack.isEmpty()) {
            return;
        }
        boolean[] wasEmpty = new boolean[SIZE];
        for (int i = 0; i < SIZE; i++) {
            wasEmpty[i] = this.inventory.getStack(i).isEmpty();
        }
        this.inventory.addStack(stack.copy());
        for (int i = 0; i < SIZE; i++) {
            if (wasEmpty[i] && !this.inventory.getStack(i).isEmpty()) {
                this.stockedDay[i] = day;
            }
        }
    }

    public NbtCompound toNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("Generated", this.generated);
        DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            stacks.set(i, this.inventory.getStack(i));
        }
        Inventories.writeNbt(nbt, stacks, registries);
        nbt.putLongArray("StockedDay", this.stockedDay);
        return nbt;
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        this.generated = nbt.getBoolean("Generated");
        DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, stacks, registries);
        for (int i = 0; i < SIZE; i++) {
            this.inventory.setStack(i, stacks.get(i));
        }
        long[] days = nbt.getLongArray("StockedDay");
        for (int i = 0; i < SIZE; i++) {
            this.stockedDay[i] = i < days.length ? days[i] : 0;
        }
    }
}
