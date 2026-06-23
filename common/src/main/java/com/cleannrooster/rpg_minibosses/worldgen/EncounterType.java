package com.cleannrooster.rpg_minibosses.worldgen;

import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.mojang.serialization.Codec;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.StringIdentifiable;

import java.util.function.Supplier;

/**
 * The five miniboss encounter vignettes. A single {@link MinibossEncounterFeature} renders all of
 * them; the configured feature only differs by which value of this enum it carries, so the layouts
 * stay data-driven while the placement/spawn logic is shared Java.
 *
 * <p>The entity type is resolved through a {@link Supplier} because the enum constants are created
 * at class-load (before {@link RPGMinibossesEntities#registerEntityTypes()} assigns
 * {@code entry.entityType}); capturing the field directly would capture {@code null}.</p>
 */
public enum EncounterType implements StringIdentifiable {
    BROKEN_TOLLGATE("broken_tollgate", () -> RPGMinibossesEntities.JUGGERNAUT_ENTITY_ENTRY.entityType),
    CONTRACT_CAMP("contract_camp", () -> RPGMinibossesEntities.ARTILLERIST_ENTITY_ENTRY.entityType),
    WAYSIDE_SHRINE("wayside_shrine", () -> RPGMinibossesEntities.TRICKSTER_ENTITY_ENTRY.entityType),
    SCORCHED_COURT("scorched_court", () -> RPGMinibossesEntities.ARCHMAGE_FIRE_ENTITY_ENTRY.entityType),
    DESECRATED_CHAPEL("desecrated_chapel", () -> RPGMinibossesEntities.TEMPLAR_ENTITY_ENTRY.entityType);

    public static final Codec<EncounterType> CODEC = StringIdentifiable.createCodec(EncounterType::values);

    private final String id;
    private final Supplier<EntityType<? extends PathAwareEntity>> entityType;

    EncounterType(String id, Supplier<EntityType<? extends PathAwareEntity>> entityType) {
        this.id = id;
        this.entityType = entityType;
    }

    /** Path of the configured/placed feature and biome tag for this encounter. */
    public String id() {
        return id;
    }

    public EntityType<? extends PathAwareEntity> entityType() {
        return entityType.get();
    }

    @Override
    public String asString() {
        return id;
    }
}
