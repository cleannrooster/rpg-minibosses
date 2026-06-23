package com.cleannrooster.rpg_minibosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class MagusDominionOrbEntity extends Entity {

    public static final TrackedData<Integer> OWNER_ID;
    static {
        OWNER_ID = DataTracker.registerData(MagusDominionOrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
    }

    private static final float ORBIT_RADIUS   = 10f;
    private static final float BASE_SPEED     = 0.025f;
    private static final int   DAMAGE_INTERVAL = 10;

    public int disdainStacks  = 0;
    public int contemptStacks = 0;
    private float rotationAngle = 0f;

    public MagusDominionOrbEntity(EntityType<? extends MagusDominionOrbEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ID, -1);
    }

    public void setOwnerEntity(Entity owner) {
        this.dataTracker.set(OWNER_ID, owner.getId());
    }

    public void setStartAngle(float angle) {
        this.rotationAngle = angle;
    }

    private Entity getOwnerEntity() {
        int id = this.dataTracker.get(OWNER_ID);
        return id == -1 ? null : this.getWorld().getEntityById(id);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwnerEntity();
        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        // Steer toward nearest player every 20t
        if (this.age % 20 == 0) {
            List<PlayerEntity> nearby = this.getWorld().getEntitiesByType(
                    TypeFilter.instanceOf(PlayerEntity.class),
                    Box.of(owner.getPos(), 40, 20, 40), e -> true);
            if (!nearby.isEmpty()) {
                // Pick nearest
                PlayerEntity nearest = nearby.stream()
                        .min((a, b) -> Double.compare(a.squaredDistanceTo(owner), b.squaredDistanceTo(owner)))
                        .orElse(nearby.get(0));
                float targetAngle = (float) Math.atan2(
                        nearest.getZ() - owner.getZ(),
                        nearest.getX() - owner.getX());
                float diff = targetAngle - rotationAngle;
                while (diff >  Math.PI) diff -= 2f * Math.PI;
                while (diff < -Math.PI) diff += 2f * Math.PI;
                rotationAngle += diff * 0.15f;
            }
        }

        rotationAngle += BASE_SPEED + disdainStacks * 0.001f;

        double orbX = owner.getX() + Math.cos(rotationAngle) * ORBIT_RADIUS;
        double orbY = owner.getY() + 1.5;
        double orbZ = owner.getZ() + Math.sin(rotationAngle) * ORBIT_RADIUS;
        this.setPosition(orbX, orbY, orbZ);

        if (!this.getWorld().isClient() && this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    8, 0.3, 0.3, 0.3, 0.08);
            sw.spawnParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    2, 0.1, 0.1, 0.1, 0.02);

            if (this.age % DAMAGE_INTERVAL == 0) {
                float dmgRadius = 3.0f + disdainStacks * 0.2f;
                float damage    = 2.5f + contemptStacks * 0.15f;
                sw.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class),
                        this.getBoundingBox().expand(dmgRadius),
                        e -> e instanceof PlayerEntity
                ).forEach(e -> e.damage(sw.getDamageSources().magic(), damage));
            }
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("OwnerID"))       this.dataTracker.set(OWNER_ID, nbt.getInt("OwnerID"));
        this.rotationAngle  = nbt.getFloat("RotationAngle");
        this.disdainStacks  = nbt.getInt("DisdainStacks");
        this.contemptStacks = nbt.getInt("ContemptStacks");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("OwnerID",           this.dataTracker.get(OWNER_ID));
        nbt.putFloat("RotationAngle",   this.rotationAngle);
        nbt.putInt("DisdainStacks",     this.disdainStacks);
        nbt.putInt("ContemptStacks",    this.contemptStacks);
    }
}
