package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.exchange.ExchangeStock;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Mercenary, "Gamble" (camp service): a price you can see for an item you cannot. The item is one stack from a
 * random roll of one of the configured dungeon chest tables (the ones Exchange's dungeon loot comes from), boss
 * keys left out; some gambles will be worth less than they cost.
 *
 * <p>The price is rolled, between {@code campGambleMinPrice} and {@code campGambleMaxPrice} before standing, when
 * the payment window opens, and holds for that window: reopening it is the only way to roll again. Up to
 * {@code campGambleDailyUses} gambles a day per Mercenary, per player.
 */
final class GambleService extends CampService {
    /** Tables tried before giving up on a roll that came up empty (nothing is then taken). */
    private static final int TABLE_TRIES = 3;

    /** The price rolled for a player's open window: whose, and the base before standing. */
    private record Roll(UUID npc, double base) {
    }

    private static final Map<UUID, Roll> ROLLS = new HashMap<>();

    static void registerCleanup() {
        PlayerEvent.PLAYER_QUIT.register(player -> ROLLS.remove(player.getUuid()));
    }

    @Override
    protected PaidServiceKind kind() {
        return PaidServiceKind.GAMBLE;
    }

    @Override
    protected double basePrice(ServerConfig config) {
        return config.campGambleMinPrice;
    }

    @Override
    protected int dailyUses() {
        return Math.max(1, RPGMinibossesEntities.config.campGambleDailyUses);
    }

    /** The conversation gate: each window gets a fresh roll. */
    static boolean beforeOpen(MinibossEntity npc, ServerPlayerEntity player) {
        ServerConfig config = RPGMinibossesEntities.config;
        double min = Math.min(config.campGambleMinPrice, config.campGambleMaxPrice);
        double max = Math.max(config.campGambleMinPrice, config.campGambleMaxPrice);
        double base = min + npc.getRandom().nextDouble() * (max - min);
        ROLLS.put(player.getUuid(), new Roll(npc.getUuid(), Math.round(base * 2.0) / 2.0));
        return true;
    }

    @Override
    public double price(MinibossEntity npc, ServerPlayerEntity player) {
        Roll roll = ROLLS.get(player.getUuid());
        double base = roll != null && roll.npc().equals(npc.getUuid()) ? roll.base() : this.basePrice(RPGMinibossesEntities.config);
        return CampServices.price(npc, player, base);
    }

    @Override
    protected String priceLabel(MinibossEntity npc, ServerPlayerEntity player) {
        ServerConfig config = RPGMinibossesEntities.config;
        double low = CampServices.price(npc, player, Math.min(config.campGambleMinPrice, config.campGambleMaxPrice));
        double high = CampServices.price(npc, player, Math.max(config.campGambleMinPrice, config.campGambleMaxPrice));
        return String.format(Locale.ROOT, "%.0f-%.0f", low, high);
    }

    @Override
    protected boolean deliver(MinibossEntity npc, ServerPlayerEntity player) {
        if (!(npc.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        ItemStack won = roll(world, npc, player);
        if (won.isEmpty()) {
            return false; // Every table came up empty: no gamble, nothing taken.
        }
        ROLLS.remove(player.getUuid());
        player.getInventory().offerOrDrop(won.copy());
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.NEUTRAL, 0.8F, 0.8F);
        player.sendMessage(npc.getName().copy().append(": ").append(CampServices.line("gamble.done", won.getCount(), won.getName())));
        return true;
    }

    /** One stack from a random configured chest table, boss keys left out; empty if every try comes up empty. */
    private static ItemStack roll(ServerWorld world, MinibossEntity npc, ServerPlayerEntity player) {
        List<String> tables = RPGMinibossesEntities.config.exchangeDungeonLootTables;
        if (tables.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Random random = npc.getRandom();
        for (int attempt = 0; attempt < TABLE_TRIES; attempt++) {
            Identifier id = Identifier.tryParse(tables.get(random.nextInt(tables.size())));
            if (id == null) {
                continue;
            }
            LootTable table = world.getServer().getReloadableRegistries().getLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE, id));
            LootContextParameterSet parameters = new LootContextParameterSet.Builder(world)
                    .add(LootContextParameters.ORIGIN, npc.getPos())
                    .add(LootContextParameters.THIS_ENTITY, player)
                    .luck(player.getLuck())
                    .build(LootContextTypes.CHEST);
            List<ItemStack> found = new ArrayList<>();
            table.generateLoot(parameters, random.nextLong(), stack -> {
                if (!stack.isEmpty() && !stack.isIn(ExchangeStock.BOSS_KEYS)) {
                    found.add(stack);
                }
            });
            if (!found.isEmpty()) {
                return found.get(random.nextInt(found.size()));
            }
        }
        return ItemStack.EMPTY;
    }
}
