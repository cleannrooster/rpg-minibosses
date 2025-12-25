package com.cleannrooster.rpg_minibosses.item;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.spell_engine.api.config.ArmorSetConfig;
import net.spell_engine.api.entity.SpellEngineAttributes;
import net.spell_engine.api.item.armor.Armor;
import net.spell_engine.api.item.set.EquipmentSet;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_power.api.SpellPowerMechanics;
import net.spell_power.api.SpellSchools;
import net.wizards.item.Group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.cleannrooster.rpg_minibosses.RPGMinibosses.MOD_ID;

public class SetBonuses {
    private static final String NAMESPACE = MOD_ID;
    private static final String SET_BONUS = "set_bonus";
    public record Entry(Identifier id, String title, Supplier<List<Identifier>> itemSupplier, List<EquipmentSet.Bonus> bonuses) { }
    public static final List<Entry> all = new ArrayList<>();
    private static Entry add(Entry entry) {
        all.add(entry);
        return entry;
    }

    private static AttributeModifiersComponent attribute(RegistryEntry<EntityAttribute> attribute, double value, EntityAttributeModifier.Operation operation, Identifier id) {
        return new AttributeModifiersComponent(
                List.of(
                        new AttributeModifiersComponent.Entry(
                                attribute,
                                new EntityAttributeModifier(
                                        id,
                                        value,
                                        operation
                                ),
                                AttributeModifierSlot.ARMOR)
                ),
                true
        );
    }
    public static Entry savant_red = add(savant_red());
    private static Entry savant_red() {
        var id = Identifier.of(NAMESPACE, "savant_red");
        return new Entry(id,
                "Red Savant Robes",
                () -> { return Armors.sanguine_red.pieceIds(); },
                List.of(
                        EquipmentSet.Bonus.withAttributes(2, attribute(
                                SpellPowerMechanics.CRITICAL_DAMAGE.attributeEntry,
                                1F,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                                id.withPath(SET_BONUS))
                        ),
                        EquipmentSet.Bonus.withSpells(2, SpellContainerHelper.createForModifier(Identifier.of(MOD_ID,"savant")))
                )
        );
    }
    public static Entry savant_blue = add(savant_blue());
    private static Entry savant_blue() {
        var id = Identifier.of(NAMESPACE, "savant_blue");
        return new Entry(id,
                "Blue Savant Robes",
                () -> { return Armors.sanguine_blue.pieceIds(); },
                List.of(
                        EquipmentSet.Bonus.withAttributes(2, attribute(
                                SpellPowerMechanics.CRITICAL_DAMAGE.attributeEntry,
                                1F,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                                id.withPath(SET_BONUS))
                        ),
                        EquipmentSet.Bonus.withSpells(2, SpellContainerHelper.createForModifier(Identifier.of(MOD_ID,"savant")))
                )
        );
    }
    public static Entry savant_purple = add(savant_purple());
    private static Entry savant_purple() {
        var id = Identifier.of(NAMESPACE, "savant_purple");
        return new Entry(id,
                "Purple Savant Robes",
                () -> { return Armors.sanguine_purple.pieceIds(); },
                List.of(
                        EquipmentSet.Bonus.withAttributes(2, attribute(
                                SpellPowerMechanics.CRITICAL_DAMAGE.attributeEntry,
                                1F,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                                id.withPath(SET_BONUS))
                        ),
                        EquipmentSet.Bonus.withSpells(2, SpellContainerHelper.createForModifier(Identifier.of(MOD_ID,"savant")))
                )
        );
    }

    public static Entry despot = add(despot());
    private static Entry despot() {
        var id = Identifier.of(NAMESPACE, "despot");
        return new Entry(id,
                "Despot's Heart",
                () -> { return Armors.despotArmor.pieceIds(); },
                List.of(
                        EquipmentSet.Bonus.withAttributes(2, attribute(
                                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                                0.5F,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                                id.withPath(SET_BONUS))
                        ),
                        EquipmentSet.Bonus.withSpells(2, SpellContainerHelper.createForModifier(Identifier.of(MOD_ID,"despot")))
                )
        );
    }
    public static Entry kintsugi = add(kintsugi());
    private static Entry kintsugi() {
        var id = Identifier.of(NAMESPACE, "kintsugi");
        return new Entry(id,
                "Aureate Flow",
                () -> { return Armors.kintsugiArmor.pieceIds(); },
                List.of(
                        EquipmentSet.Bonus.withAttributes(2, attribute(
                                EntityAttributes.GENERIC_ARMOR,
                                0.25F,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                                id.withPath(SET_BONUS))
                        ),
                        EquipmentSet.Bonus.withSpells(2, SpellContainerHelper.createForModifier(Identifier.of(MOD_ID,"kintsugi")))
                )
        );
    }
    public static Entry foxshade = add(foxshade());
    private static Entry foxshade() {
        var id = Identifier.of(NAMESPACE, "foxshade");
        return new Entry(id,
                "Fox's Cunning",
                () -> { return Armors.foxArmor.pieceIds(); },
                List.of(
                        EquipmentSet.Bonus.withAttributes(2, attribute(
                                SpellEngineAttributes.EVASION_CHANCE.entry,
                                0.4F,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                                id.withPath(SET_BONUS))
                        ),
                        EquipmentSet.Bonus.withSpells(2, SpellContainerHelper.createForModifier(Identifier.of(MOD_ID,"foxshade")))
                )
        );
    }

}
