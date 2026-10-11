package com.cleannrooster.rpg_minibosses.compat.spellengine;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.compat.SpellEngineCompat;
import com.cleannrooster.rpg_minibosses.compat.spellengine.item.Armors;
import com.cleannrooster.rpg_minibosses.compat.spellengine.item.SummonHorn;
import com.cleannrooster.rpg_minibosses.compat.spellengine.magus.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.entity.StormAnchorEntity;
import com.cleannrooster.rpg_minibosses.magic.MagicBackend;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.InstrumentTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.item.ScrollItem;
import net.spell_engine.rpg_series.config.ConfigFile;
import net.spell_engine.rpg_series.loot.LootConfig;
import net.tiny_config.ConfigManager;

import java.util.List;

import static com.cleannrooster.rpg_minibosses.RPGMinibosses.id;
import static net.spell_engine.rpg_series.config.LootDefaults.itemLootConfig;

/**
 * Everything rpg-minibosses adds only when Spell Engine is installed: Forsaken Magus, the weapons and
 * armor sets, the item spells, spell scroll drops and loot-table injection. Instantiated reflectively by
 * {@link SpellEngineCompat#hooks()}; never reference this class from outside {@code compat.spellengine}.
 */
public final class SpellEngineContent implements SpellEngineCompat.Hooks {
    public static final ConfigManager<ConfigFile.Equipment> itemConfig = new ConfigManager<ConfigFile.Equipment>
            ("items_v2", new ConfigFile.Equipment())
            .builder()
            .setDirectory(RPGMinibosses.CONTENT_NAMESPACE)
            .sanitize(true)
            .build();

    public static Item LAVOSHORN;

    @Override
    public void initConfig() {
        itemConfig.refresh();
    }

    @Override
    public void init() {
        // Custom spell delivery for the summon_deatomization_storm scroll: spawn a StormAnchorEntity
        // that follows the caster for 30s.
        SpellHandlers.registerCustomDelivery(
                id("summon_storm"),
                (world, spellRegistry, caster, targets, context, position) -> {
                    StormAnchorEntity storm = new StormAnchorEntity(RPGMinibossesEntities.STORM_ANCHOR, world);
                    storm.setPosition(caster.getX(), caster.getY(), caster.getZ());
                    storm.setOwnerEntity(caster);
                    storm.setPlayerCentered(true);
                    storm.setLifespan(30 * 20);
                    world.spawnEntity(storm);
                    return true;
                }
        );
        CustomModels.registerModelIds(List.of(id("projectile/feather")));

        // Forsaken Magus grows faster with every player that dies near it.
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity) {
                entity.getWorld().getEntitiesByType(
                        TypeFilter.instanceOf(MagusPrimeEntity.class),
                        entity.getBoundingBox().expand(64),
                        magus -> true
                ).forEach(magus -> {
                    magus.contemptFulfilledStacks++;
                    EntityAttributeInstance atkSpeed = magus.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
                    if (atkSpeed != null) {
                        atkSpeed.removeModifier(MagusPrimeEntity.CONTEMPT_ATKSPEED_ID);
                        atkSpeed.addPersistentModifier(new EntityAttributeModifier(
                                MagusPrimeEntity.CONTEMPT_ATKSPEED_ID,
                                magus.contemptFulfilledStacks * 0.04,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                });
            }
            return EventResult.pass();
        });

        itemConfig.save();
    }

    @Override
    public void declareEntities() {
        SpellEngineEntities.declare();
    }

    @Override
    public void registerEntities() {
        SpellEngineEntities.register();

        String W1 = "#rpg_series:loot_tier/tier_1_weapons";
        String W2 = "#rpg_series:loot_tier/tier_2_weapons";
        String W3 = "#rpg_series:loot_tier/tier_3_weapons";
        String   A2 = "#rpg_series:loot_tier/tier_2_armors";
        String   X2 = "#rpg_series:loot_tier/tier_2_accessories";
        String   R2 = "#rpg_series:loot_tier/tier_2_relics";
        for(RPGMinibossesEntities.Entry entry : RPGMinibossesEntities.entries) {
            if(entry.shouldSpawn){
                itemLootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                        new LootConfig.Pool().bonus_rolls(0.2F).rolls(2)
                                .add(W1, true,3)
                                .add(W2, true,3)
                                .add(A2, true,2)
                                .add(X2)
                                .add(R2))
                ;
            }
            else{
                itemLootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                        new LootConfig.Pool().bonus_rolls(0.2F).rolls(2)
                                .add(W2, true,3)
                                .add(W3, true,3)

                                .add(A2, true,2)
                                .add(X2)
                                .add(R2));
            }
        }
    }

    @Override
    public void declareEffects() {
        SpellEngineEffects.declare();
    }

    @Override
    public void registerSummonItems() {
        LAVOSHORN = new SummonHorn<MagusPrimeEntity>(new Item.Settings().maxCount(1).maxDamage(1),
                SpellEngineEntities.MAGUS_PRIME.entityType, InstrumentTags.GOAT_HORNS);
        Registry.register(Registries.ITEM, id("lavos_horn"), LAVOSHORN);
        CreativeTabRegistry.append(CreativeTabRegistry.defer(RPGMinibossesEntities.KEY), LAVOSHORN);
    }

    @Override
    public void registerEquipment() {
        Armors.register(itemConfig.value.weapons);
        Armors.registerArmors(itemConfig.value.armor_sets);
    }

    @Override
    public MagicBackend magicBackend() {
        return SpellPowerMagicBackend.INSTANCE;
    }

    @Override
    public void onGeminiUberDeath(LivingEntity gemini, ServerWorld serverWorld) {
        Item scrollItemType = Registries.ITEM.get(ScrollItem.ID);
        if (scrollItemType == Items.AIR) return;

        // ── 3 random spell scrolls ───────────────────────────────────────────
        var allSpells = SpellRegistry.stream(serverWorld).toList();
        var streamSpells = allSpells.stream().filter(spellReference -> spellReference.isIn(TagKey.of(SpellRegistry.KEY,Identifier.tryParse("spell_engine:treasure"))));
        allSpells = streamSpells.toList();
        if (!allSpells.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                var entry = allSpells.get(gemini.getRandom().nextInt(allSpells.size()));
                ItemStack scroll = new ItemStack(scrollItemType);
                ScrollItem.applySpell(scroll, entry, ScrollItem.resolveSpellPool(serverWorld, entry));
                gemini.dropStack(scroll);
            }
        }

        // ── 25 % chance for a summon_deatomization_storm scroll ──────────────
        if (gemini.getRandom().nextFloat() < 0.25F) {
            SpellRegistry.from(serverWorld)
                .getEntry(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "summon_deatomization_storm"))
                .ifPresent(entry -> {
                    ItemStack stormScroll = new ItemStack(scrollItemType);
                    ScrollItem.applySpell(stormScroll, entry, ScrollItem.resolveSpellPool(serverWorld, entry));
                    gemini.dropStack(stormScroll);
                });
        }
    }
}
