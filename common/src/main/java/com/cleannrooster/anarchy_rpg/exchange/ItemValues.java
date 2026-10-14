package com.cleannrooster.anarchy_rpg.exchange;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What items are worth in Exchange, in units of the currency item (which is always worth exactly 1).
 *
 * <p>Explicit values come from {@code data/anarchy_rpg/item_values.json}: exact item ids first, then item
 * tags. Anything not listed is priced from what it is: rarity, its attribute modifiers, its enchantments,
 * and how worn it is. Values are per item, multiplied by the stack count.
 */
public final class ItemValues {
    public static final Identifier RESOURCE = RPGMinibosses.id("item_values.json");

    record Table(Map<Identifier, Double> items, Map<Identifier, Double> tags) {
        static final Table EMPTY = new Table(Map.of(), Map.of());
        static final Codec<Table> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Identifier.CODEC, Codec.DOUBLE).optionalFieldOf("items", Map.of()).forGetter(Table::items),
                Codec.unboundedMap(Identifier.CODEC, Codec.DOUBLE).optionalFieldOf("tags", Map.of()).forGetter(Table::tags)
        ).apply(instance, Table::new));
    }

    private static volatile Table table = Table.EMPTY;

    private ItemValues() {
    }

    /** Reload hook: read the value table from the server's data packs. */
    public static void reload(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(RESOURCE);
        if (resource.isEmpty()) {
            table = Table.EMPTY;
            return;
        }
        try (Reader reader = resource.get().getReader()) {
            table = Table.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .resultOrPartial(error -> RPGMinibosses.LOGGER.warn("[{}] Bad {}: {}", RPGMinibosses.CONTENT_NAMESPACE, RESOURCE, error))
                    .orElse(Table.EMPTY);
        } catch (Exception e) {
            RPGMinibosses.LOGGER.warn("[{}] Could not read {}", RPGMinibosses.CONTENT_NAMESPACE, RESOURCE, e);
            table = Table.EMPTY;
        }
    }

    public static Item currency() {
        Identifier id = Identifier.tryParse(RPGMinibossesEntities.config.currencyItem);
        Item item = id == null ? null : Registries.ITEM.get(id);
        return item == null || item == net.minecraft.item.Items.AIR ? net.minecraft.item.Items.EMERALD : item;
    }

    /** The whole stack's value. */
    public static double value(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        return perItem(stack) * stack.getCount();
    }

    private static double perItem(ItemStack stack) {
        if (stack.isOf(currency())) {
            return 1.0;
        }
        Table current = table;
        Double explicit = current.items().get(Registries.ITEM.getId(stack.getItem()));
        if (explicit == null) {
            for (Map.Entry<Identifier, Double> tag : current.tags().entrySet()) {
                if (stack.isIn(TagKey.of(RegistryKeys.ITEM, tag.getKey()))) {
                    explicit = tag.getValue();
                    break;
                }
            }
        }
        double value = explicit != null ? explicit : heuristic(stack);
        if (stack.isDamageable() && stack.getMaxDamage() > 0) {
            double remaining = 1.0 - (double) stack.getDamage() / stack.getMaxDamage();
            value *= Math.max(0.1, remaining);
        }
        return Math.max(0.0, value);
    }

    /** Rarity sets the floor; attributes and enchantments add to it. */
    private static double heuristic(ItemStack stack) {
        double value = switch (stack.getRarity()) {
            case COMMON -> stack.getMaxCount() > 1 ? 0.1 : 1.0;
            case UNCOMMON -> 3.0;
            case RARE -> 8.0;
            case EPIC -> 20.0;
        };
        AttributeModifiersComponent modifiers = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            double amount = Math.abs(entry.modifier().value());
            if (entry.modifier().operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                value += amount * attributeWeight(entry.attribute());
            } else {
                value += amount * 10.0;
            }
        }
        value += enchantmentValue(stack.getEnchantments());
        value += enchantmentValue(stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        return value;
    }

    /** Attack speed is negative on most weapons and says little about worth, so it counts for nothing. */
    private static double attributeWeight(RegistryEntry<EntityAttribute> attribute) {
        if (attribute.matches(EntityAttributes.GENERIC_ATTACK_SPEED)) return 0.0;
        if (attribute.matches(EntityAttributes.GENERIC_ARMOR)) return 1.5;
        if (attribute.matches(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)) return 2.0;
        if (attribute.matches(EntityAttributes.GENERIC_MAX_HEALTH)) return 0.5;
        return 1.0;
    }

    private static double enchantmentValue(ItemEnchantmentsComponent enchantments) {
        double value = 0.0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            value += entry.getIntValue() * 1.5;
        }
        return value;
    }

    /** The total value of a list of stacks. */
    public static double total(List<ItemStack> stacks) {
        double total = 0.0;
        for (ItemStack stack : stacks) {
            total += value(stack);
        }
        return total;
    }
}
