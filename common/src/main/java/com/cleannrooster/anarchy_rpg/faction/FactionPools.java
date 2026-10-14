package com.cleannrooster.anarchy_rpg.faction;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a world's factions are rolled from: name translation keys, flag colors, flag patterns, and the
 * weights for drawing each relation. Loaded from {@code data/anarchy_rpg/faction_pool.json}, so a
 * datapack can replace it. The pools only matter the first time a world's roster is rolled; after that
 * the roster is read from the save.
 */
public record FactionPools(List<String> names, List<DyeColor> colors, List<Identifier> patterns,
                           Map<FactionRelation, Integer> relationWeights) {

    public static final Identifier RESOURCE = RPGMinibosses.id("faction_pool.json");

    private static final Codec<FactionRelation> RELATION_CODEC =
            Codec.STRING.xmap(name -> FactionRelation.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
                    relation -> relation.name().toLowerCase(java.util.Locale.ROOT));

    public static final Codec<FactionPools> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("names").forGetter(FactionPools::names),
            DyeColor.CODEC.listOf().fieldOf("colors").forGetter(FactionPools::colors),
            Identifier.CODEC.listOf().fieldOf("patterns").forGetter(FactionPools::patterns),
            Codec.unboundedMap(RELATION_CODEC, Codec.INT).fieldOf("relation_weights").forGetter(FactionPools::relationWeights)
    ).apply(instance, FactionPools::new));

    /** Used when the datapack file is missing or unusable. */
    public static final FactionPools DEFAULT = new FactionPools(
            List.of(nameKey("ashen_road"), nameKey("free_blades"), nameKey("oathbound"), nameKey("last_lantern"),
                    nameKey("broken_spear"), nameKey("wayfarers"), nameKey("ironbound"), nameKey("thornmarch"),
                    nameKey("quiet_hand"), nameKey("open_gate")),
            List.of(DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN, DyeColor.YELLOW, DyeColor.PURPLE,
                    DyeColor.ORANGE, DyeColor.CYAN, DyeColor.BLACK),
            List.of(Identifier.ofVanilla("stripe_center"), Identifier.ofVanilla("cross"),
                    Identifier.ofVanilla("border"), Identifier.ofVanilla("triangle_top"),
                    Identifier.ofVanilla("circle"), Identifier.ofVanilla("rhombus"),
                    Identifier.ofVanilla("half_horizontal"), Identifier.ofVanilla("stripe_downright")),
            new EnumMap<>(Map.of(FactionRelation.ALLIED, 1, FactionRelation.NEUTRAL, 3,
                    FactionRelation.RIVAL, 3, FactionRelation.AT_WAR, 2)));

    private static volatile FactionPools current = DEFAULT;

    private static String nameKey(String path) {
        return "faction." + RPGMinibosses.CONTENT_NAMESPACE + ".name." + path;
    }

    public static FactionPools current() {
        return current;
    }

    /** Whether these pools can roll a roster at all: three distinct names and colors, one pattern, a usable weight. */
    public boolean usable() {
        return this.names.stream().distinct().count() >= FactionSlot.values().length
                && this.colors.stream().distinct().count() >= FactionSlot.values().length
                && !this.patterns.isEmpty()
                && this.relationWeights.values().stream().anyMatch(weight -> weight > 0);
    }

    /** Reload hook: read the pool file from the server's data packs, keeping the defaults if it is unusable. */
    public static void reload(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(RESOURCE);
        if (resource.isEmpty()) {
            current = DEFAULT;
            return;
        }
        try (Reader reader = resource.get().getReader()) {
            FactionPools parsed = CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .resultOrPartial(error -> RPGMinibosses.LOGGER.warn("[{}] Bad {}: {}",
                            RPGMinibosses.CONTENT_NAMESPACE, RESOURCE, error))
                    .orElse(null);
            if (parsed == null || !parsed.usable()) {
                RPGMinibosses.LOGGER.warn("[{}] {} cannot roll three distinct guilds; using the built-in pools",
                        RPGMinibosses.CONTENT_NAMESPACE, RESOURCE);
                current = DEFAULT;
            } else {
                current = parsed;
            }
        } catch (Exception e) {
            RPGMinibosses.LOGGER.warn("[{}] Could not read {}", RPGMinibosses.CONTENT_NAMESPACE, RESOURCE, e);
            current = DEFAULT;
        }
    }
}
