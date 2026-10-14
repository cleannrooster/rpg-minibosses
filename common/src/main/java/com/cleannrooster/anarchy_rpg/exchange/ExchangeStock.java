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
import java.util.Collections;
import java.util.List;

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
 */
public final class ExchangeStock {
    public static final int SIZE = 9;
    public static final RegistryKey<LootTable> SUPPLIES =
            RegistryKey.of(RegistryKeys.LOOT_TABLE, RPGMinibosses.id("exchange/supplies"));
    public static final TagKey<Item> BOSS_KEYS = TagKey.of(RegistryKeys.ITEM, RPGMinibosses.id("boss_keys"));

    private final SimpleInventory inventory = new SimpleInventory(SIZE);
    private boolean generated;

    public SimpleInventory inventory() {
        return this.inventory;
    }

    public boolean isEmpty() {
        return this.inventory.isEmpty();
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
        long seed = npc.getUuid().getLeastSignificantBits() ^ world.getSeed();
        Random random = Random.create(seed);

        var config = RPGMinibossesEntities.config;
        if (random.nextDouble() < config.exchangeBossKeyChance) {
            List<Item> keys = Registries.ITEM.stream().filter(item -> item.getDefaultStack().isIn(BOSS_KEYS)).toList();
            if (!keys.isEmpty()) {
                this.add(keys.get(random.nextInt(keys.size())).getDefaultStack());
            }
        }
        for (int i = 0; i < config.exchangeStockDropRolls; i++) {
            this.roll(world, npc, npc.getType().getLootTableId(), random.nextLong(), asker, true);
        }
        if (random.nextDouble() < config.exchangeDungeonLootChance && !config.exchangeDungeonLootTables.isEmpty()) {
            this.dungeonLoot(world, npc, asker, random);
        }
        List<Item> weapons = npc.getWeaponPool();
        for (int i = 0; i < config.exchangeStockWeapons && !weapons.isEmpty(); i++) {
            this.add(weapons.get(random.nextInt(weapons.size())).getDefaultStack());
        }
        for (int i = 0; i < config.exchangeStockSupplyRolls; i++) {
            this.roll(world, npc, SUPPLIES, random.nextLong(), asker, false);
        }
        this.roll(world, npc, EntityType.ENDER_DRAGON.getLootTableId(), random.nextLong(), asker, false);
    }

    private void roll(ServerWorld world, MinibossEntity npc, RegistryKey<LootTable> key, long seed,
                      @Nullable PlayerEntity asker, boolean withoutBossKeys) {
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
        table.generateLoot(builder.build(LootContextTypes.ENTITY), seed, stack -> {
            if (!withoutBossKeys || !stack.isIn(BOSS_KEYS)) {
                this.add(stack);
            }
        });
    }

    /**
     * One roll of a random configured chest table. A chest fills many stacks, so only a few, picked at
     * random, are kept: enough to make the stock feel found rather than flooded.
     */
    private void dungeonLoot(ServerWorld world, MinibossEntity npc, @Nullable PlayerEntity asker, Random random) {
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
        found.stream().filter(stack -> !stack.isEmpty()).limit(Math.max(0, config.exchangeDungeonLootMaxStacks)).forEach(this::add);
    }

    /** Merges into matching stacks, then fills empty slots; whatever does not fit is left out. */
    private void add(ItemStack stack) {
        if (!stack.isEmpty()) {
            this.inventory.addStack(stack.copy());
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
        return nbt;
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        this.generated = nbt.getBoolean("Generated");
        DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, stacks, registries);
        for (int i = 0; i < SIZE; i++) {
            this.inventory.setStack(i, stacks.get(i));
        }
    }
}
