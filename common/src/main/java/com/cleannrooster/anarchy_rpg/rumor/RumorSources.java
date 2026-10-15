package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Where rumors come from. Each source finds one real subject the asker has not yet been told truthfully, or
 * nothing. Structures and bosses are searched around the adventurer being asked; players anywhere.
 */
public final class RumorSources {
    /** Each structure tag {@code anarchy_rpg:rumorable/<kind>} is one kind of place rumors tell of. */
    private static final String KIND_PREFIX = "rumorable/";
    /** Entities that count as bosses for rumors, besides adventurers placed by worldgen encounters. */
    public static final TagKey<EntityType<?>> RUMOR_BOSSES = TagKey.of(RegistryKeys.ENTITY_TYPE, RPGMinibosses.id("rumor_bosses"));
    /** Something this close is right in front of you, not a rumor. */
    private static final double TOO_CLOSE = 32.0;

    private RumorSources() {
    }

    public static Optional<RumorSubject> find(RumorKind kind, ServerWorld world, MinibossEntity npc, ServerPlayerEntity asker,
                                              RumorState state, ServerConfig config, Random random) {
        return switch (kind) {
            case STRUCTURE -> structure(world, npc.getBlockPos(), asker.getUuid(), state, config, random);
            case BOSS -> boss(world, npc.getPos(), asker.getUuid(), state, config, random);
            case PLAYER -> player(world.getServer(), asker, state, random);
        };
    }

    /**
     * The kinds of structure a rumor can be about. Each tag under {@code anarchy_rpg:rumorable/} is one kind
     * (villages, outposts, temples...), located as a whole, so a modded village counts as a village. Mods named
     * in {@code rumorStructureNamespaces} add every structure of theirs not already in a kind, as one kind per
     * mod, for mods whose structures are not listed one by one.
     */
    private static List<RegistryEntryList<Structure>> structureKinds(ServerWorld world, ServerConfig config) {
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        List<RegistryEntryList<Structure>> kinds = new ArrayList<>();
        Set<Identifier> grouped = new HashSet<>();
        registry.streamTagsAndEntries().forEach(pair -> {
            Identifier tag = pair.getFirst().id();
            if (tag.getNamespace().equals(RPGMinibosses.CONTENT_NAMESPACE) && tag.getPath().startsWith(KIND_PREFIX)
                    && pair.getSecond().size() > 0) {
                kinds.add(pair.getSecond());
                pair.getSecond().forEach(entry -> entry.getKey().ifPresent(key -> grouped.add(key.getValue())));
            }
        });
        for (String namespace : config.rumorStructureNamespaces) {
            List<RegistryEntry<Structure>> ofMod = new ArrayList<>();
            registry.streamEntries().forEach(entry -> {
                Identifier id = entry.registryKey().getValue();
                if (id.getNamespace().equals(namespace) && !grouped.contains(id)) {
                    ofMod.add(entry);
                }
            });
            if (!ofMod.isEmpty()) {
                kinds.add(RegistryEntryList.of(ofMod));
            }
        }
        return kinds;
    }

    /**
     * The nearest structure of a random few kinds. {@code locateStructure} is costly, so at most
     * {@code rumorStructureAttempts} kinds are searched, once per question and never on a tick. Only the
     * nearest of each kind is considered: once you know of it, rumors move on to other kinds.
     */
    private static Optional<RumorSubject> structure(ServerWorld world, BlockPos origin, UUID asker, RumorState state,
                                                    ServerConfig config, Random random) {
        List<RumorSubject> found = structures(world, origin, asker, state, config, random, 1);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    /**
     * Up to {@code count} places, each the nearest of a different kind, none already told to {@code asker}. Each
     * extra place allows two more searches beyond {@code rumorStructureAttempts}, so a choice of three stays
     * cheap but rarely comes up short.
     */
    public static List<RumorSubject> structures(ServerWorld world, BlockPos origin, UUID asker, RumorState state,
                                                ServerConfig config, Random random, int count) {
        List<RegistryEntryList<Structure>> kinds = structureKinds(world, config);
        Collections.shuffle(kinds, new java.util.Random(random.nextLong()));
        int budget = config.rumorStructureAttempts + 2 * Math.max(0, count - 1);
        int attempts = 0;
        List<RumorSubject> places = new ArrayList<>();
        for (RegistryEntryList<Structure> kind : kinds) {
            if (attempts >= budget || places.size() >= count) {
                break;
            }
            attempts++;
            Pair<BlockPos, RegistryEntry<Structure>> found = world.getChunkManager().getChunkGenerator()
                    .locateStructure(world, kind, origin, config.rumorStructureRadiusChunks, false);
            if (found == null) {
                continue;
            }
            Optional<Identifier> id = found.getSecond().getKey().map(key -> key.getValue());
            if (id.isEmpty()) {
                continue;
            }
            BlockPos at = found.getFirst();
            Vec3d position = Vec3d.ofBottomCenter(at);
            if (horizontalDistance(Vec3d.ofBottomCenter(origin), position) < TOO_CLOSE) {
                continue;
            }
            String key = "structure:" + id.get() + "@" + (at.getX() >> 4) + "," + (at.getZ() >> 4);
            if (state.alreadyTold(asker, key)) {
                continue;
            }
            places.add(new RumorSubject(RumorKind.STRUCTURE, key, structureName(id.get()), world.getRegistryKey(), position, null));
        }
        return places;
    }

    /**
     * The structure's name from the lang file, else made from the last part of its id ("an illager hideout"),
     * so structures from mods nobody wrote a name for still read sensibly.
     */
    public static Text structureName(@org.jetbrains.annotations.Nullable Identifier id) {
        if (id == null) {
            return Text.translatable("rumor." + RPGMinibosses.CONTENT_NAMESPACE + ".structure.unknown");
        }
        String path = id.getPath();
        String words = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ').trim();
        String article = !words.isEmpty() && "aeiou".indexOf(Character.toLowerCase(words.charAt(0))) >= 0 ? "an " : "a ";
        return Text.translatableWithFallback("rumor." + RPGMinibosses.CONTENT_NAMESPACE + ".structure."
                + id.getNamespace() + "." + path.replace('/', '.'), article + words);
    }

    /**
     * A boss within {@code rumorBossRadius}: a loaded entity in {@link #RUMOR_BOSSES}, a loaded adventurer
     * placed by an encounter, or an encounter boss the {@link EncounterLedger} knows of in an unloaded chunk.
     */
    private static Optional<RumorSubject> boss(ServerWorld world, Vec3d origin, UUID asker, RumorState state,
                                               ServerConfig config, Random random) {
        double radius = config.rumorBossRadius;
        List<RumorSubject> candidates = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof LivingEntity living) || !isBoss(living)) {
                continue;
            }
            seen.add(entity.getUuid());
            double distance = horizontalDistance(origin, entity.getPos());
            if (distance < TOO_CLOSE || distance > radius) {
                continue;
            }
            String key = "boss:" + entity.getUuid();
            if (!state.alreadyTold(asker, key)) {
                candidates.add(new RumorSubject(RumorKind.BOSS, key, entity.getType().getName(), world.getRegistryKey(), entity.getPos(), null));
            }
        }
        EncounterLedger ledger = EncounterLedger.get(world.getServer());
        for (EncounterLedger.Placement placement : ledger.all()) {
            if (!placement.dimension().equals(world.getRegistryKey()) || seen.contains(placement.boss())) {
                continue;
            }
            if (world.getEntity(placement.boss()) != null) {
                // Loaded, yet not a boss any more (recruited, downed for good): no longer worth a rumor.
                ledger.remove(placement.boss());
                continue;
            }
            Vec3d position = Vec3d.ofBottomCenter(placement.pos());
            double distance = horizontalDistance(origin, position);
            String key = "boss:" + placement.boss();
            if (distance < TOO_CLOSE || distance > radius || state.alreadyTold(asker, key)) {
                continue;
            }
            Optional<EntityType<?>> type = Registries.ENTITY_TYPE.getOrEmpty(placement.entityType());
            if (type.isPresent()) {
                candidates.add(new RumorSubject(RumorKind.BOSS, key, type.get().getName(), placement.dimension(), position, null));
            }
        }
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    public static boolean isBoss(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity instanceof MinibossEntity miniboss) {
            return miniboss.isFromStructure() && miniboss.getOwnerUuid() == null && !miniboss.isIncapacitated();
        }
        return entity.getType().isIn(RUMOR_BOSSES);
    }

    /**
     * Another online player who takes part in the adventurers' world (has talked to one). A player can be
     * the subject again each in-game day, since players move: the memory key carries the day.
     */
    private static Optional<RumorSubject> player(MinecraftServer server, ServerPlayerEntity asker, RumorState state, Random random) {
        long day = server.getOverworld().getTime() / 24000L;
        List<RumorSubject> candidates = new ArrayList<>();
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other == asker || other.isSpectator() || !state.isRumorSubject(other.getUuid())) {
                continue;
            }
            String key = "player:" + other.getUuid() + "@" + day;
            if (!state.alreadyTold(asker.getUuid(), key)) {
                candidates.add(new RumorSubject(RumorKind.PLAYER, key, other.getName(), other.getServerWorld().getRegistryKey(), other.getPos(), other.getUuid()));
            }
        }
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    static double horizontalDistance(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
