package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.AI.*;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.*;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.object.PlayState;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellPowerMechanics;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class GeminiEntity extends PathAwareEntity implements GeoEntity, Monster {

    public SpellSchool school;


    public static final RawAnimation METEOR_CHARGE = RawAnimation.begin().then("animation.awakener.meteor_charge", Animation.LoopType.PLAY_ONCE);

    public static final RawAnimation METEOR_CHANNEL = RawAnimation.begin().then("animation.awakener.meteor_channel", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation BEAM_LARGE  = RawAnimation.begin().then("animation.awakener.beam_large", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation BEAM = RawAnimation.begin().then("animation.awakener.beam_1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.awakener.idle");
    private boolean spawned;
    private  ServerBossBar bossBar;

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
        this.school = SpellSchools.FIRE;
        this.experiencePoints = 800;

        this.lookControl = new MinibossLookControl(this);
    }
    protected GeminiEntity(EntityType<? extends PathAwareEntity> entityType, World world, SpellSchool school) {
        super(entityType, world);
        this.experiencePoints = 800;

        this.school = school;
        this.bossBar = (ServerBossBar)(new ServerBossBar(this.getDisplayName(), school.equals(SpellSchools.FROST) ? BossBar.Color.BLUE : BossBar.Color.RED, BossBar.Style.PROGRESS)).setDarkenSky(true);

        this.lookControl = new MinibossLookControl(this);
    }
    public RegistryEntry<Spell> getBeamSpell(){
        return this.school == SpellSchools.FROST ?  SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "beam_cold")).get() : SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "beam")).get();

    }
    public RegistryEntry<Spell> getMeteorSpell(){
        return this.school == SpellSchools.FROST ?  SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "comet")).get() : SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "starfall")).get();

    }
    public RegistryEntry<Spell> getCloudSpell(){
        return this.school == SpellSchools.FROST ?  SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "frost_cloud")).get() : SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "flame_cloud")).get();

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
    }
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }
    public static final TrackedData<Boolean> IS_CLONES ;

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);

        builder.add(IS_CLONES, false);
    }

    static{
        IS_CLONES = DataTracker.registerData(GeminiEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

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

    public boolean acting = false;
    public int failSafe;
    protected boolean teleportRandomly() {
        if (!this.getWorld().isClient() && this.isAlive() && this.getTarget() != null) {
            double d = this.getTarget().getX() + (this.random.nextDouble() - 0.5) * 16;
            double e = this.getTarget().getY() + (double)(this.random.nextInt(16) - 8);
            double f = this.getTarget().getZ() + (this.random.nextDouble() - 0.5) * 16;
            return this.teleportTo(d, e, f);
        } else {
            return false;
        }
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
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

        while(mutable.getY() > this.getWorld().getBottomY() && !this.getWorld().getBlockState(mutable).blocksMovement()) {
            mutable.move(Direction.DOWN);
        }
        BlockState blockState = this.getWorld().getBlockState(mutable);
        boolean bl = blockState.blocksMovement();
        boolean bl2 = blockState.getFluidState().isIn(FluidTags.WATER);
        if (bl && !bl2 && this.getTarget() != null &&this.getWorld().isSkyVisible(mutable.up().up()) &&   canSee(mutable.up().toCenterPos(),this.getTarget()) && Math.abs(mutable.up().getY() - y) <= 2) {
            Vec3d vec3d = this.getPos();
            boolean bl3 = this.teleport(x, y, z, true);
            if (bl3) {
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
        if(this.getPartner() == null &&  this.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA.entityType) &&  !this.getWorld().isClient() && !this.spawned){
            if(this.getWorld() instanceof ServerWorld serverWorld){
                serverWorld.iterateEntities().forEach(entity -> {
                    if(entity.distanceTo(this) < 32 && entity.getType().equals(RPGMinibossesEntities.GEMINI_BETA.entityType)){
                        if(((GeminiEntity)entity).getPartner() == null) {
                            ((GeminiEntity) entity).setPartner(this);
                            this.setPartner((GeminiEntity) entity);
                            this.phaseTimer = 0;
                            ((GeminiEntity) entity).phaseTimer = 0;
                            ((GeminiEntity) entity).setPhase(this.phase == Phase.PRIMARY ? Phase.SECONDARY : Phase.PRIMARY);
                        }
                    }
                });
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
    }

    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }
    @Override
    protected void mobTick() {

        super.mobTick();


        if(this.getWorld() instanceof ServerWorld serverWorld && this.getPartner() != null&& this.getType().equals(RPGMinibossesEntities.GEMINI_ALPHA.entityType) && phaseTimer > phaseTime){
            this.setPhase(this.phase == Phase.PRIMARY ? Phase.SECONDARY : Phase.PRIMARY);
            this.getPartner().setPhase(this.phase == Phase.PRIMARY ? Phase.SECONDARY : Phase.PRIMARY);
            this.phaseTimer = 0;

        }
        if(this.getPartner() == null || this.getPartner().isDead() || this.getPartner().isRemoved()){
            this.setPhase(Phase.PRIMARY);
        }
        if(this.getWorld() instanceof ServerWorld serverWorld  && this.getPartner() != null && this.getPartner().getTarget() == null && this.getTarget() != null){
            this.getPartner().setTarget(this.getTarget());
        }
        if(this.phase.equals(Phase.PRIMARY)) {
            if (this.getTarget() != null && !acting && clonesTimer <= 0) {
                acting = true;
                this.triggerAnim("beam_large", "beam_large");
                RegistryEntry<Spell> spell = this.getBeamSpell();
                for (int i = 0; i < 8; i++) {
                    boolean bool = teleportRandomly();
                    teleportTimer = (int) (this.teleportCooldown * (bool ? 1 : 0.5));
                    if (bool) break;
                }
                ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);
                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(28, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(36, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                this.getDataTracker().set(IS_CLONES, true);

                ((WorldScheduler) this.getWorld()).schedule(60, () -> {
                    acting = false;
                    this.getDataTracker().set(IS_CLONES, false);

                });
                clonesTimer = (int) (clonesCooldown + this.getRandom().nextGaussian() * 20);
                failSafe = 0;
            }
            if (this.getTarget() != null && !acting && strongAttackTimer <= 0) {
                acting = true;
                RegistryEntry<Spell> spell = this.getBeamSpell();;

                ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(28, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(36, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                this.triggerAnim("beam_large", "beam_large");
                failSafe = 0;
                ((WorldScheduler) this.getWorld()).schedule(60, () -> {
                    acting = false;
                });
                strongAttackTimer = (int) (strongAttackCooldown + this.getRandom().nextGaussian() * 20);
            }
            if (this.getTarget() != null && !acting && basicAttackTimer <= 0) {
                acting = true;
                this.triggerAnim("beam", "beam");
                failSafe = 0;
                RegistryEntry<Spell> spell = this.getBeamSpell();;

                ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(30, () -> {
                    acting = false;
                });
                basicAttackTimer = (int) (basicAttackCooldown );
            }
            if (this.getTarget() != null && this.getTarget() != null && this.teleportTimer <= 0) {
                for (int i = 0; i < 8; i++) {
                    boolean bool = teleportRandomly();
                    teleportTimer = (int) (this.teleportCooldown * (bool ? 1 : 0.5));
                    if (bool) break;
                }
            }
        }
        else{
            if(this.getTarget() != null && (this.distanceTo(this.getTarget()) > 32 || this.getY() - this.getTarget().getY() < 4)){
                Vec3d pos = this.getTarget().getPos().add(0,8,0);
                this.requestTeleport(pos.getX(),pos.getY(),pos.getZ() );
                if(this.getWorld().getBlockState(BlockPos.ofFloored(this.getPos())).blocksMovement()){
                    if(this.teleportRandomly(this,12)){
                        pos = this.getPos().add(0,8,0);
                        this.requestTeleport(pos.getX(),pos.getY(),pos.getZ());
                    }

                }
            }

            if (this.getTarget() != null && !acting && cloudTimer <= 0) {
                acting = true;
                this.triggerAnim("meteor_channel", "meteor_channel");
                failSafe = 0;
                RegistryEntry<Spell> spell = this.getCloudSpell();;

                ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                    if(this.getTarget() != null) {
                        SpellHelper.placeCloud(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }
                });
                ((WorldScheduler) this.getWorld()).schedule(60, () -> {
                    if(this.getTarget() != null) {
                        SpellHelper.placeCloud(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }                });
                ((WorldScheduler) this.getWorld()).schedule(80, () -> {

                    if(this.getTarget() != null) {
                        SpellHelper.placeCloud(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }
                });
                ((WorldScheduler) this.getWorld()).schedule(100, () -> {

                    if(this.getTarget() != null) {
                        SpellHelper.placeCloud(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }
                });
                ((WorldScheduler) this.getWorld()).schedule(120, () -> {
                    acting = false;
                });
                cloudTimer = (int) (cloudCooldown + this.getRandom().nextGaussian() * 20 );
            }
            if (this.getTarget() != null && !acting && meteorTimer <= 0) {
                acting = true;
                this.triggerAnim("meteor_channel", "meteor_channel");
                failSafe = 0;
                RegistryEntry<Spell> spell = this.getMeteorSpell();;

                ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                    if(this.getTarget() != null) {
                        SpellHelper.fallProjectile(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }
                });
                ((WorldScheduler) this.getWorld()).schedule(60, () -> {
                    if(this.getTarget() != null) {
                        SpellHelper.fallProjectile(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }                });
                ((WorldScheduler) this.getWorld()).schedule(80, () -> {

                    if(this.getTarget() != null) {
                        SpellHelper.fallProjectile(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }
                });
                ((WorldScheduler) this.getWorld()).schedule(100, () -> {

                    if(this.getTarget() != null) {
                        SpellHelper.fallProjectile(this.getWorld(), this, this.getTarget(), this.getTarget().getPos(), spell, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(this.school,this)).position(this.getTarget().getPos()));
                        ParticleHelper.sendBatches(this, spell.value().release.particles);
                    }
                });
                ((WorldScheduler) this.getWorld()).schedule(120, () -> {
                    acting = false;
                });
                meteorTimer = (int) (meteorCooldown + this.getRandom().nextGaussian() * 20 );
            }
            if (this.getTarget() != null && !acting && strongAttackTimer <= 0) {
                acting = true;
                RegistryEntry<Spell> spell = this.getBeamSpell();;

                ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(28, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(36, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                this.triggerAnim("beam_large", "beam_large");
                failSafe = 0;
                ((WorldScheduler) this.getWorld()).schedule(60, () -> {
                    acting = false;
                });
                strongAttackTimer = (int) (strongAttackCooldown + this.getRandom().nextGaussian() * 20);
            }

            if (this.getTarget() != null && !acting && basicAttackTimer <= 0) {
                acting = true;
                this.triggerAnim("beam", "beam");
                failSafe = 0;
                RegistryEntry<Spell> spell = this.getBeamSpell();;

                ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                    List<Entity> entityList = TargetHelper.targetsFromArea(this, this.getEyePos(), spell.value().range, spell.value().target.area, entity -> entity != this.getPartner());
                    for (Entity entity : entityList) {
                        boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spell, spell.value().impacts, new SpellHelper.ImpactContext().position(this.getPos()));
                    }
                    SoundHelper.playSound(this.getWorld(),this,spell.value().release.sound);

                    ParticleHelper.sendBatches(this, spell.value().release.particles);
                });
                ((WorldScheduler) this.getWorld()).schedule(30, () -> {
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

    public void setPartner(GeminiEntity partner) {
        this.partner = partner;
        this.partnerId = partner.getUuid();
    }
    @Nullable
    public  GeminiEntity getPartner() {
        return  this.getWorld().isClient() ? null : partnerId == null ? null : (GeminiEntity)((ServerWorld)this.getWorld()).getEntity(partnerId);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GeminiEntity>(this, "idle",
                0, this::predicate2)
        );
        controllers.add(
                new AnimationController<>(this, "meteor_channel", event -> PlayState.CONTINUE)
                        .triggerableAnim("meteor_channel", METEOR_CHANNEL));
        controllers.add(
                new AnimationController<>(this, "meteor_charge", event -> PlayState.CONTINUE)
                        .triggerableAnim("meteor_charge", METEOR_CHARGE));
        controllers.add(
                new AnimationController<>(this, "beam", event -> PlayState.CONTINUE)
                        .triggerableAnim("beam", BEAM));
        controllers.add(
                new AnimationController<>(this, "beam_large", event -> PlayState.CONTINUE)
                        .triggerableAnim("beam_large", BEAM_LARGE));
    }
    public AnimatableInstanceCache instanceCache = AzureLibUtil.createInstanceCache(this);

    private PlayState predicate2(AnimationState<GeminiEntity> state) {

        return state.setAndContinue(IDLE);

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
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return instanceCache;
    }
}
