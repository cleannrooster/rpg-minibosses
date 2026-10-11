package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.magic.MagicCaster;
import com.cleannrooster.rpg_minibosses.magic.AbilityExecutor;
import com.cleannrooster.rpg_minibosses.magic.AbilitySpec;
import com.cleannrooster.rpg_minibosses.magic.CastTarget;
import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import com.cleannrooster.rpg_minibosses.magic.TickScheduler;
import com.cleannrooster.rpg_minibosses.compat.SpellEngineCompat;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.GeminiAnimationProvider;
import com.cleannrooster.rpg_minibosses.entity.AI.*;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class GeminiEntity extends PathAwareEntity implements MagicCaster, Monster {

    public MagicSchool school;
    public boolean uber = false;

    private boolean spawned;
    private  ServerBossBar bossBar;
    private int stormEntityId = -1;
    /** Last role the boss bar was rendered for; {@code null} until the first refresh. */
    private Phase lastBossBarPhase = null;
    /** Whether the opening timers have been shortened for this twin's first engagement. */
    private boolean openingPrimed = false;
    private int driftTimer = 0;
    private double driftOffsetX = 0;
    private double driftOffsetZ = 0;
    private double lastTeleportAngle = Double.NaN;

    @Override
    protected Box calculateBoundingBox() {
        return super.calculateBoundingBox();
    }

    @Override
    public Box getBoundingBox(EntityPose pose) {
        return super.getBoundingBox(pose);
    }

    @Override
    protected EntityDimensions getBaseDimensions(EntityPose pose) {
        return new EntityDimensions(1,2.5F,1.8F,EntityAttachments.of(1,2.5F),false).scaled(this.getScaleFactor());
    }

    @Override
    public Box getVisibilityBoundingBox() {
        return super.getVisibilityBoundingBox();
    }

    protected GeminiEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.school = MagicSchool.FIRE;
        this.experiencePoints = 800;

        this.lookControl = new MinibossLookControl(this);
    }
    protected GeminiEntity(EntityType<? extends PathAwareEntity> entityType, World world, MagicSchool school) {
        super(entityType, world);
        this.experiencePoints = 800;

        this.school = school;
        this.bossBar = (ServerBossBar)(new ServerBossBar(this.getDisplayName(), school == MagicSchool.FROST ? BossBar.Color.BLUE : BossBar.Color.RED, BossBar.Style.PROGRESS)).setDarkenSky(true);

        this.lookControl = new MinibossLookControl(this);
    }
    protected GeminiEntity(EntityType<? extends PathAwareEntity> entityType, World world, MagicSchool school, boolean uber) {
        this(entityType, world, school);
        this.uber = uber;
        if (uber) {
            this.bossBar.setColor(BossBar.Color.PURPLE);
        }
    }
    /** Spell power stated by this boss's entry in {@code RPGMinibossesEntities}, from config. */
    @Override
    public double magicPower(MagicSchool school) {
        return RPGMinibossesEntities.magicPower(this.getType(), school);
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }
    public enum Phase {
     PRIMARY, SECONDARY;
    }
    public Phase phase = Phase.PRIMARY;

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("PrimaryPhase", phase == Phase.PRIMARY);
        if(this.partnerId != null){
            nbt.putUuid("partner",this.partnerId);
        }
        nbt.putBoolean("spawnedPartner",this.spawned);
        nbt.putBoolean("Uber", this.uber);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if(nbt.getBoolean("PrimaryPhase")){
            this.phase = Phase.PRIMARY;
        }
        else{
            this.phase = Phase.SECONDARY;
        }
        if(nbt.get("partner") != null) {
            this.partnerId = nbt.getUuid("partner");
        }
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
        this.spawned = nbt.getBoolean("spawnedPartner");
        // Only overwrite uber from NBT when the key is actually present (i.e. loaded from disk).
        // If absent (e.g. fresh /summon with no NBT), preserve the value set by the entity factory
        // constructor so that gemini_alpha_uber / gemini_beta_uber always behave as uber.
        if (nbt.contains("Uber")) {
            this.uber = nbt.getBoolean("Uber");
        }
        this.dataTracker.set(IS_UBER_DATA, this.uber);
        // Keep boss-bar colour in sync with the live uber flag
        if (this.uber) {
            this.bossBar.setColor(net.minecraft.entity.boss.BossBar.Color.PURPLE);
        }
    }
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        // Goes through the role refresh so a rename does not quietly drop the active-twin marker.
        refreshRoleBossBar();
    }
    public static final TrackedData<Boolean> IS_CLONES ;
    public static final TrackedData<Boolean> IS_UBER_DATA ;

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);

        builder.add(IS_CLONES, false);
        builder.add(IS_UBER_DATA, false);
    }

    static{
        IS_CLONES = DataTracker.registerData(GeminiEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        IS_UBER_DATA = DataTracker.registerData(GeminiEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    }
    public int phaseTime = 16*20;

    public void setPhase(Phase phase) {
        this.phase = phase;
    }
    protected void initGoals() {
        Predicate<LivingEntity> CAN_ATTACK_PREDICATE = (entity) -> {
            return !entity.getType().isIn(EntityTypeTags.WITHER_FRIENDS) && entity.isMobOrPlayer() && entity != this.getPartner();
        };
        this.targetSelector.add(1, new RevengeGoal(this, GeminiEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, 0, false, false, t -> !((ServerPlayerEntity)t).isSpectator() && !((ServerPlayerEntity)t).isCreative()));

        this.targetSelector.add(3, new ActiveTargetGoal(this, LivingEntity.class, 0, false, false, CAN_ATTACK_PREDICATE));



    }

    public int basicAttackCooldown = 4 * 20;
    public int strongAttackCooldown = 12*20;
    public int clonesCooldown = 20*20;
    public int clonesTimer = 20*20;
    public int strongAttackTimer = 12*20;
    public int basicAttackTimer = 4*20;
    public int meteorTimer = 16*20;
    public int meteorCooldown = 16*20;

    public int getClonesCooldown() { return uber ? 15 * 20 : clonesCooldown; }
    public int getTeleportCooldown() { return uber ? 80 : teleportCooldown; }

    public boolean acting = false;
    public int failSafe;

    protected boolean teleportRandomly() {
        if (!this.getWorld().isClient() && this.isAlive() && this.getTarget() != null) {
            LivingEntity target = this.getTarget();
            GeminiEntity partner = this.getPartner();
            double partnerAngle = partner == null ? Double.NaN
                    : Math.atan2(partner.getZ() - target.getZ(), partner.getX() - target.getX());

            // Lightweight pair bias: prefer the far side of the target and a different range band
            // from the partner, while retaining enough angular noise for the twins to feel organic.
            for (int attempt = 0; attempt < 8; attempt++) {
                double angle = Double.isNaN(partnerAngle)
                        ? this.random.nextDouble() * Math.PI * 2.0
                        : partnerAngle + Math.PI + this.random.nextGaussian() * 0.42;
                if (!Double.isNaN(lastTeleportAngle) && angularDistance(angle, lastTeleportAngle) < 0.65) {
                    angle += (this.random.nextBoolean() ? 1 : -1) * (0.65 + this.random.nextDouble() * 0.45);
                }
                double partnerRange = partner == null ? 10.0 : Math.sqrt(
                        MathHelper.square(partner.getX() - target.getX())
                                + MathHelper.square(partner.getZ() - target.getZ()));
                double range = partnerRange < 8.0 ? 11.0 + this.random.nextDouble() * 5.0
                        : 6.0 + this.random.nextDouble() * 5.0;
                // Grounded: the PRIMARY belongs on the floor, so sample at the target's own level
                // and land snapped to the ground there.
                double y = target.getY() + this.random.nextDouble();
                if (this.teleportTo(target.getX() + Math.cos(angle) * range, y,
                        target.getZ() + Math.sin(angle) * range, true)) {
                    lastTeleportAngle = angle;
                    faceTargetAfterTeleport(target);
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private static double angularDistance(double first, double second) {
        return Math.abs(MathHelper.wrapDegrees(Math.toDegrees(first - second)) * Math.PI / 180.0);
    }

    private void faceTargetAfterTeleport(LivingEntity target) {
        this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
        this.setYaw(this.headYaw);
        this.bodyYaw = this.getYaw();
    }

    public boolean isFrost() {
        return this.school == MagicSchool.FROST;
    }

    /**
     * Fires on both twins the instant PRIMARY and SECONDARY swap. Short on purpose — the player
     * needs to be able to name the active threat again within a few ticks, not watch a cutscene.
     */
    private void playRoleHandoffCue(boolean becomingPrimary) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        (becomingPrimary ? GeminiAnimationProvider.ROLE_ASCEND_COMMAND
                : GeminiAnimationProvider.ROLE_SETTLE_COMMAND).sendForEntity(this);
        if (becomingPrimary) {
            // It has been hovering as the SECONDARY, so drop it straight onto the floor rather than
            // letting it sink there over the next few seconds. Falling back on the drift's downward
            // push if no valid landing exists.
            teleportRandomly();
            // Rising twin: a bright flash plus a column of its own element.
            serverWorld.spawnParticles(ParticleTypes.FLASH,
                    this.getX(), this.getBodyY(0.65), this.getZ(), 2, 0.45, 0.7, 0.45, 0.02);
            serverWorld.spawnParticles(isFrost() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME,
                    this.getX(), this.getBodyY(0.5), this.getZ(), 40, 0.55, 1.1, 0.55, 0.09);
            this.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.25F, 1.15F);
            this.playSound(SoundEvents.ITEM_TRIDENT_THUNDER.value(), 0.7F, 1.4F);
        } else {
            // Settling twin: its element gutters out instead of flaring.
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getBodyY(0.65), this.getZ(), 10, 0.5, 0.5, 0.5, 0.01);
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getBodyY(0.4), this.getZ(), 14, 0.5, 0.4, 0.5, 0.005);
            this.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.75F, 0.85F);
        }
        refreshRoleBossBar();
    }

    /**
     * The boss bars are the one piece of UI both twins always occupy, so they carry the role too:
     * the active twin gets a marked, solid bar and the supporting one a segmented bar.
     */
    private void refreshRoleBossBar() {
        if (this.bossBar == null) return;
        boolean primary = this.phase == Phase.PRIMARY;
        this.bossBar.setName(primary
                ? Text.empty().append(this.getDisplayName()).append(Text.literal(" ✦"))
                : this.getDisplayName());
        this.bossBar.setStyle(primary ? BossBar.Style.PROGRESS : BossBar.Style.NOTCHED_10);
    }

    /**
     * Restrained drift so a twin never looks parked between casts.
     *
     * <p>The vertical half of this is role-specific and must stay that way: the encounter reads as
     * one twin down on the floor with you and one up in the sky, so only the SECONDARY gets lift.
     * Applying a desired hover height to both is what previously left neither of them grounded.
     */
    /**
     * Shortens the first of every timer once this twin actually has something to fight.
     *
     * <p>The cooldown constants are the fight's pacing and are left alone; these fields just happen
     * to start at a full cooldown, which meant the pair stood completely still for the first four
     * seconds after being summoned — with no movement goals and no gravity, that is indistinguishable
     * from a hard freeze. The alpha and beta are offset from each other so the opening does not land
     * on the same tick for both.
     */
    private void primeOpening() {
        if (openingPrimed || this.getWorld().isClient() || this.getTarget() == null) return;
        openingPrimed = true;
        int stagger = isAlphaType() ? 0 : 12;
        basicAttackTimer = 20 + stagger;
        teleportTimer = 30 + stagger;
        cloudTimer = 45 + stagger;
        strongAttackTimer = 70 + stagger;
        meteorTimer = 110 + stagger;
        clonesTimer = 200 + stagger;
    }

    /**
     * Target goals remain the normal long-term selector. This small fallback covers the encounter's
     * spawn edge: both twins are created in the same tick and previously entered their bespoke
     * state machine before either selector had committed a player target. With no movement goals or
     * gravity, that looked like a dead encounter and also prevented every spell callback from
     * running. Only valid survival players inside the existing spell envelope are considered.
     */
    private void acquireNearbyPlayerIfNeeded() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.age % 10 != 0) return;
        LivingEntity current = this.getTarget();
        if (current != null && current.isAlive()) return;

        ServerPlayerEntity closest = null;
        double closestDistance = 64.0 * 64.0;
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            double distance = this.squaredDistanceTo(player);
            if (distance <= closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }
        if (closest != null) {
            this.setTarget(closest);
            GeminiEntity partner = this.getPartner();
            if (partner != null && (partner.getTarget() == null || !partner.getTarget().isAlive())) {
                partner.setTarget(closest);
            }
        }
    }

    private void applyAerialDrift() {
        if (this.acting || this.getTarget() == null || this.age % 20 != 0) return;
        Vec3d toTarget = this.getTarget().getPos().subtract(this.getPos());
        Vec3d lateral = new Vec3d(-toTarget.z, 0.0, toTarget.x).normalize()
                .multiply(isAlphaType() ? 0.018 : -0.018);
        GeminiEntity partner = this.getPartner();
        Vec3d separation = Vec3d.ZERO;
        if (partner != null && this.squaredDistanceTo(partner) < 25.0) {
            separation = this.getPos().subtract(partner.getPos()).normalize().multiply(0.025);
        }
        Vec3d drift = this.getVelocity().multiply(0.75).add(lateral).add(separation);
        if (this.phase == Phase.PRIMARY) {
            // Grounded twin: same lateral wander, no lift. These have no gravity, so if one is off
            // the floor — a fresh handoff, or a world loaded mid-fight — it needs an explicit,
            // gentle push down or it would hang there forever.
            this.setVelocity(drift.x, this.isOnGround() ? 0.0 : -0.08, drift.z);
            return;
        }
        double desiredHeight = this.getTarget().getY() + 8.0;
        double vertical = MathHelper.clamp((desiredHeight - this.getY()) * 0.004, -0.018, 0.018);
        this.setVelocity(drift.add(0.0, vertical, 0.0));
    }
    protected boolean teleportRandomly(Entity entity, double radius) {
        if (!this.getWorld().isClient() && this.isAlive() && this.getTarget() != null) {
            double d = entity.getX() + (this.random.nextDouble() - 0.5) * radius;
            double e = entity.getY() + (double)(this.random.nextInt((int) radius) - radius/2);
            double f = entity.getZ() + (this.random.nextDouble() - 0.5) * radius;
            return this.teleportTo(d, e, f);
        } else {
            return false;
        }
    }
    public boolean canSee(Vec3d lookAt, Entity entity) {

            Vec3d vec3d = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
            Vec3d vec3d2 = lookAt;
            if (vec3d2.distanceTo(vec3d) > 128.0) {
                return false;
            } else {
                return this.getWorld().raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this)).getType() == HitResult.Type.MISS;
            }

    }

    private boolean teleportTo(double x, double y, double z) {
        return teleportTo(x, y, z, false);
    }

    /**
     * @param snapToGround land standing on the floor found beneath the candidate rather than at the
     *                     sampled height. The PRIMARY fights grounded, and these entities have no
     *                     gravity, so arriving a few blocks high would strand it there.
     */
    private boolean teleportTo(double x, double y, double z, boolean snapToGround) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

        while(mutable.getY() > this.getWorld().getBottomY() && !this.getWorld().getBlockState(mutable).blocksMovement()) {
            mutable.move(Direction.DOWN);
        }
        BlockState blockState = this.getWorld().getBlockState(mutable);
        boolean bl = blockState.blocksMovement();
        boolean bl2 = blockState.getFluidState().isIn(FluidTags.WATER);
        if (bl && !bl2 && this.getTarget() != null &&this.getWorld().isSkyVisible(mutable.up().up()) &&   canSee(mutable.up().toCenterPos(),this.getTarget()) && Math.abs(mutable.up().getY() - y) <= 2) {
            Vec3d vec3d = this.getPos();
            // Departure collapses inward at the old position; arrival blooms outward in this
            // twin's own element, so the player can tell which of the two just landed.
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getBodyY(0.5), this.getZ(),
                        18, 0.45, 0.8, 0.45, 0.08);
                serverWorld.spawnParticles(isFrost() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME,
                        this.getX(), this.getBodyY(0.5), this.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            }
            boolean bl3 = this.teleport(x, snapToGround ? mutable.getY() + 1 : y, z, true);
            if (bl3) {
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getBodyY(0.5), this.getZ(),
                            24, 0.55, 0.9, 0.55, 0.12);
                    serverWorld.spawnParticles(isFrost() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME,
                            this.getX(), this.getBodyY(0.5), this.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
                }
                this.getWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(this));
                if (!this.isSilent()) {
                    this.getWorld().playSound((PlayerEntity)null, this.prevX, this.prevY, this.prevZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT, this.getSoundCategory(), 1.0F, 1.0F);
                    this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                }
            }

            return bl3;
        } else {
            return false;
        }
    }
    public int teleportCooldown = 120;
    public int teleportTimer = 120;

    @Override
    public void tick() {
        if (!this.getWorld().isClient()) {
            // Re-assert the hover loop on a cadence. A single dispatch does not survive: the first
            // one goes out before any player is tracking this entity, so it reaches nobody and the
            // model never animates. Re-sending a LOOP command that is already playing is a no-op
            // rather than a restart — the original code did it every tick and the hover ran fine —
            // so a 20-tick cadence is enough to cover a fresh viewer without spamming the network.
            if (this.age % 20 == 0) {
                GeminiAnimationProvider.IDLE_COMMAND.sendForEntity(this);
            }
            // Sync uber tracked data
            if (this.uber && !this.dataTracker.get(IS_UBER_DATA)) {
                this.dataTracker.set(IS_UBER_DATA, true);
            }
        }
        // Auto-pair: any unpaired Gemini (alpha or beta, normal or uber) searches for a compatible partner.
        // Rules: uber pairs with uber, normal with normal; alpha pairs with beta.
        if (this.getPartner() == null && !this.getWorld().isClient() && !this.spawned) {
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                boolean thisIsAlpha = this.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA.entityType)
                        || this.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType);
                for (GeminiEntity other : serverWorld.getEntitiesByClass(GeminiEntity.class,
                        this.getBoundingBox().expand(64),
                        e -> e != this && e.getPartner() == null && !e.spawned)) {
                    // Must be opposite role (alpha↔beta) and same tier (uber↔uber or normal↔normal)
                    boolean otherIsAlpha = other.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA.entityType)
                            || other.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType);
                    if (thisIsAlpha == otherIsAlpha) continue; // both same role
                    if (this.uber != other.uber) continue;     // mismatched tier

                    // Establish the pair
                    other.setPartner(this);
                    this.setPartner(other);
                    this.spawned = true;
                    other.spawned = true;

                    // Alpha drives the phase timer; beta mirrors it
                    GeminiEntity alpha = thisIsAlpha ? this : other;
                    GeminiEntity beta  = thisIsAlpha ? other : this;
                    alpha.phaseTimer = 0;
                    beta.phaseTimer  = 0;
                    beta.setPhase(alpha.phase == Phase.PRIMARY ? Phase.SECONDARY : Phase.PRIMARY);

                    // Spawn initial storm for uber variant
                    if (this.uber) {
                        GeminiEntity secondary = alpha.phase == Phase.SECONDARY ? alpha : beta;
                        TickScheduler.schedule((ServerWorld) this.getWorld(), 10, secondary::spawnStorm);
                    }
                    break; // only pair with the first valid match
                }
            }
        }
        if(this.getTarget() != null && !this.acting){
            this.getLookControl().lookAt(this.getTarget().getEyePos());

        }

        super.tick();
        this.setYaw(this.headYaw);
        this.bodyYaw = this.getYaw();
        this.prevBodyYaw = this.bodyYaw;

    }
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
        // A player who arrives mid-fight needs the hover loop and the role marker straight away
        // rather than waiting up to a second for the next cadence tick.
        if (!this.getWorld().isClient()) {
            GeminiAnimationProvider.IDLE_COMMAND.sendForEntity(this);
            refreshRoleBossBar();
        }
    }

    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }
    @Override
    protected void mobTick() {

        super.mobTick();


        if(this.getWorld() instanceof ServerWorld serverWorld && this.getPartner() != null && isAlphaType() && phaseTimer > phaseTime){
            // Storm transition for uber variant — old storm fades via its own lifespan timer;
            // immediately spawn a new storm under the djinn that is about to become SECONDARY.
            if (this.uber) {
                GeminiEntity newSecondary = this.phase == Phase.PRIMARY ? this : this.getPartner();
                TickScheduler.schedule((ServerWorld) this.getWorld(), 10, newSecondary::spawnStorm);
            }
            this.setPhase(this.phase == Phase.PRIMARY ? Phase.SECONDARY : Phase.PRIMARY);
            this.getPartner().setPhase(this.phase == Phase.PRIMARY ? Phase.SECONDARY : Phase.PRIMARY);
            this.playRoleHandoffCue(this.phase == Phase.PRIMARY);
            this.getPartner().playRoleHandoffCue(this.getPartner().phase == Phase.PRIMARY);
            this.phaseTimer = 0;

        }
        if(this.getPartner() == null || this.getPartner().isDead() || this.getPartner().isRemoved()){
            this.setPhase(Phase.PRIMARY);
        }
        // Catches every route into a role change, including a lone survivor being promoted above,
        // without rebuilding the bar text on ticks where nothing moved.
        if (!this.getWorld().isClient() && this.phase != lastBossBarPhase) {
            lastBossBarPhase = this.phase;
            refreshRoleBossBar();
        }
        if(this.getWorld() instanceof ServerWorld serverWorld  && this.getPartner() != null && this.getPartner().getTarget() == null && this.getTarget() != null){
            this.getPartner().setTarget(this.getTarget());
        }
        acquireNearbyPlayerIfNeeded();
        primeOpening();
        if(this.phase.equals(Phase.PRIMARY)) {
            applyAerialDrift();
            if (this.getTarget() != null && !acting && clonesTimer <= 0) {
                acting = true;
                // Take the crossfire position first, then wind up. Dispatching the telegraph before
                // relocating meant this twin visibly began the cast and then vanished mid-pose.
                for (int i = 0; i < 8; i++) {
                    boolean bool = teleportRandomly();
                    teleportTimer = (int) (this.getTeleportCooldown() * (bool ? 1 : 0.5));
                    if (bool) break;
                }
                GeminiAnimationProvider.beamChannel(isFrost()).sendForEntity(this);
                TickScheduler.schedule((ServerWorld) this.getWorld(), 20, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 28, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 36, () -> {
                    beamPulse();
                });
                this.getDataTracker().set(IS_CLONES, true);

                TickScheduler.schedule((ServerWorld) this.getWorld(), 60, () -> {
                    acting = false;
                    this.getDataTracker().set(IS_CLONES, false);

                });
                clonesTimer = (int) (getClonesCooldown() + this.getRandom().nextGaussian() * 20);
                failSafe = 0;
            }
            if (this.getTarget() != null && !acting && strongAttackTimer <= 0) {
                acting = true;

                TickScheduler.schedule((ServerWorld) this.getWorld(), 20, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 28, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 36, () -> {
                    beamPulse();
                });
                GeminiAnimationProvider.beamChannel(isFrost()).sendForEntity(this);
                failSafe = 0;
                TickScheduler.schedule((ServerWorld) this.getWorld(), 60, () -> {
                    acting = false;
                });
                strongAttackTimer = (int) (strongAttackCooldown + this.getRandom().nextGaussian() * 20);
            }
            if (this.getTarget() != null && !acting && basicAttackTimer <= 0) {
                acting = true;
                GeminiAnimationProvider.beamSnap(isFrost()).sendForEntity(this);
                failSafe = 0;

                TickScheduler.schedule((ServerWorld) this.getWorld(), 10, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 30, () -> {
                    acting = false;
                });
                basicAttackTimer = (int) (basicAttackCooldown );
            }
            if (this.getTarget() != null && this.getTarget() != null && this.teleportTimer <= 0) {
                for (int i = 0; i < 8; i++) {
                    boolean bool = teleportRandomly();
                    teleportTimer = (int) (this.getTeleportCooldown() * (bool ? 1 : 0.5));
                    if (bool) break;
                }
            }
        }
        else{
            applyAerialDrift();
            if(this.getTarget() != null && (this.distanceTo(this.getTarget()) > 32 || this.getY() - this.getTarget().getY() < 4)){
                // Re-anchoring above the target is a hard snap, so it gets the same departure puff
                // the deliberate teleports do — otherwise the SECONDARY simply blinks with no cause.
                if (this.getWorld() instanceof ServerWorld anchorWorld) {
                    anchorWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                            this.getX(), this.getBodyY(0.5), this.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
                }
                // Uber lateral drift: offset hover position periodically
                if (this.uber) {
                    driftTimer++;
                    if (driftTimer >= 100) {
                        driftOffsetX = (this.random.nextDouble() - 0.5) * 16;
                        driftOffsetZ = (this.random.nextDouble() - 0.5) * 16;
                        driftTimer = 0;
                    }
                    Vec3d pos = this.getTarget().getPos().add(driftOffsetX, 8, driftOffsetZ);
                    this.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                } else {
                    Vec3d pos = this.getTarget().getPos().add(0, 8, 0);
                    this.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                }
                if (this.getWorld() instanceof ServerWorld anchorWorld) {
                    anchorWorld.spawnParticles(isFrost() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME,
                            this.getX(), this.getBodyY(0.5), this.getZ(), 14, 0.4, 0.6, 0.4, 0.04);
                }
                if(this.getWorld().getBlockState(BlockPos.ofFloored(this.getPos())).blocksMovement()){
                    if(this.teleportRandomly(this,12)){
                        Vec3d pos2 = this.getPos().add(0,8,0);
                        this.requestTeleport(pos2.getX(),pos2.getY(),pos2.getZ());
                    }

                }
            }

            if (this.getTarget() != null && !acting && cloudTimer <= 0) {
                acting = true;
                // Area denial reads as arms wide and low, pushing down — deliberately the opposite
                // shape to the overhead gather the comet cast uses.
                GeminiAnimationProvider.cloudSpread(isFrost()).sendForEntity(this);
                failSafe = 0;

                TickScheduler.schedule((ServerWorld) this.getWorld(), 40, () -> {
                    callCloud();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 60, () -> {
                    callCloud();                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 80, () -> {

                    callCloud();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 100, () -> {

                    callCloud();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 120, () -> {
                    acting = false;
                });
                cloudTimer = (int) (cloudCooldown + this.getRandom().nextGaussian() * 20 );
            }
            if (this.getTarget() != null && !acting && meteorTimer <= 0) {
                acting = true;
                GeminiAnimationProvider.cometCall(isFrost()).sendForEntity(this);
                failSafe = 0;

                TickScheduler.schedule((ServerWorld) this.getWorld(), 40, () -> {
                    callMeteor();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 60, () -> {
                    callMeteor();                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 80, () -> {

                    callMeteor();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 100, () -> {

                    callMeteor();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 120, () -> {
                    acting = false;
                });
                meteorTimer = (int) (meteorCooldown + this.getRandom().nextGaussian() * 20 );
            }
            // Aerial beam attacks — uber only (normal SECONDARY djinn uses meteors/clouds only)
            if (this.uber && this.getTarget() != null && !acting && strongAttackTimer <= 0) {
                acting = true;

                TickScheduler.schedule((ServerWorld) this.getWorld(), 20, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 28, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 36, () -> {
                    beamPulse();
                });
                GeminiAnimationProvider.beamChannel(isFrost()).sendForEntity(this);
                failSafe = 0;
                TickScheduler.schedule((ServerWorld) this.getWorld(), 60, () -> {
                    acting = false;
                });
                strongAttackTimer = (int) (strongAttackCooldown + this.getRandom().nextGaussian() * 20);
            }

            if (this.uber && this.getTarget() != null && !acting && basicAttackTimer <= 0) {
                acting = true;
                GeminiAnimationProvider.beamSnap(isFrost()).sendForEntity(this);
                failSafe = 0;

                TickScheduler.schedule((ServerWorld) this.getWorld(), 10, () -> {
                    beamPulse();
                });
                TickScheduler.schedule((ServerWorld) this.getWorld(), 30, () -> {
                    acting = false;
                });
                basicAttackTimer = (int) (basicAttackCooldown);
            }

            if(this.getPartner() != null && this.getPartner().distanceTo(this) > 32){
                this.teleportRandomly(this.getPartner(),16);
            }
        }
        if(!this.getWorld().isClient()){
            if(failSafe > 400){
                this.acting = false;
                this.getDataTracker().set(IS_CLONES,false);

            }
            meteorTimer--;
            cloudTimer--;
            phaseTimer++;
            clonesTimer--;
            strongAttackTimer--;
            basicAttackTimer--;
            failSafe++;
            teleportTimer--;
        }
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

    }
    public int cloudTimer = 14*20;
    public int cloudCooldown = 14*20;

    public int phaseTimer;
    public GeminiEntity partner;
    public UUID partnerId;

    // ── Casts ─────────────────────────────────────────────────────────────────

    /** One pulse of this twin's beam, along the line to its target (or its gaze, without one). */
    private void beamPulse() {
        if (!(this.getWorld() instanceof ServerWorld world)) return;
        var target = this.getTarget();
        var aim = target != null ? CastTarget.at(this, target)
                : CastTarget.at(this, this.getEyePos().add(this.getRotationVector().multiply(16.0)));
        AbilityExecutor.cast(world, this, GeminiAbilities.beam(isFrost()), aim);
    }

    /** Mark the target's spot now; the cloud rises there after the spell's delivery delay. */
    private void callCloud() {
        var target = this.getTarget();
        if (target == null || !(this.getWorld() instanceof ServerWorld world)) return;
        var spot = CastTarget.at(this, target.getPos());
        var spec = isFrost() ? GeminiAbilities.FROST_CLOUD : GeminiAbilities.FLAME_CLOUD;
        TickScheduler.schedule(world, GeminiAbilities.CLOUD_DELAY, () -> {
            if (this.isAlive()) {
                AbilityExecutor.cast(world, this, spec, spot);
            }
        });
    }

    /** Frost twin: one comet. Fire twin: a run of stars, each falling on wherever the target is by then. */
    private void callMeteor() {
        if (this.getTarget() == null || !(this.getWorld() instanceof ServerWorld world)) return;
        if (isFrost()) {
            dropOn(world, GeminiAbilities.COMET, GeminiAbilities.COMET_SPREAD);
            return;
        }
        dropOn(world, GeminiAbilities.STARFALL, GeminiAbilities.STARFALL_SPREAD);
        for (int i = 1; i < GeminiAbilities.STARFALL_STARS; i++) {
            TickScheduler.schedule(world, i * GeminiAbilities.STARFALL_INTERVAL, () -> {
                if (this.isAlive()) {
                    dropOn(world, GeminiAbilities.STARFALL, GeminiAbilities.STARFALL_SPREAD);
                }
            });
        }
    }

    /** One meteor onto the target, scattered the way the spell's launch divergence scattered it. */
    private void dropOn(ServerWorld world, AbilitySpec spec, double spread) {
        var target = this.getTarget();
        if (target == null) return;
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(this.random.nextDouble()) * spread;
        var spot = target.getPos().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
        AbilityExecutor.cast(world, this, spec, CastTarget.at(this, spot));
    }

    public void setPartner(GeminiEntity partner) {
        this.partner = partner;
        this.partnerId = partner.getUuid();
    }
    @Nullable
    public  GeminiEntity getPartner() {
        return  this.getWorld().isClient() ? null : partnerId == null ? null : (GeminiEntity)((ServerWorld)this.getWorld()).getEntity(partnerId);
    }
    public boolean isAlphaType() {
        return this.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA.entityType)
                || this.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType);
    }

    public void spawnStorm() {
        if (!this.uber || this.getWorld().isClient()) return;

        StormAnchorEntity storm = new StormAnchorEntity(RPGMinibossesEntities.STORM_ANCHOR, this.getWorld());
        storm.setPosition(this.getX(), this.getY() - 8, this.getZ());
        storm.setOwner(this);
        storm.setLifespan(phaseTime * 2);
        this.getWorld().spawnEntity(storm);
        this.stormEntityId = storm.getId();
    }

    public void removeStorm() {
        if (this.getWorld().isClient()) return;
        // Discard all StormAnchorEntity owned by this djinn (multiple may be live)
        this.getWorld().getEntitiesByClass(StormAnchorEntity.class,
                this.getBoundingBox().expand(128),
                s -> {
                    Entity owner = s.getOwner();
                    return owner != null && owner.getId() == this.getId();
                }
        ).forEach(Entity::discard);
        this.stormEntityId = -1;
    }

    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!this.uber || !(this.getWorld() instanceof ServerWorld serverWorld)) return;

        SpellEngineCompat.hooks().onGeminiUberDeath(this, serverWorld);
    }

    @Override
    public void remove(RemovalReason reason) {
        removeStorm();
        super.remove(reason);
    }

    public class MinibossLookControl extends LookControl {
        protected final MobEntity entity;
        protected float maxYawChange;
        protected float maxPitchChange;
        protected int lookAtTimer;
        protected double x;
        protected double y;
        protected double z;
        public MinibossLookControl(MobEntity entity) {
            super(entity);
            this.entity = entity;
        }


        public void lookAt(Vec3d direction) {
            this.lookAt(direction.x, direction.y, direction.z);
        }

        public void lookAt(Entity entity) {
            this.lookAt(entity.getX(), getLookingHeightFor(entity), entity.getZ());
        }

        public void lookAt(Entity entity, float maxYawChange, float maxPitchChange) {
            this.lookAt(entity.getX(), getLookingHeightFor(entity), entity.getZ(), maxYawChange, maxPitchChange);
        }

        public void lookAt(double x, double y, double z) {
            this.lookAt(x, y, z, (float)this.entity.getMaxLookYawChange(), (float)this.entity.getMaxLookPitchChange());
        }

        public void lookAt(double x, double y, double z, float maxYawChange, float maxPitchChange) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxYawChange = 30;
            this.maxPitchChange = 30;
            this.lookAtTimer = 8;
        }

        public void tick() {

                this.getTargetYaw().ifPresent((yaw) -> {
                    this.entity.setHeadYaw(this.changeAngle(this.entity.headYaw, yaw, this.maxYawChange));
                    this.entity.setBodyYaw(this.entity.getHeadYaw());

                    this.entity.prevHeadYaw = this.entity.headYaw;
                });
                this.getTargetPitch().ifPresent((pitch) -> {
                    this.entity.setPitch(this.changeAngle(this.entity.getPitch(), pitch, this.maxPitchChange));
                    this.entity.prevPitch = this.entity.getPitch();

                });

            this.clampHeadYaw();
        }
        protected void clampHeadYaw() {
            if (!this.entity.getNavigation().isIdle()) {
                this.entity.headYaw = MathHelper.clampAngle(this.entity.headYaw, this.entity.bodyYaw, (float)this.entity.getMaxHeadRotation());
            }

        }

        protected boolean shouldStayHorizontal() {
            return false;
        }

        public boolean isLookingAtSpecificPosition() {
            return this.lookAtTimer > 0;
        }

        public double getLookX() {
            return this.x;
        }

        public double getLookY() {
            return this.y;
        }

        public double getLookZ() {
            return this.z;
        }

        protected Optional<Float> getTargetPitch() {
            double d = this.x - this.entity.getX();
            double e = this.y - this.entity.getEyeY();
            double f = this.z - this.entity.getZ();
            double g = Math.sqrt(d * d + f * f);
            return !(Math.abs(e) > 9.999999747378752E-6) && !(Math.abs(g) > 9.999999747378752E-6) ? Optional.empty() : Optional.of((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)));
        }

        protected Optional<Float> getTargetYaw() {
            double d = this.x - this.entity.getX();
            double e = this.z - this.entity.getZ();
            return !(Math.abs(e) > 9.999999747378752E-6) && !(Math.abs(d) > 9.999999747378752E-6) ? Optional.empty() : Optional.of((float)(MathHelper.atan2(e, d) * 57.2957763671875) - 90.0F);
        }

        protected float changeAngle(float from, float to, float max) {
            float f = MathHelper.subtractAngles(from, to);
            float g = MathHelper.clamp(f, -max, max);
            return from + g;
        }

        private static double getLookingHeightFor(Entity entity) {
            return entity instanceof LivingEntity ? entity.getEyeY() : (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0;
        }
    }
}
