package com.cleannrooster.anarchy_rpg.exchange;

import org.jetbrains.annotations.Nullable;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.recipe.Ingredient;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.HoeItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.enchantment.Enchantment;
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
 * tags. Armor and tools not listed are priced on their repair material ({@link #gearValue}): so many units of it
 * by piece (a chestplate is eight, a hoe half of one), with netherite gear its diamond counterpart plus the
 * ingot (a whole one for armor; for a tool, its share against a sword). That is used only when the material itself has a known value; otherwise, as with anything else not
 * listed, the item is priced from what it is: rarity, attribute modifiers and enchantments. Then it is scaled by
 * how worn it is. Values are per item, multiplied by the stack count.
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
        Double explicit = explicitValue(stack);
        Double gear = explicit == null ? gearValue(stack) : null;
        double value = explicit != null ? explicit : gear != null ? gear : heuristic(stack);
        if (stack.isDamageable() && stack.getMaxDamage() > 0) {
            double remaining = 1.0 - (double) stack.getDamage() / stack.getMaxDamage();
            value *= Math.max(0.1, remaining);
        }
        return Math.max(0.0, value);
    }

    /** Material units per piece of armor, by type: the recipe's count. */
    private static double armorUnits(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 5.0;
            case CHESTPLATE -> 8.0;
            case LEGGINGS -> 7.0;
            case BOOTS -> 4.0;
            case BODY -> 6.0;
        };
    }

    /** A sword's material units: the yardstick for how much of a netherite ingot a tool is worth. */
    private static final double SWORD_UNITS = 2.0;

    /**
     * Material units per tool, by usefulness rather than recipe: a hoe takes as much to make as a sword but is
     * worth far less. Anything else with a tool material (modded weapons) counts as a sword.
     */
    private static double toolUnits(Item item) {
        if (item instanceof PickaxeItem) return 2.5;
        if (item instanceof AxeItem) return 2.0;
        if (item instanceof ShovelItem) return 1.0;
        if (item instanceof HoeItem) return 0.5;
        return SWORD_UNITS;
    }

    /**
     * Armor or a tool priced on its repair material, before wear: units of the material, plus the rarity's
     * premium and enchantments. Netherite gear is its diamond counterpart plus one netherite ingot. Null when the
     * item is not armor or a tool, or its material has no known value (modded gear then keeps the heuristic).
     */
    private static @Nullable Double gearValue(ItemStack stack) {
        Item item = stack.getItem();
        Ingredient repair;
        double units;
        if (item instanceof ArmorItem armor) {
            repair = armor.getMaterial().value().repairIngredient().get();
            units = armorUnits(armor.getType());
        } else if (item instanceof ToolItem tool) {
            repair = tool.getMaterial().getRepairIngredient();
            units = toolUnits(item);
        } else {
            return null;
        }
        if (repair == null || repair.isEmpty()) {
            return null;
        }
        double material;
        if (repair.test(new ItemStack(Items.NETHERITE_INGOT))) {
            Double diamond = explicitValue(new ItemStack(Items.DIAMOND));
            Double ingot = explicitValue(new ItemStack(Items.NETHERITE_INGOT));
            if (diamond == null || ingot == null) {
                return null;
            }
            // Armor carries the whole ingot; a tool carries it in proportion to its usefulness against a sword's,
            // so a netherite hoe is not worth eleven diamond hoes.
            double ingotShare = item instanceof ArmorItem ? 1.0 : units / SWORD_UNITS;
            material = units * diamond + ingot * ingotShare;
        } else {
            Double each = null;
            for (ItemStack option : repair.getMatchingStacks()) {
                each = explicitValue(option);
                if (each != null) {
                    break;
                }
            }
            if (each == null) {
                return null;
            }
            material = units * each;
        }
        return material + rarityPremium(stack) + enchantmentValue(stack.getEnchantments());
    }

    /** What rarity adds over a common item of its kind. */
    private static double rarityPremium(ItemStack stack) {
        return switch (stack.getRarity()) {
            case COMMON -> 0.0;
            case UNCOMMON -> 2.0;
            case RARE -> 7.0;
            case EPIC -> 19.0;
        };
    }

    /** The table's value for one of this item (currency is 1), or null if the table does not price it. */
    private static @Nullable Double explicitValue(ItemStack stack) {
        if (stack.isOf(currency())) {
            return 1.0;
        }
        Table current = table;
        Double explicit = current.items().get(Registries.ITEM.getId(stack.getItem()));
        if (explicit != null) {
            return explicit;
        }
        for (Map.Entry<Identifier, Double> tag : current.tags().entrySet()) {
            if (stack.isIn(TagKey.of(RegistryKeys.ITEM, tag.getKey()))) {
                return tag.getValue();
            }
        }
        return null;
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

    /** An enchantment at its highest level; a lower level is worth its share of this. */
    private static final double FULL_ENCHANTMENT = 6.0;
    /** Treasure enchantments (Mending, Frost Walker...) can't come from a table, so they count this many times over. */
    private static final double TREASURE_FACTOR = 2.0;
    /** A curse takes this much away. The item never goes below nothing. */
    private static final double CURSE_PENALTY = 3.0;

    /**
     * Each enchantment is worth its level's share of its highest level, so Mending (I of I) counts as fully as
     * Sharpness V; treasure ones count double, and curses take value away.
     */
    private static double enchantmentValue(ItemEnchantmentsComponent enchantments) {
        double value = 0.0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            if (enchantment.isIn(EnchantmentTags.CURSE)) {
                value -= CURSE_PENALTY;
                continue;
            }
            int max = Math.max(1, enchantment.value().getMaxLevel());
            double worth = FULL_ENCHANTMENT * Math.min(entry.getIntValue(), max) / max;
            if (enchantment.isIn(EnchantmentTags.TREASURE)) {
                worth *= TREASURE_FACTOR;
            }
            value += worth;
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
