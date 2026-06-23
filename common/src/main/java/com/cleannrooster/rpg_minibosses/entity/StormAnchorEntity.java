package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;

public class StormAnchorEntity extends Entity {

    // ── Tracked data ─────────────────────────────────────────────────────────
    public static final TrackedData<Integer> OWNER_ID;
    public static final TrackedData<Float>   RADIUS;
    public static final TrackedData<Boolean> FADING;
    public static final TrackedData<Boolean> PLAYER_CENTERED;

    static {
        OWNER_ID        = DataTracker.registerData(StormAnchorEntity.class, TrackedDataHandlerRegistry.INTEGER);
        RADIUS          = DataTracker.registerData(StormAnchorEntity.class, TrackedDataHandlerRegistry.FLOAT);
        FADING          = DataTracker.registerData(StormAnchorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        PLAYER_CENTERED = DataTracker.registerData(StormAnchorEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    }

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float BASE_RADIUS     = 8.0F;
    private static final float PULSE_AMPLITUDE = 0.8F;
    private static final int   GROW_DURATION   = 40;  // ticks to fully grow
    private static final int   FADE_DURATION   = 30;  // ticks to fully fade
    private static final int   DAMAGE_INTERVAL = 10;  // ticks between DoT pulses
    private static final int   LIGHTNING_INTERVAL = 40; // ticks between lightning bursts
    private static final double PARTICLE_SEND_RANGE_SQ = 64.0 * 64.0;
    private static final int   WANDER_INTERVAL = 120; // ticks between wander target picks
    private static final double WANDER_RANGE   = 12.0; // max blocks from spawn anchor
    private static final double WANDER_SPEED   = 0.03; // lerp fraction per tick

    // ── Instance state ────────────────────────────────────────────────────────
    private int lifecycleTick = 0;
    private boolean growing   = true;
    private float rotationAngle = 0F; // continuously advances for orbital motion
    private int lifespan = 16 * 20 * 2; // default: 2 phase durations; override via setLifespan()
    private double spawnX, spawnZ;
    private boolean spawnSet = false;
    private double wanderTargetX, wanderTargetZ;
    private int wanderTimer = WANDER_INTERVAL; // fire immediately on first tick

    // ── Constructor ───────────────────────────────────────────────────────────
    public StormAnchorEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.noClip = true;
    }

    // ── Data tracker ──────────────────────────────────────────────────────────
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_ID, -1);
        builder.add(RADIUS, 0.0F);
        builder.add(FADING, false);
        builder.add(PLAYER_CENTERED, false);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("OwnerID"))      this.dataTracker.set(OWNER_ID, nbt.getInt("OwnerID"));
        this.growing        = nbt.getBoolean("Growing");
        this.lifecycleTick  = nbt.getInt("LifecycleTick");
        this.rotationAngle  = nbt.getFloat("RotationAngle");
        this.lifespan       = nbt.contains("Lifespan") ? nbt.getInt("Lifespan") : this.lifespan;
        this.spawnX         = nbt.getDouble("SpawnX");
        this.spawnZ         = nbt.getDouble("SpawnZ");
        this.spawnSet       = nbt.getBoolean("SpawnSet");
        this.wanderTargetX  = nbt.getDouble("WanderTargetX");
        this.wanderTargetZ  = nbt.getDouble("WanderTargetZ");
        this.wanderTimer    = nbt.getInt("WanderTimer");
        if (nbt.contains("PlayerCentered")) this.dataTracker.set(PLAYER_CENTERED, nbt.getBoolean("PlayerCentered"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("OwnerID",          this.dataTracker.get(OWNER_ID));
        nbt.putBoolean("Growing",      this.growing);
        nbt.putInt("LifecycleTick",    this.lifecycleTick);
        nbt.putFloat("RotationAngle",  this.rotationAngle);
        nbt.putInt("Lifespan",         this.lifespan);
        nbt.putDouble("SpawnX",        this.spawnX);
        nbt.putDouble("SpawnZ",        this.spawnZ);
        nbt.putBoolean("SpawnSet",     this.spawnSet);
        nbt.putDouble("WanderTargetX", this.wanderTargetX);
        nbt.putDouble("WanderTargetZ", this.wanderTargetZ);
        nbt.putInt("WanderTimer",      this.wanderTimer);
        nbt.putBoolean("PlayerCentered", this.dataTracker.get(PLAYER_CENTERED));
    }

    // ── API ───────────────────────────────────────────────────────────────────
    public void setOwner(GeminiEntity owner) {
        this.dataTracker.set(OWNER_ID, owner.getId());
    }

    /** General-purpose owner setter for non-Gemini owners (players, etc.). */
    public void setOwnerEntity(LivingEntity entity) {
        this.dataTracker.set(OWNER_ID, entity.getId());
    }

    public Entity getOwner() {
        int id = this.dataTracker.get(OWNER_ID);
        return (id == -1) ? null : this.getWorld().getEntityById(id);
    }

    public void startFading() {
        this.dataTracker.set(FADING, true);
        this.lifecycleTick = 0;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    /**
     * When true, the storm follows the owner entity (e.g. player caster) instead of
     * wandering, clears the inner 30 % of its radius so the owner can see, and does
     * not damage the owner.
     */
    public void setPlayerCentered(boolean value) {
        this.dataTracker.set(PLAYER_CENTERED, value);
    }

    @Override
    public boolean hasNoGravity() { return true; }

    // ── Main tick ─────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) return;
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;

        // ── Despawn guard (owner death only) ──
        Entity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            this.discard();
            return;
        }

        boolean playerCentered = this.dataTracker.get(PLAYER_CENTERED);

        // ── Anchor spawn position on first tick (wander mode only) ──
        if (!playerCentered && !spawnSet) {
            spawnX = this.getX();
            spawnZ = this.getZ();
            wanderTargetX = spawnX;
            wanderTargetZ = spawnZ;
            spawnSet = true;
        }

        // ── Auto-fade when lifespan nearly expired ──
        if (!this.dataTracker.get(FADING) && this.age >= lifespan - FADE_DURATION) {
            this.dataTracker.set(FADING, true);
            this.lifecycleTick = 0;
        }

        // ── Position update ──
        if (playerCentered) {
            trackOwnerPosition(owner); // follow caster
        } else {
            updateWander();            // independent drift
        }

        // ── Lifecycle ──
        lifecycleTick++;
        boolean fading = this.dataTracker.get(FADING);

        float currentRadius;
        if (fading) {
            float progress = Math.min(1.0F, (float) lifecycleTick / FADE_DURATION);
            currentRadius = BASE_RADIUS * (1.0F - progress);
            this.dataTracker.set(RADIUS, currentRadius);
            if (lifecycleTick >= FADE_DURATION) { this.discard(); return; }
        } else if (growing) {
            float progress = Math.min(1.0F, (float) lifecycleTick / GROW_DURATION);
            currentRadius = BASE_RADIUS * progress;
            this.dataTracker.set(RADIUS, currentRadius);
            if (lifecycleTick >= GROW_DURATION) growing = false;
        } else {
            // Active — gentle sinusoidal pulse
            float pulse = (float) Math.sin(this.age * 0.05) * PULSE_AMPLITUDE;
            currentRadius = BASE_RADIUS + pulse;
            this.dataTracker.set(RADIUS, currentRadius);
        }

        if (currentRadius <= 0.05F) return;

        // Advance rotation (radians per tick; ~7°/tick ≈ one full orbit in ~3 s)
        rotationAngle += 0.12F;

        // ── Particles (every tick, low counts) ──
        emitStormParticles(serverWorld, currentRadius, fading, playerCentered);

        // ── Disintegration particles on entities inside the storm ──
        if (!fading) {
            emitDisintegrationParticles(serverWorld, currentRadius, playerCentered);
        }

        // ── Periodic lightning burst ──
        if (!fading && this.age % LIGHTNING_INTERVAL == 0) {
            emitLightningBurst(serverWorld, currentRadius);
            serverWorld.playSound(null, this.getBlockPos(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE,
                    0.25F, 1.4F + random.nextFloat() * 0.4F);
        }

        // ── Ambient wind sound ──
        if (this.age % 25 == 0) {
            serverWorld.playSound(null, this.getBlockPos(),
                    SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.HOSTILE,
                    0.35F, 0.45F + random.nextFloat() * 0.1F);
        }

        // ── Damage tick ──
        if (!fading && this.age % DAMAGE_INTERVAL == 0) {
            applyStormDamage(serverWorld, owner, currentRadius, playerCentered);
        }
    }

    // ── Particle emission ─────────────────────────────────────────────────────

    /**
     * Main per-tick particle emission — four layers, each using explicit
     * tangential + radial velocity decomposition for a true spiral swirl.
     *
     *  • Main swirl    — PORTAL across the full disk, spiral-inward trajectory.
     *  • Outer wall    — LARGE_SMOKE at 60–100 % radius, strong tangential swirl
     *                    + slight inward pull (dense storm boundary).
     *  • Ground shadow — SQUID_INK at floor level, swirling inward.
     *  • Eye updraft   — WITCH wisps in the inner 35 %, gentle outward swirl + rise.
     *
     * Velocity is decomposed per-particle into:
     *   tangential = (-sin θ, 0, cos θ) × tangSpeed   (CCW rotation)
     *   radial     = (cos θ, 0, sin θ) × radialSpeed  (positive = outward)
     * Combining these with different ratios per layer drives the swirl feel.
     */
    private void emitStormParticles(ServerWorld world, float radius, boolean fading, boolean playerCentered) {
        if (radius < 0.2F) return;

        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();

        // When player-centered the inner 30 % is kept clear so the caster can see.
        // minInner is 0.10 normally, 0.30 when playerCentered.
        double minInner = playerCentered ? 0.30 : 0.10;

        // ── 1. Main swirl — PORTAL across disk ───────────────────────────────
        //    Random angles. Tangential speed scales with r (faster at the rim)
        //    + steady inward radial pull → spiral inward.
        for (int i = 0; i < 80; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r     = radius * (minInner + random.nextDouble() * (1.0 - minInner));
            double px    = cx + Math.cos(angle) * r;
            double pz    = cz + Math.sin(angle) * r;
            double py    = cy + random.nextDouble() * (2.0 + random.nextDouble() * 0.5);

            // Tangential — scales with normalised radius so rim particles swirl faster
            double tangSpeed   = 0.04 + (r / radius) * 0.10;
            // Inward radial — constant pull toward the eye
            double inwardSpeed = 0.018 + random.nextDouble() * 0.018;

            double vx = -Math.sin(angle) * tangSpeed  - Math.cos(angle) * inwardSpeed;
            double vz =  Math.cos(angle) * tangSpeed  - Math.sin(angle) * inwardSpeed;
            double vy = 0.010 + random.nextDouble() * 0.015;

            spawnDirected(world, ParticleTypes.PORTAL, px, py, pz, vx, vy, vz);
        }

        // ── 2. Outer swirl wall — LARGE_SMOKE at 60–100 % radius ─────────────
        //    Dense smoke ring with strong tangential + moderate inward pull.
        //    Emitted every other tick to keep counts manageable.
        if (this.age % 2 == 0) {
            for (int i = 0; i < 40; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r     = radius * (0.60 + random.nextDouble() * 0.40);
                double px    = cx + Math.cos(angle) * r;
                double pz    = cz + Math.sin(angle) * r;
                double py    = cy + random.nextDouble() * (2.0 + random.nextDouble() * 0.4);

                double tangSpeed   = 0.07 + random.nextDouble() * 0.04;
                double inwardSpeed = 0.010 + random.nextDouble() * 0.012;

                double vx = -Math.sin(angle) * tangSpeed  - Math.cos(angle) * inwardSpeed;
                double vz =  Math.cos(angle) * tangSpeed  - Math.sin(angle) * inwardSpeed;
                double vy = 0.005 + random.nextDouble() * 0.008;

                spawnDirected(world, ParticleTypes.LARGE_SMOKE, px, py, pz, vx, vy, vz);
            }
        }

        // ── 3. Ground shadow — SQUID_INK swirling inward at floor level ──────
        //    Flat disk, tangential + inward so the shadow itself visibly rotates.
        //    When player-centered, bias spawn toward outer 70 % to keep the centre clear.
        if (!fading && this.age % 3 == 0) {
            for (int i = 0; i < 40; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                // uniform disk biased to outer zone when player-centered
                double rNorm = playerCentered
                        ? (minInner + Math.sqrt(random.nextDouble()) * (1.0 - minInner))
                        : Math.sqrt(random.nextDouble());
                double r     = radius * rNorm;
                double px    = cx + Math.cos(angle) * r;
                double pz    = cz + Math.sin(angle) * r;
                double py    = cy + 0.02 + random.nextDouble() * 0.06;

                double tangSpeed   = 0.012 + random.nextDouble() * 0.010;
                double inwardSpeed = 0.008 + random.nextDouble() * 0.006;

                double vx = -Math.sin(angle) * tangSpeed  - Math.cos(angle) * inwardSpeed;
                double vz =  Math.cos(angle) * tangSpeed  - Math.sin(angle) * inwardSpeed;

                spawnDirected(world, ParticleTypes.SQUID_INK, px, py, pz, vx, 0.002, vz);
            }
        }

        // ── 4. Eye updraft — WITCH wisps in the inner 35 % ───────────────────
        //    Near the centre, particles swirl gently outward (eye divergence) and
        //    rise — opposite radial direction to the outer layers.
        //    Skipped when player-centered (inner zone is kept clear for visibility).
        if (!fading && !playerCentered && this.age % 4 == 0) {
            for (int i = 0; i < 80; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r     = radius * (random.nextDouble() * 0.35);
                double px    = cx + Math.cos(angle) * r;
                double pz    = cz + Math.sin(angle) * r;
                double py    = cy + random.nextDouble() * (2.0 + random.nextDouble() * 0.8);

                double tangSpeed    = 0.020 + random.nextDouble() * 0.020;
                double outwardSpeed = 0.010 + random.nextDouble() * 0.010; // outward in the eye

                double vx = -Math.sin(angle) * tangSpeed  + Math.cos(angle) * outwardSpeed;
                double vz =  Math.cos(angle) * tangSpeed  + Math.sin(angle) * outwardSpeed;
                double vy = 0.040 + random.nextDouble() * 0.040; // strong upward draft

                spawnDirected(world, ParticleTypes.WITCH, px, py, pz, vx, vy, vz);
            }
        }
    }

    /**
     * Periodic lightning burst — END_ROD particles flung outward from the centre.
     * They burst upward and outward, giving a brief "discharge" effect.
     */
    private void emitLightningBurst(ServerWorld world, float radius) {
        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();

        int count = 6 + random.nextInt(4); // 6–9 particles per burst
        for (int i = 0; i < count; i++) {
            // Random position inside the cylinder
            float angle = (float) (random.nextDouble() * Math.PI * 2);
            double r = radius * random.nextDouble();
            double px = cx + Math.cos(angle) * r;
            double pz = cz + Math.sin(angle) * r;
            double py = cy + random.nextDouble() * (2.0 + random.nextDouble() * 0.6); // height 0–2.6

            // Outward + upward burst
            double outSpeed = 0.15 + random.nextDouble() * 0.25;
            double vx = Math.cos(angle) * outSpeed;
            double vz = Math.sin(angle) * outSpeed;
            double vy = 0.2 + random.nextDouble() * 0.3;

            spawnDirected(world, ParticleTypes.END_ROD, px, py, pz, vx, vy, vz);
        }
    }

    /**
     * Per-tick disintegration effect for entities inside the storm.
     *
     * For each living entity within the storm radius, emits PORTAL + END_ROD particles
     * from random points across their bounding box. Velocity is predominantly tangential
     * (matching the storm's CCW rotation at that entity's angular position) with a slight
     * inward pull, so fragments visually "flow away" in the swirl direction.
     *
     * Particle count and spawn area scale with the entity's bounding-box footprint so
     * larger creatures produce proportionally more disintegration debris.
     */
    private void emitDisintegrationParticles(ServerWorld world, float radius, boolean playerCentered) {
        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();
        int ownerId = this.dataTracker.get(OWNER_ID);

        Box stormBox = new Box(cx - radius, cy - 1.0, cz - radius,
                               cx + radius, cy + 5.0, cz + radius);

        world.getEntitiesByClass(LivingEntity.class, stormBox, e -> {
            if (!e.isAlive()) return false;
            if (playerCentered && e.getId() == ownerId) return false; // spare the caster
            double dx = e.getX() - cx;
            double dz = e.getZ() - cz;
            return (dx * dx + dz * dz) <= (double) radius * radius;
        }).forEach(entity -> {
            double ex = entity.getX();
            double ey = entity.getY();
            double ez = entity.getZ();

            // Angle from storm centre to this entity
            double dx   = ex - cx;
            double dz   = ez - cz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.5) return; // skip entities at the very eye

            // Unit tangential direction (CCW: perpendicular to radius, left-hand rule)
            double tx = -dz / dist;
            double tz =  dx / dist;
            // Unit inward direction
            double inX = -dx / dist;
            double inZ = -dz / dist;

            // Bounding-box dimensions drive spawn spread and particle count
            Box bb       = entity.getBoundingBox();
            double width  = bb.maxX - bb.minX;  // horizontal footprint
            double height = bb.maxY - bb.minY;  // vertical extent
            // Particle count scales with surface area proxy (width² + width×height)
            double sizeScale = Math.sqrt(width * width + width * height);
            int particleCount = Math.max(2, (int) Math.round(sizeScale * 3.0));
            // Occasional bright spark count scales similarly
            int sparkCount = Math.max(1, (int) Math.round(sizeScale * 1.2));

            // ── Main disintegration stream — PORTAL particles ────────────────
            for (int i = 0; i < particleCount; i++) {
                // Spawn at a random point within the entity's volume
                double px = ex + (random.nextDouble() - 0.5) * width  * 0.9;
                double py = ey + random.nextDouble()         * height;
                double pz = ez + (random.nextDouble() - 0.5) * width  * 0.9;

                // Tangential dominates; slight inward pull + tiny random jitter
                double speed = 0.09 + random.nextDouble() * 0.07;
                double vx = tx  * speed
                          + inX * (0.015 + random.nextDouble() * 0.015)
                          + (random.nextDouble() - 0.5) * 0.015;
                double vz = tz  * speed
                          + inZ * (0.015 + random.nextDouble() * 0.015)
                          + (random.nextDouble() - 0.5) * 0.015;
                double vy = 0.015 + random.nextDouble() * 0.030;

                spawnDirected(world, ParticleTypes.PORTAL, px, py, pz, vx, vy, vz);
            }

            // ── Bright energy sparks — END_ROD every 2 ticks ────────────────
            if (this.age % 2 == 0) {
                for (int i = 0; i < sparkCount; i++) {
                    double px = ex + (random.nextDouble() - 0.5) * width;
                    double py = ey + random.nextDouble()         * height;
                    double pz = ez + (random.nextDouble() - 0.5) * width;

                    double speed = 0.12 + random.nextDouble() * 0.10;
                    double vx = tx * speed + (random.nextDouble() - 0.5) * 0.02;
                    double vz = tz * speed + (random.nextDouble() - 0.5) * 0.02;
                    double vy = 0.030 + random.nextDouble() * 0.050;

                    spawnDirected(world, ParticleTypes.END_ROD, px, py, pz, vx, vy, vz);
                }
            }
        });
    }

    /**
     * Sends a single directed particle to all players within PARTICLE_SEND_RANGE_SQ.
     *
     * Uses count=0 with force=true, which in Minecraft's protocol sends a single
     * particle at exactly (x, y, z) with velocity (vx, vy, vz).
     */
    private void spawnDirected(ServerWorld world, net.minecraft.particle.ParticleEffect particle,
                                double x, double y, double z,
                                double vx, double vy, double vz) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(x, y, z) < PARTICLE_SEND_RANGE_SQ) {
                world.spawnParticles(player, particle, true, x, y, z, 0, vx, vy, vz, 1.0);
            }
        }
    }

    // ── Independent wander ────────────────────────────────────────────────────

    /** Minimum distance from the PRIMARY djinn's feet to the storm's edge (blocks). */
    private static final double PRIMARY_CLEARANCE = 8.0;

    /**
     * Returns whichever Gemini djinn is currently in PRIMARY phase, or null.
     * Returns null immediately when the storm is player-centered (no repulsion needed).
     */
    private GeminiEntity findPrimaryDjinn() {
        if (this.dataTracker.get(PLAYER_CENTERED)) return null;
        Entity owner = getOwner();
        if (!(owner instanceof GeminiEntity gem)) return null;
        if (gem.phase == GeminiEntity.Phase.PRIMARY) return gem;
        return gem.getPartner(); // may be null if partner is dead
    }

    /**
     * Tracks the owner entity's X/Z position and keeps the storm at ground level
     * below them. Used in player-centered mode so the storm follows the caster.
     * Lerps at TRACK_SPEED per tick — fast enough to keep up with normal movement.
     */
    private static final double TRACK_SPEED = 0.20;

    private void trackOwnerPosition(Entity owner) {
        double targetX = owner.getX();
        double targetZ = owner.getZ();

        double newX = this.getX() + (targetX - this.getX()) * TRACK_SPEED;
        double newZ = this.getZ() + (targetZ - this.getZ()) * TRACK_SPEED;

        // Raycast down to ground
        Vec3d start = new Vec3d(newX, owner.getY() + 1.0, newZ);
        Vec3d end   = new Vec3d(newX, owner.getY() - 24.0, newZ);
        var hit = this.getWorld().raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this));

        double newY = this.getY() + (hit.getPos().getY() - this.getY()) * 0.15;
        this.setPosition(newX, newY, newZ);
    }

    /**
     * Drifts the storm independently of the djinn that spawned it.
     * Every WANDER_INTERVAL ticks a new random target is chosen within WANDER_RANGE
     * of the original spawn position; the storm lerps toward it at WANDER_SPEED.
     * After lerping, the storm is pushed away from the PRIMARY djinn so that its
     * edge is always at least PRIMARY_CLEARANCE blocks clear of them.
     * Y is always projected down to ground level via raycast.
     */
    private void updateWander() {
        wanderTimer++;
        if (wanderTimer >= WANDER_INTERVAL) {
            wanderTimer = 0;
            wanderTargetX = spawnX + (random.nextDouble() * 2 - 1) * WANDER_RANGE;
            wanderTargetZ = spawnZ + (random.nextDouble() * 2 - 1) * WANDER_RANGE;
        }

        double newX = this.getX() + (wanderTargetX - this.getX()) * WANDER_SPEED;
        double newZ = this.getZ() + (wanderTargetZ - this.getZ()) * WANDER_SPEED;

        // ── Repulsion from PRIMARY djinn ──────────────────────────────────────
        // Center must stay at least (BASE_RADIUS + PRIMARY_CLEARANCE) from PRIMARY.
        GeminiEntity primary = findPrimaryDjinn();
        if (primary != null) {
            double minCenterDist = BASE_RADIUS + PRIMARY_CLEARANCE;
            double dx = newX - primary.getX();
            double dz = newZ - primary.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < minCenterDist) {
                if (dist < 0.01) {
                    // Coincident — pick a random escape direction
                    double a = random.nextDouble() * Math.PI * 2;
                    dx = Math.cos(a);
                    dz = Math.sin(a);
                    dist = 1.0;
                }
                double scale = minCenterDist / dist;
                newX = primary.getX() + dx * scale;
                newZ = primary.getZ() + dz * scale;
                // Redirect wander target so we don't immediately drift back
                wanderTargetX = newX;
                wanderTargetZ = newZ;
            }
        }

        // Raycast down from current height to find ground
        Vec3d start = new Vec3d(newX, this.getY() + 5.0, newZ);
        Vec3d end   = new Vec3d(newX, this.getY() - 24.0, newZ);
        var hit = this.getWorld().raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this));

        double newY = this.getY() + (hit.getPos().getY() - this.getY()) * 0.15;
        this.setPosition(newX, newY, newZ);
    }

    // ── Damage ────────────────────────────────────────────────────────────────

    private void applyStormDamage(ServerWorld serverWorld, Entity owner, float radius, boolean playerCentered) {
        Box damageBox = new Box(
                this.getX() - radius, this.getY() - 1.0, this.getZ() - radius,
                this.getX() + radius, this.getY() + 4.0, this.getZ() + radius
        );

        int ownerId = this.dataTracker.get(OWNER_ID);
        List<LivingEntity> targets = this.getWorld().getEntitiesByClass(
                LivingEntity.class, damageBox,
                e -> e.isAlive()
                        && !(e instanceof GeminiEntity)
                        && e.squaredDistanceTo(this) <= (double) radius * radius
                        && !(playerCentered && e.getId() == ownerId) // never damage caster
        );

        var spellEntry = SpellRegistry.from(this.getWorld())
                .getEntry(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "deatomization_storm"));

        if (spellEntry.isPresent() && owner instanceof LivingEntity livingOwner) {
            net.minecraft.registry.entry.RegistryEntry<Spell> spell = spellEntry.get();
            for (LivingEntity target : targets) {
                SpellHelper.performImpacts(this.getWorld(), livingOwner, target, livingOwner,
                        spell, spell.value().impacts,
                        new SpellHelper.ImpactContext().position(this.getPos()));
            }
        } else {
            for (LivingEntity target : targets) {
                target.damage(this.getDamageSources().magic(), 4.0F);
            }
        }
    }
}
