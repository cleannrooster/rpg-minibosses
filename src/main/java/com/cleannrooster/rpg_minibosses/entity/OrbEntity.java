package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;



public class OrbEntity extends Entity implements Ownable {

    private Entity owner;
    @Nullable
    private int ownerUuid;
    public SpellHelper.ImpactContext context;
    public int castCount = 0;
    public int deathStacks = 0;
    private float size = 0.0f;
    private int projTimer = 0;

    public void setCastCount(int count) {
        this.castCount = count;
        if (count > 0) {
            this.size = 0.3f;
        }
    }

    public void setDeathStacks(int stacks) { this.deathStacks = stacks; }

    public OrbEntity(EntityType<? extends OrbEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.noClip = true;
    }

    public static final TrackedData<Integer> COLOR;
    public static final TrackedData<Integer> OWNER;
    public static final TrackedData<Float> SIZE;

    static {
        COLOR = DataTracker.registerData(OrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
        OWNER = DataTracker.registerData(OrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
        SIZE  = DataTracker.registerData(OrbEntity.class, TrackedDataHandlerRegistry.FLOAT);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(COLOR, 1);
        builder.add(OWNER, -1);
        builder.add(SIZE, 0.0f);
    }

    public int getColor() { return this.dataTracker.get(COLOR); }
    public void setColor(int color) { this.dataTracker.set(COLOR, color); }
    public float getSize() { return this.dataTracker.get(SIZE); }

    @Override
    public void tick() {
        if (!this.getWorld().isClient()) {
            AzCommand.create("fly", "idle", AzPlayBehaviors.LOOP).sendForEntity(this);
        }
        if (this.age % 40 == 0) {
            this.playSound(SoundEvents.ENTITY_BLAZE_AMBIENT, 1, 0.5F);
        }

        if (!this.getWorld().isClient()) {
            // Sync size to tracked data
            size += 0.002f;
            this.dataTracker.set(SIZE, size);

            // Detonation check
            if (size >= 1.0f) {
                detonate();
                if (this.getOwner() instanceof MagusPrimeEntity magus) {
                    magus.activeDarkMatterOrb = null;
                }
                this.discard();
                return;
            }

            // Spawn homing projectiles at interval
            if (this.getOwner() instanceof LivingEntity livingEntity) {
                projTimer++;
                int projInterval = Math.max(20, 40 - deathStacks * 3);
                if (projTimer >= projInterval) {
                    projTimer = 0;
                    Identifier projId = Identifier.of(RPGMinibosses.MOD_ID, "magus_dark_matter_proj");
                    Optional<RegistryEntry.Reference<Spell>> projSpell = SpellRegistry.from(this.getWorld()).getEntry(projId);
                    if (projSpell.isPresent()) {
                        List<PlayerEntity> targets = this.getWorld().getEntitiesByType(
                                TypeFilter.instanceOf(PlayerEntity.class),
                                this.getBoundingBox().expand(48), e -> true);
                        int count = 1 + deathStacks / 2;
                        for (int i = 0; i < count && i < targets.size(); i++) {
                            SpellHelper.shootProjectile(this.getWorld(), livingEntity, targets.get(i),
                                    projSpell.get(), new SpellHelper.ImpactContext(1.0f, 1.0f,
                                            this.getPos(),
                                            SpellPower.getSpellPower(SpellSchools.SOUL, livingEntity),
                                            SpellTarget.FocusMode.DIRECT, 0));
                        }
                    }
                }
            }
        }

        super.tick();
    }

    public void onProjectileHit() {
        size = Math.min(1.0f, size + 0.08f);
        this.dataTracker.set(SIZE, size);
    }

    private void detonate() {
        if (!(this.getWorld() instanceof ServerWorld sw)) return;
        sw.spawnParticles(ParticleTypes.PORTAL,
                this.getX(), this.getY(), this.getZ(), 300, 4.0, 4.0, 4.0, 0.3);
        sw.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY(), this.getZ(), 3, 1.0, 1.0, 1.0, 0.1);
        this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 2.0f, 0.6f);
        sw.getEntitiesByType(TypeFilter.instanceOf(PlayerEntity.class),
                new Box(this.getX() - 8, this.getY() - 4, this.getZ() - 8,
                        this.getX() + 8, this.getY() + 4, this.getZ() + 8), e -> true)
                .forEach(p -> p.damage(sw.getDamageSources().magic(), 15.0f + deathStacks * 1.5f));
    }

    @Override
    public boolean canHit() {
        return false;
    }

    public void setOwner(@Nullable Entity entity) {
        if (entity != null) {
            this.ownerUuid = entity.getId();
            this.owner = entity;
            this.dataTracker.set(OWNER, entity.getId());
        }
    }

    @Nullable
    public Entity getOwner() {
        if (this.dataTracker.get(OWNER) != -1) {
            return this.getWorld().getEntityById(this.dataTracker.get(OWNER));
        } else {
            return null;
        }
    }

    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Color", (Integer) this.dataTracker.get(COLOR));
        nbt.putInt("Owner", this.ownerUuid);
        nbt.putFloat("Size", this.size);
        nbt.putInt("CastCount", this.castCount);
        nbt.putInt("DeathStacks", this.deathStacks);
    }

    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("Color")) {
            this.dataTracker.set(COLOR, nbt.getInt("Color"));
        }
        if (nbt.contains("Owner")) {
            this.ownerUuid = nbt.getInt("Owner");
            this.owner = null;
        }
        if (nbt.contains("Size")) {
            this.size = nbt.getFloat("Size");
            this.dataTracker.set(SIZE, this.size);
        }
        if (nbt.contains("CastCount")) this.castCount = nbt.getInt("CastCount");
        if (nbt.contains("DeathStacks")) this.deathStacks = nbt.getInt("DeathStacks");
    }
}
