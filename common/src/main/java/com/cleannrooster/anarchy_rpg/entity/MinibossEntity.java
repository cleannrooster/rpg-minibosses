package com.cleannrooster.anarchy_rpg.entity;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import java.util.Objects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.Tameable;
import com.cleannrooster.anarchy_rpg.magic.fx.FxEmitter;
import dev.eann.atlasvfx.cue.FxCues;
import com.cleannrooster.anarchy_rpg.magic.fx.CueArgs;
import com.cleannrooster.anarchy_rpg.magic.Relations;
import com.cleannrooster.anarchy_rpg.magic.MagicSchool;
import com.cleannrooster.anarchy_rpg.magic.MagicCaster;
import com.cleannrooster.anarchy_rpg.client.entity.effect.Effects;
import com.cleannrooster.anarchy_rpg.client.entity.renderer.MinibossAnimationProvider;
import com.cleannrooster.anarchy_rpg.entity.AI.*;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import mod.azure.azurelib.common.util.MoveAnalysis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.provider.TradeRebalanceEnchantmentProviders;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.task.OpenDoorsTask;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.*;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.village.*;
import net.minecraft.world.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static java.lang.Math.max;
import static net.minecraft.entity.mob.HostileEntity.canSpawnIgnoreLightLevel;
import static net.minecraft.entity.mob.HostileEntity.isSpawnDark;

public class MinibossEntity extends PathAwareEntity implements MagicCaster, Tameable,  Angerable,
        com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackAim.Aiming {
    private UUID ownerUuid;
    public boolean performing;

    /** Brain that governs state-machine AI. Null for mobs that haven't migrated yet. */
    @Nullable
    public com.cleannrooster.anarchy_rpg.entity.brain.MobBrain brain;

    protected MinibossEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.lookControl = new MinibossLookControl(this);

        this.setPathfindingPenalty(PathNodeType.DOOR_WOOD_CLOSED,0F);
        this.dispatcher = new MinibossAnimationProvider.MinibossAnimationDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
    }


    public static boolean canSpawnIgnoreLightLevel(EntityType<? extends PathAwareEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getDifficulty() != Difficulty.PEACEFUL && canMobSpawn(type, world, spawnReason, pos, random);
    }

    protected MinibossEntity(EntityType<? extends PathAwareEntity> entityType, World world, float spawnCoeff) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.spawnCoeff = spawnCoeff;

        this.lookControl = new MinibossLookControl(this);
        this.setPathfindingPenalty(PathNodeType.DOOR_WOOD_CLOSED,0F);
        this.dispatcher = new MinibossAnimationProvider.MinibossAnimationDispatcher(this);
        this.moveAnalysis = new MoveAnalysis(this);
    }
    public List<String> NAMES = List.of(
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.1",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.2",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.3",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.4");
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PETRIFIED, false);
        builder.add(HAS_ROLLED, false);
        builder.add(NAME, -1);
        builder.add(DOWN, false);
        builder.add(INDICATOR, 40);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(CANTHIRE, false);
        builder.add(FACTION_FLAG, com.cleannrooster.anarchy_rpg.faction.PackedFlag.NONE);
        builder.add(COMPANION_DOWNED, false);
        builder.add(FORSAKEN, false);

    }


    @Override
    public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        if(spawnReason.equals(SpawnReason.TRIAL_SPAWNER)){
            this.setCantHire(true);
        }
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    List<Item> bonusList = List.of();

    /** The weapons this adventurer can be equipped with; also what its Exchange stock draws weapons from. */
    public List<Item> getWeaponPool() {
        return this.bonusList;
    }

    @Override
    public Text getName() {
        Text name = this.getDataTracker().get(NAME) != -1
                ? Text.translatable(this.NAMES.get(this.getDataTracker().get(NAME))) : super.getName();
        return this.isForsaken()
                ? Text.translatable("entity." + com.cleannrooster.anarchy_rpg.RPGMinibosses.CONTENT_NAMESPACE + ".forsaken", name)
                : name;
    }
    @Environment(value = EnvType.CLIENT)
    public static void setRotationFromVelocity(Entity entity) {
        Vec3d vel = entity.getVelocity();
        if (vel.horizontalLengthSquared() > 1.0E-7 && entity instanceof PathAwareEntity pathAwareEntity) {
            // Movement direction in Minecraft yaw convention
            float movDirYaw = (float)(MathHelper.atan2(vel.z, vel.x) * 57.2957763671875) - 90.0F;
            // If velocity is backward relative to entity facing, flip body 180° so it faces origin of movement
            float diff = MathHelper.subtractAngles(movDirYaw, pathAwareEntity.getYaw());
            float targetBodyYaw = Math.abs(diff) > 90.0F ? movDirYaw + 180.0F : movDirYaw;

            while (targetBodyYaw - pathAwareEntity.prevBodyYaw < -180.0F) {
                pathAwareEntity.prevBodyYaw -= 360.0F;
            }
            while (targetBodyYaw - pathAwareEntity.prevBodyYaw >= 180.0F) {
                pathAwareEntity.prevBodyYaw += 360.0F;
            }

            pathAwareEntity.bodyYaw = MathHelper.lerp(0.2F, pathAwareEntity.prevBodyYaw, targetBodyYaw);
            pathAwareEntity.prevBodyYaw = pathAwareEntity.bodyYaw;
        }
    }
    @Environment(value = EnvType.CLIENT)
    public static void setRotationAndHeadFromVelocity(Entity entity) {
        Vec3d vec3d = entity.getVelocity();
        if (vec3d.lengthSquared() != 0.0 && entity instanceof PathAwareEntity pathAwareEntity) {

            vec3d = pathAwareEntity.getVelocity();
            double d = vec3d.horizontalLength();
            float yaw = 180+(float)(MathHelper.atan2(vec3d.z, vec3d.x) * 57.2957763671875) + 90.0F;



            while(yaw -  ((PathAwareEntity) entity).prevBodyYaw < -180.0F) {
                ((PathAwareEntity) entity).prevBodyYaw -= 360.0F;
            }

            while(yaw -  ((PathAwareEntity) entity).prevBodyYaw >= 180.0F) {
                ((PathAwareEntity) entity).prevBodyYaw += 360.0F;
            }
            ((PathAwareEntity) entity).headYaw = (MathHelper.lerp(0.2F, (((PathAwareEntity) entity).prevBodyYaw), yaw));
           // ((PathAwareEntity) entity).prevHeadYaw = ((PathAwareEntity) entity).headYaw;

            ((PathAwareEntity) entity).bodyYaw = (MathHelper.lerp(0.2F, (((PathAwareEntity) entity).prevBodyYaw), yaw));
           // ((PathAwareEntity) entity).prevBodyYaw = ((PathAwareEntity) entity).bodyYaw;
        }
    }
    public final MinibossAnimationProvider.MinibossAnimationDispatcher dispatcher;

    public final MoveAnalysis moveAnalysis;


    private static final TrackedData<Boolean> HAS_ROLLED ;
    public static final TrackedData<Boolean> DOWN;

    public static final TrackedData<Integer> NAME ;

    public static final TrackedData<Integer> INDICATOR ;
    public static final TrackedData<Optional<UUID>> OWNER_UUID ;
    public static final TrackedData<Boolean> CANTHIRE ;
    /** This adventurer's faction flag, packed ({@link com.cleannrooster.anarchy_rpg.faction.PackedFlag}), for drawing it above them. */
    public static final TrackedData<Integer> FACTION_FLAG;
    /** A companion knocked down by a lethal blow. Separate from {@link #DOWN}, which means downed and recruitable. */
    public static final TrackedData<Boolean> COMPANION_DOWNED;
    /** Mirrors the intro (petrified) effect to clients, which vanilla does not send it to. */
    public static final TrackedData<Boolean> PETRIFIED;
    /** A Forsaken Adventurer: answers to no guild and attacks on sight. Synced so clients can draw its aura. */
    public static final TrackedData<Boolean> FORSAKEN;

    static{
        HAS_ROLLED = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        NAME = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);
        DOWN = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        INDICATOR = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);
        OWNER_UUID = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        CANTHIRE = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        FACTION_FLAG = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);
        COMPANION_DOWNED = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        PETRIFIED = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        FORSAKEN = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    }

  /*  public static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.unknown.walk");
    public static final RawAnimation WALK_B = RawAnimation.begin().thenLoop("animation.unknown.walk_backwards");
    public static final RawAnimation WALK_B_2h = RawAnimation.begin().thenLoop("animation.unknown.walk_backwards2_2h");
    public static final RawAnimation WALK_B_T = RawAnimation.begin().thenLoop("animation.unknown.walk_backwards_transition").thenPlay("animation.unknown.walk_backwards");
    public static final RawAnimation WALK_B_2h_T = RawAnimation.begin().thenPlay("animation.unknown.walk_backwards2_2h2_transition").thenLoop("animation.unknown.walk_backwards2_2h");

    public static final RawAnimation WALKING_BACKWARDS = RawAnimation.begin().thenLoop("walking_backwards");
    public static final RawAnimation WALK_NO_AGGRO = RawAnimation.begin().thenLoop("walking");
    public static final RawAnimation SPRINT = RawAnimation.begin().thenLoop("running");

    public static final RawAnimation WALK2H = RawAnimation.begin().thenLoop("animation.unknown.walk_2h");
    public static final RawAnimation IDLE_AGGRO = RawAnimation.begin().thenPlay("animation.unknown.idle");

    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    public static final RawAnimation IDLE2H = RawAnimation.begin().thenPlay("animation.unknown.idle_2h");
    public static final RawAnimation DOWNANIM = RawAnimation.begin().thenPlayAndHold("animation.generic.down");
    public static final RawAnimation SWING1 = RawAnimation.begin().then("animation.mob.swing1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation PREPARE = RawAnimation.begin().then("animation.mob.prepare", Animation.LoopType.PLAY_ONCE);
*/
    public float spawnCoeff = 1;
   // public AnimatableInstanceCache instanceCache = AzureLibUtil.createInstanceCache(this);
 /*   @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {

        animationData.add(new AnimationController<MinibossEntity>(this, "walk",
                1, this::predicate2)
        );


    }*/
    // ── Anticipation indicator (deprecated) ───────────────────────────────────
    //
    // The old attack warning — a red flash plus the anticipation sting fired at the moment an ability
    // committed — is retired. It predates the phase model: every attack now telegraphs through its own
    // windup animation, which is longer, more specific and readable from the mob's silhouette rather than
    // from a colour. Keeping both meant the warning fired on the same frame the windup started, which read
    // as a stutter in front of every heavy attack.
    //
    // These methods are kept as no-ops rather than deleted so any external caller still links, and the
    // INDICATOR tracked value stays registered so the data-tracker layout is unchanged for saves and for
    // clients on the same build. MagusPrimeEntity has its own independent copy of this system and is
    // untouched.

    /** @deprecated the attack warning system is retired; always returns 0. */
    @Deprecated
    public int getIndicator(){
        return 0;
    }

    /** @deprecated the attack warning system is retired; does nothing. */
    @Deprecated
    public void playBoom(){
    }

    /** @deprecated the attack warning system is retired; does nothing. Telegraphs live in attack windups. */
    @Deprecated
    public void resetIndicator(){
    }

    /** @deprecated the attack warning system is retired; does nothing. */
    @Deprecated
    public void tickIndicator(){
    }
    public static ArrayList<Item> itemList = new ArrayList<>();

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);

    }
    public void generateAndEquipLoot(EquipmentSlot slot, int attempts){
        for(int i = 0; i < attempts; i++) {
            RegistryKey<LootTable> registryKey = EntityType.ENDER_DRAGON.getLootTableId();
            LootTable lootTable = this.getWorld().getServer().getReloadableRegistries().getLootTable(registryKey);
            LootContextParameterSet.Builder builder = (new LootContextParameterSet.Builder((ServerWorld) this.getWorld())).add(LootContextParameters.THIS_ENTITY, this).add(LootContextParameters.ORIGIN, this.getPos()).add(LootContextParameters.DAMAGE_SOURCE, this.getDamageSources().generic());


            LootContextParameterSet lootContextParameterSet = builder.build(LootContextTypes.ENTITY);
            lootTable.generateLoot(lootContextParameterSet, this.getLootTableSeed(), (itemStack -> checkAndEquipLoot(itemStack, slot)));
            if(this.hasStackEquipped(slot)){
                break;
            };

        }
        for(int i = 0; i < attempts; i++) {
            RegistryKey<LootTable> registryKey = this.getLootTableId();
            LootTable lootTable = this.getWorld().getServer().getReloadableRegistries().getLootTable(registryKey);
            LootContextParameterSet.Builder builder = (new LootContextParameterSet.Builder((ServerWorld) this.getWorld())).add(LootContextParameters.THIS_ENTITY, this).add(LootContextParameters.ORIGIN, this.getPos()).add(LootContextParameters.DAMAGE_SOURCE, this.getDamageSources().generic());


            LootContextParameterSet lootContextParameterSet = builder.build(LootContextTypes.ENTITY);
            lootTable.generateLoot(lootContextParameterSet, this.getLootTableSeed(), (itemStack -> checkAndEquipLoot(itemStack, slot)));
            if(this.hasStackEquipped(slot)){
                break;
            };

        }
    }

    public void checkAndEquipLoot(ItemStack stack,EquipmentSlot slot){
        if(this.bonusList.contains(stack.getItem())){

            if(!this.hasStackEquipped(slot)){
                this.equipStack(slot,stack);
            }
        }
    }



    public ItemStack getMainWeapon(){
        return ItemStack.EMPTY;

    }
    public ItemStack getBackWeapon(){
        return ItemStack.EMPTY;

    }
    public double getCooldownCoeff(){
        double g = 1;
        if(!this.getWorld().isClient() && this.getTarget() != null){
            for(MinibossEntity entity : ((ServerWorld)this.getWorld()).getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class),minibossEntity ->
                    minibossEntity != this &&
                    minibossEntity.distanceTo(this) < 32)){
                g++;
            }
        }
        return Math.max(0.1,Math.max(1,Math.pow(g,0.58496250072D))+0.5*this.getRandom().nextGaussian());
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (com.cleannrooster.anarchy_rpg.companion.Companions.onLethalDamage(this, damageSource)) {
            return;
        }
        // Killed by an adventurer of a faction at war with this one's: a real death, not a recruitable downing.
        boolean killedInWar = damageSource.getAttacker() instanceof MinibossEntity killer
                && com.cleannrooster.anarchy_rpg.faction.Factions.atWar(killer, this);
        if(!this.getCantHire() && !killedInWar) {
            if (!damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !this.getDataTracker().get(DOWN) && this.getWorld() instanceof ServerWorld && this.getOwnerUuid() == null) {
                this.setHealth(0.01F);
                //(this).triggerAnim("down", "down");

                this.getDataTracker().set(DOWN, true);
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 9, false, false));
                return;
            }
        }

        if( damageSource.getAttacker() instanceof ServerPlayerEntity player){
            com.cleannrooster.anarchy_rpg.faction.Factions.recordDeed(this, player,
                    com.cleannrooster.anarchy_rpg.faction.StandingCause.KILLED_MEMBER, RPGMinibossesEntities.config.standingKillMember);
        }
        // An encounter boss that truly dies is no longer anything to rumor about.
        if (this.fromStructure && this.getWorld() instanceof ServerWorld serverWorld) {
            com.cleannrooster.anarchy_rpg.rumor.EncounterLedger.get(serverWorld.getServer()).remove(this.getUuid());
        }
        super.onDeath(damageSource);
    }


    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if(this.isIncapacitated()) {
            return;
        }
        super.move(movementType, movement);
    }
    @Override
    public boolean canSpawn(WorldView world) {
        if(this.getWorld().getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
            if(this.getWorld().isSkyVisible(this.getBlockPos().up()) || this.getY() > 80){
                return false;
            }
        }
        if(RPGMinibossesEntities.config.enableAdvancementRequirement) {
            if (this.getWorld().getPlayers().stream().anyMatch(player -> {
                var satisfied = false;
                var condition = false;
                for (String string : RPGMinibossesEntities.config.advancements.keySet()) {
                    if (this.getWorld().getServer().getAdvancementLoader().get(Identifier.tryParse(string)) != null && ((ServerPlayerEntity) player).getAdvancementTracker().getProgress(this.getWorld().getServer().getAdvancementLoader().get(Identifier.tryParse(string))) != null && RPGMinibossesEntities.config.distance > player.distanceTo(this)) {
                        satisfied = (RPGMinibossesEntities.config.advancements.get(string).equals(true) && ((ServerPlayerEntity) player).getAdvancementTracker().getProgress(this.getWorld().getServer().getAdvancementLoader().get(Identifier.tryParse(string))).isDone())
                                || (RPGMinibossesEntities.config.advancements.get(string).equals(false) && !((ServerPlayerEntity) player).getAdvancementTracker().getProgress(this.getWorld().getServer().getAdvancementLoader().get(Identifier.tryParse(string))).isDone());
                        if (!satisfied && RPGMinibossesEntities.config.allRequired) {
                            condition = false;
                            break;
                        }
                        else if (satisfied){
                            condition = true;
                        }
                    }
                }
                if(RPGMinibossesEntities.config.debug) {
                    System.out.println(satisfied);
                    System.out.println(condition);
                }

                return condition;
            })) {
                return super.canSpawn(world);
            } else {
                return false;
            }
        }
        else{
            return super.canSpawn(world);
        }
    }

    @Override
    public void tick() {
        // Companion timers run here, not in mobTick: a downed companion has its AI off, so mobTick stops,
        // and it still has to notice when it is time to get back up.
        if (!this.getWorld().isClient()) {
            com.cleannrooster.anarchy_rpg.companion.Companions.tick(this);
            com.cleannrooster.anarchy_rpg.rumor.Rumors.tick(this);
            com.cleannrooster.anarchy_rpg.service.CampGuard.tick(this);
        }
        // Teach this client the shapes of this mob's attacks. Done on tick rather than in the
        // constructor because the hook is overridden per mob, and a base constructor calling it would
        // build the brain before the subclass's own fields exist.
        if (this.getWorld().isClient()) {
            com.cleannrooster.anarchy_rpg.entity.combat.AttackRegistry.ensureClientPrototypes(this);
        } else {
            this.dataTracker.set(PETRIFIED, this.hasStatusEffect(this.getIntroEffect()));
        }
        if(this.getTarget() != null){
            if(this.getTarget() instanceof MinibossEntity entity && entity.isIncapacitated() && this.isTamed()){
                this.setTarget(null);
            }
        }
        if(this.firstUpdate && !this.getWorld().isClient()){
            if(this.getDataTracker().get(DOWN)){
                //(this).triggerAnim("down","down");

            }
            if(this.getDataTracker().get(NAME) == -1){
                if(RPGMinibossesEntities.config.petrifyChance > this.getRandom().nextFloat()) {
                    this.applyIntroEffect();
                }
                if(!this.hasCustomName()) {
                    this.getDataTracker().set(NAME, this.getRandom().nextInt(4));
                }
            }
        }

        if(this.age % 20 == 0 && this.getRandom().nextInt(19) == 0 && this.getWorld() instanceof ServerWorld serverWorld && serverWorld.getPlayers().stream().anyMatch(player -> this.distanceTo(player) < 6 && this.canSee(player))){
            for(MinibossEntity boss : this.getWorld().getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class),this.getBoundingBox().expand(8), Predicates.alwaysTrue())) {
                if (!boss.notPetrified()) {
                    boss.removeStatusEffect(this.getIntroEffect());
                    boss.playReleaseParticlesAndSound();
                }

            }
            if (!this.notPetrified()) {
                this.removeStatusEffect(this.getIntroEffect());
                playReleaseParticlesAndSound();
            }
        }
        if(!this.getDataTracker().get(HAS_ROLLED) && this.getServer() != null) {
            if(!skipMainHand()) {
                this.generateAndEquipLoot(EquipmentSlot.MAINHAND, 8);
            }
            if(!skipOffHand()) {
                this.generateAndEquipLoot(EquipmentSlot.OFFHAND, 8);
            }
            if(getMainHandStack().isEmpty()){
                this.equipStack(EquipmentSlot.MAINHAND,this.getMainWeapon());
            }
            this.getDataTracker().set(HAS_ROLLED,true);
        }
        // Out of combat the body still snaps to its travel direction, which is what a wandering mob should
        // do. In combat that behaviour is exactly what made a strafing miniboss look like it was running
        // forward sideways, so there the body is held on the server's synced facing instead and the
        // directional locomotion clips carry the movement.
        if (this.getWorld().isClient() && !this.isAttacking()) {
            setRotationFromVelocity(this);
        }
        super.tick();
        moveAnalysis.update();
        animTick();
        if (this.getWorld().isClient() && !this.isAttacking()) {
            setRotationFromVelocity(this);
        }

        if (this.getWorld().isClient()) {
            tickHeadRotation();
        }
    }

    /**
     * While combat steering or an attack owns the body, the facing the server chose <em>is</em> the
     * facing — vanilla's body control would otherwise drag it back toward the travel direction every
     * tick, and a mob that turns to face wherever it is sliding cannot read as deliberate.
     */
    @Override
    protected float turnHead(float bodyRotation, float headRotation) {
        if (!this.getWorld().isClient()
                && this.brain != null
                && this.brain.locomotion.owner()
                != com.cleannrooster.anarchy_rpg.entity.combat.MovementOwner.NAVIGATION) {
            this.bodyYaw = this.getYaw();
            return headRotation;
        }
        return super.turnHead(bodyRotation, headRotation);
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
        super.takeKnockback(strength, x, z);
        // Steering yields for a few ticks so a shove actually lands instead of being overwritten.
        if (strength > 0.0 && this.brain != null) {
            this.brain.onKnockback();
        }
    }

    @Environment(value = EnvType.CLIENT)
    private void tickHeadRotation() {
        MinibossLookControl lc = (MinibossLookControl) this.lookControl;
        if (lc.lookAtTimer > 0) {
            double dx = lc.x - this.getX();
            double dz = lc.z - this.getZ();
            if (Math.abs(dx) > 1.0E-7 || Math.abs(dz) > 1.0E-7) {
                float targetYaw = (float)(MathHelper.atan2(dz, dx) * 57.2957763671875) - 90.0F;
                float sub = MathHelper.subtractAngles(this.headYaw, targetYaw);
                this.headYaw = this.headYaw + MathHelper.clamp(sub, -lc.maxYawChange, lc.maxYawChange);
            }
            double dy = lc.y - this.getEyeY();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (Math.abs(dy) > 1.0E-7 || horiz > 1.0E-7) {
                float targetPitch = (float)(-(MathHelper.atan2(dy, horiz) * 57.2957763671875));
                float sub = MathHelper.subtractAngles(this.getPitch(), targetPitch);
                this.setPitch(this.getPitch() + MathHelper.clamp(sub, -lc.maxPitchChange, lc.maxPitchChange));
            }
        } else {
            float headTarget = this.getNavigation().isIdle() ? this.bodyYaw : this.getYaw();
            float sub = MathHelper.subtractAngles(this.headYaw, headTarget);
            this.headYaw = this.headYaw + MathHelper.clamp(sub, -10.0F, 10.0F);
        }
        this.headYaw = MathHelper.clampAngle(this.headYaw, this.bodyYaw, 35.0F);
        this.prevHeadYaw = this.headYaw;
    }

    @Nullable
    @Override
    public LivingEntity getTarget() {
        if(this.getWorld().getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
            return null;
        }
        if(super.getTarget() != null){
            if(super.getTarget() instanceof MinibossEntity entity && entity.isIncapacitated() && this.isTamed()){
                this.setTarget(null);
                return null;
            }
            if( this.getOwnerUuid() != null &&   super.getTarget().getUuid().equals(this.getOwnerUuid())){
                this.setTarget(null);
                return null;
            }
            if( this.getOwnerUuid() != null &&   super.getTarget() instanceof Tameable tameable && tameable.getOwnerUuid() != null && tameable.getOwnerUuid().equals(this.getOwnerUuid())){
                this.setTarget(null);
                return null;
            }
            if(this.isTamed() && this.getOwner() instanceof PlayerEntity player1 && super.getTarget() instanceof Tameable tameable && tameable.getOwner() instanceof PlayerEntity player2 && !player1.shouldDamagePlayer(player2)){
                this.setTarget(null);
                return null;
            }

        }
        return super.getTarget();
    }



    public boolean hasPatrolLeader = false;

    /** This adventurer's party, and through it its faction. Server-side; assigned on first use. */
    @Nullable
    private com.cleannrooster.anarchy_rpg.faction.PartyMembership partyMembership;

    /**
     * This adventurer's party. Camp residents and patrols are given theirs when they spawn; any other
     * adventurer has none, and the first time anything asks it becomes Forsaken, in a party of one
     * ({@code forsakenOutsideCamps}). With that off, it joins a nearby party or starts its own
     * ({@link com.cleannrooster.anarchy_rpg.faction.Factions#defaultPartyFor}).
     */
    public com.cleannrooster.anarchy_rpg.faction.PartyMembership getPartyMembership() {
        if (this.partyMembership == null) {
            if (!this.getWorld().isClient() && RPGMinibossesEntities.config.forsakenOutsideCamps) {
                this.partyMembership = com.cleannrooster.anarchy_rpg.faction.PartyMembership.create(this.getRandom());
                this.dataTracker.set(FORSAKEN, true);
            } else {
                this.partyMembership = com.cleannrooster.anarchy_rpg.faction.Factions.defaultPartyFor(this);
            }
        }
        return this.partyMembership;
    }

    /**
     * Whether this is a Forsaken Adventurer: no guild, hostile to every player and at odds with every guild.
     * One recruited as a companion stops being Forsaken for good ({@link #redeem}).
     */
    public boolean isForsaken() {
        return this.dataTracker.get(FORSAKEN) && this.getOwnerUuid() == null;
    }

    /** No longer Forsaken: recruited, it answers to its player now. */
    public void redeem() {
        this.dataTracker.set(FORSAKEN, false);
    }

    /** Keeps the tracked flag in step with this adventurer's faction, so clients can draw it. */
    private void syncFactionFlag(ServerWorld world) {
        var slot = com.cleannrooster.anarchy_rpg.faction.Factions.factionOf(this);
        int packed = slot == null ? com.cleannrooster.anarchy_rpg.faction.PackedFlag.NONE
                : com.cleannrooster.anarchy_rpg.faction.PackedFlag.pack(
                        com.cleannrooster.anarchy_rpg.faction.FactionState.roster(world.getServer()).get(slot).flag(), world.getRegistryManager());
        if (this.dataTracker.get(FACTION_FLAG) != packed) {
            this.dataTracker.set(FACTION_FLAG, packed);
        }
    }

    public int getPackedFactionFlag() {
        return this.dataTracker.get(FACTION_FLAG);
    }

    public boolean hasPartyMembership() {
        return this.partyMembership != null;
    }

    public void setPartyMembership(com.cleannrooster.anarchy_rpg.faction.PartyMembership partyMembership) {
        this.partyMembership = partyMembership;
        this.dataTracker.set(FORSAKEN, false);
    }

    /** The player this adventurer is talking to, if any. Server-side and not saved: a reload ends it. */
    @Nullable
    private UUID conversationPartner;

    public @Nullable UUID getConversationPartner() {
        return this.conversationPartner;
    }

    public void setConversationPartner(@Nullable UUID partner) {
        this.conversationPartner = partner;
    }

    public boolean isDowned() {
        return this.getDataTracker().get(DOWN);
    }

    /** Downed either way: recruitable ({@link #DOWN}) or a companion waiting to get up. Out of the fight. */
    public boolean isIncapacitated() {
        return this.getDataTracker().get(DOWN) || this.getDataTracker().get(COMPANION_DOWNED);
    }

    public void setCompanionDowned(boolean downed) {
        this.getDataTracker().set(COMPANION_DOWNED, downed);
    }

    public boolean getDataTrackerCompanionDowned() {
        return this.getDataTracker().get(COMPANION_DOWNED);
    }

    /** Healing a downed companion gets it straight back up. */
    @Override
    public void heal(float amount) {
        if (amount > 0.0F && !this.getWorld().isClient() && this.getDataTracker().get(COMPANION_DOWNED)) {
            com.cleannrooster.anarchy_rpg.companion.Companions.onHealed(this);
        }
        super.heal(amount);
    }

    /** Which re-creation of a companion this entity is; a copy behind the registry's count removes itself. */
    private long companionIncarnation;

    public long getCompanionIncarnation() {
        return this.companionIncarnation;
    }

    public void setCompanionIncarnation(long incarnation) {
        this.companionIncarnation = incarnation;
    }

    /** A dismissed companion walking away for good. */
    private boolean departing;
    @Nullable
    private BlockPos departTarget;
    private int departUntilAge;

    public boolean isDeparting() {
        return this.departing;
    }

    public void startDeparting(BlockPos target, int maxTicks) {
        this.departing = true;
        this.departTarget = target;
        this.departUntilAge = this.age + maxTicks;
    }

    public @Nullable BlockPos getDepartTarget() {
        return this.departTarget;
    }

    public boolean departTimedOut() {
        return this.age >= this.departUntilAge;
    }

    /** What this adventurer has to trade. Server-side; generated on first use and saved. */
    private final com.cleannrooster.anarchy_rpg.exchange.ExchangeStock exchangeStock = new com.cleannrooster.anarchy_rpg.exchange.ExchangeStock();

    public com.cleannrooster.anarchy_rpg.exchange.ExchangeStock getExchangeStock() {
        return this.exchangeStock;
    }

    /** How many rumors this adventurer has left to tell. Server-side; saved. */
    private final com.cleannrooster.anarchy_rpg.rumor.RumorCharges rumorCharges = new com.cleannrooster.anarchy_rpg.rumor.RumorCharges();

    public com.cleannrooster.anarchy_rpg.rumor.RumorCharges getRumorCharges() {
        return this.rumorCharges;
    }

    /** Camp services bought from this adventurer today, per player. Server-side; saved. */
    private final com.cleannrooster.anarchy_rpg.service.CampServiceUses campServiceUses = new com.cleannrooster.anarchy_rpg.service.CampServiceUses();

    public com.cleannrooster.anarchy_rpg.service.CampServiceUses getCampServiceUses() {
        return this.campServiceUses;
    }

    /** World time a paid camp watch ends (Guard the camp); 0 for none. Saved. */
    private long guardUntil;

    public long getGuardUntil() {
        return this.guardUntil;
    }

    public void setGuardUntil(long guardUntil) {
        this.guardUntil = guardUntil;
    }

    @Override
    public void setAiDisabled(boolean aiDisabled) {
        super.setAiDisabled(aiDisabled);
    }

    /**
     * Set when this miniboss is placed by a worldgen encounter feature. Mirrors realmwalker's
     * structure-spawn flag: it forces {@link #isPersistent()} (so the boss never despawns or has
     * spawn-eligibility trouble after chunk reload) and is serialized in NBT so persistence survives
     * save/load rather than relying on a one-shot {@code setPersistent()} call at spawn time.
     */
    private boolean fromStructure;

    /** Flag this miniboss as structure-spawned, making it persistent for the encounter. */
    public void markFromStructure() {
        this.fromStructure = true;
    }

    public boolean isFromStructure() {
        return this.fromStructure;
    }

    @Override
    public boolean isPersistent() {
        return this.fromStructure || super.isPersistent();
    }

    @Override
    protected void mobTick() {

        ((MobNavigation)this.getNavigation()).setCanPathThroughDoors(true);
        ((MobNavigation)this.getNavigation()).setCanEnterOpenDoors(true);
        ((MobNavigation)this.getNavigation()).setCanWalkOverFences(true);
        aimAt(this.getTarget());
        super.mobTick();
        com.cleannrooster.anarchy_rpg.conversation.Conversations.tick(this);
        if (this.age % 20 == 0 && this.getWorld() instanceof ServerWorld serverWorld) {
            this.syncFactionFlag(serverWorld);
        }
        if (this.getCustomer() != null && !this.getWorld().isClient()) {
            this.getNavigation().stop();
            this.getLookControl().lookAt(this.getCustomer(), 30.0F, 30.0F);
        }

    }

    /**
     * Pitch committed attacks are thrown at, in degrees. Negative is up.
     *
     * <p>A field of its own rather than the entity's pitch, because vanilla's {@code LookControl}
     * resets pitch to zero every tick after custom AI runs — see
     * {@link com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackAim}. Server-side only: it
     * is read at the moment an attack commits and travels to clients inside the attack frame's pitch,
     * so there is nothing to sync.
     */
    private float attackPitch;

    @Override
    public float attackPitch() {
        return this.attackPitch;
    }

    /**
     * How fast the aim tracks. Brisk enough to follow a player up a step, slow enough that a jump does
     * not yank the aim skyward for the two ticks the player is airborne.
     */
    private static final float AIM_EASE = 0.35f;

    /** Ease the aim toward a target's elevation, curved and capped by {@code AttackAim}. */
    public void aimAt(@Nullable Entity target) {
        if (this.getWorld().isClient()) return;
        if (target == null) {
            this.attackPitch = com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackAim
                    .ease(this.attackPitch, 0.0f, AIM_EASE);
            return;
        }
        this.attackPitch = com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackAim.ease(
                this.attackPitch,
                com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackAim.pitchToward(this, target),
                AIM_EASE);
    }
    @Override
    public void tickMovement() {
        if(!notPetrified()){
            if(!this.isOnGround()) {
                super.tickMovement();

            }
            return;
        }

        super.tickMovement();

    }
    public boolean skipMainHand(){
        return false;
    }
    public boolean skipOffHand(){
        return false;
    }

    @Override
    protected float getDropChance(EquipmentSlot slot) {
        return 100F;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        if (this.getOwnerUuid() != null) {
            return;
        }
        super.dropEquipment(world, source, causedByPlayer);
    }
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if(hand.equals(Hand.MAIN_HAND)) {
            if (!this.notPetrified()) {
                for (MinibossEntity boss : this.getWorld().getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class), this.getBoundingBox().expand(8), Predicates.alwaysTrue())) {
                    if (!boss.notPetrified()) {
                        boss.playIntro(player);

                    }
                }
                playIntro(player);
                return ActionResult.SUCCESS_NO_ITEM_USED;

            }

            if (this.getDataTracker().get(DOWN) && !this.getCantHire()) {
                if (this.getOwnerUuid() == null) {
                    // Recruiting is the server's decision. The client only acknowledges the click: changing its
                    // own copy here (owner, DOWN, health) left a refused recruit looking recruited on screen,
                    // with nothing from the server to correct it.
                    if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                        return ActionResult.SUCCESS_NO_ITEM_USED;
                    }
                    if (com.cleannrooster.anarchy_rpg.companion.CompanionRegistry.get(serverPlayer.getServer()).ofOwner(serverPlayer.getUuid()) != null) {
                        serverPlayer.sendMessage(Text.translatable("companion." + RPGMinibosses.CONTENT_NAMESPACE + ".already_have"), true);
                        return ActionResult.SUCCESS_NO_ITEM_USED;
                    }
                    // The spare counts with the adventurer's faction while it still has one: once recruited it has none.
                    com.cleannrooster.anarchy_rpg.faction.Factions.recordDeed(this, serverPlayer,
                            com.cleannrooster.anarchy_rpg.faction.StandingCause.SPARED_MEMBER, RPGMinibossesEntities.config.standingSpareMember);
                    com.cleannrooster.anarchy_rpg.companion.Companions.tryRecruit(serverPlayer, this);
                    this.getDataTracker().set(DOWN, false);
                    this.heal(this.getMaxHealth());
                    return ActionResult.SUCCESS_NO_ITEM_USED;
                }
            }
            // Spawn eggs and name tags keep their vanilla use on adventurers.
            ItemStack held = player.getStackInHand(hand);
            if (this.isIncapacitated() || held.getItem() instanceof SpawnEggItem || held.isOf(Items.NAME_TAG)) {
                return ActionResult.PASS;
            }
            // Talking (including the owner's Follow and Stay orders) is decided on the server.
            if (this.getWorld().isClient()) {
                return ActionResult.SUCCESS;
            }
            return player instanceof ServerPlayerEntity serverPlayer && com.cleannrooster.anarchy_rpg.conversation.Conversations.tryOpen(this, serverPlayer)
                    ? ActionResult.CONSUME : ActionResult.PASS;
        }
        return ActionResult.PASS;
    }
    public String introTranslation(){
        return "text.anarchy_rpg.petrified";
    }
    /**
     * Whether this boss is free of its intro (petrified) effect. The server reads the effect itself; clients
     * read {@link #PETRIFIED}, which the server keeps in step every tick, since vanilla does not send mobs'
     * effects to clients.
     */
    public boolean notPetrified(){
        if (this.getWorld().isClient()) {
            return !this.dataTracker.get(PETRIFIED);
        }
        return !this.hasStatusEffect(this.getIntroEffect());
    }
    @Override
    public boolean isInvulnerable() {
        return  notPetrified() && super.isInvulnerable();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getDataTracker().get(COMPANION_DOWNED) && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        // A companion's owner can never hurt it: melee, projectiles, owner-lit TNT and spells all resolve to
        // the owner through getAttacker().
        if (this.getOwnerUuid() != null && source.getAttacker() != null && this.getOwnerUuid().equals(source.getAttacker().getUuid())) {
            return false;
        }
        if (!this.getWorld().isClient() && source.getAttacker() != null) {
            com.cleannrooster.anarchy_rpg.conversation.Conversations.onDamagedBy(this, source.getAttacker().getUuid());
        }
        if(amount < 1000000 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && this.getWorld().getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
            return false;
        }
        if(this.isIncapacitated() && source.getAttacker() instanceof MinibossEntity && !RPGMinibossesEntities.config.betrayal){
            return false;
        }
        // The friendly-fire reduction exists so a patrol does not blow itself apart with splash. It has no
        // business softening a fight the two of them actually picked, so a deliberate enemy takes the full
        // hit.
        if(source.getAttacker() instanceof MinibossEntity entity && !RPGMinibossesEntities.config.betrayal
                && entity.getOwnerUuid() == null && !entity.isEnemyOf(this)){
            amount *= RPGMinibossesEntities.config.friendlyFire;
        }
        if(source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)){
            return super.damage(source, amount);
        }
        if(source.isIn(DamageTypeTags.IS_FALL)){
            return false;
        }
        if(!this.notPetrified()){
            for(MinibossEntity boss : this.getWorld().getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class),this.getBoundingBox().expand(8), Predicates.alwaysTrue())) {
                if (!boss.notPetrified()) {
                    boss.removeStatusEffect(this.getIntroEffect());
                    boss.playReleaseParticlesAndSound();
                }

            }
            if (!this.notPetrified()) {
                this.removeStatusEffect(this.getIntroEffect());
                playReleaseParticlesAndSound();
            }
            return false;
        }
        boolean result = notPetrified() && super.damage(source, amount);
        if (result && brain != null) brain.onDamageTaken(amount);
        return result;
    }


    /**
     * Whether this miniboss may harm {@code candidate} with an attack.
     *
     * <p>Melee and spells now answer this the same way. Spell Engine's relations model — customised for
     * these mobs in {@code EntityRelationsMixin} — is the authority, so a Juggernaut's hammer and its slam
     * agree about who is a valid victim instead of each carrying its own opinion. Under that model a wild
     * miniboss may harm whatever it is actually fighting but will not cleave its own patrol apart, and a
     * tamed one may harm anything that is not its owner or a sibling under the same owner.
     *
     * <p>The three cases checked here first are the ones the relations model does not cover: never the
     * hand that owns you, never a downed miniboss unless betrayal is enabled, and never a sibling serving
     * the same master.
     */
    /** Spell power stated by this boss's entry in {@code RPGMinibossesEntities}, from config. */
    @Override
    public double magicPower(MagicSchool school) {
        return RPGMinibossesEntities.magicPower(this.getType(), school);
    }

    public boolean canHarm(Entity candidate) {
        if (candidate == this || candidate.isSpectator() || !(candidate instanceof LivingEntity living)
                || !living.isAlive()) {
            return false;
        }
        if (this.isTeammate(candidate)) {
            return false;
        }
        if (this.getOwnerUuid() != null && this.getOwnerUuid().equals(candidate.getUuid())) {
            return false;
        }
        if (candidate instanceof MinibossEntity other) {
            if (other.isIncapacitated() && !RPGMinibossesEntities.config.betrayal) {
                return false;
            }
            if (this.getOwnerUuid() != null && this.getOwnerUuid().equals(other.getOwnerUuid())) {
                return false;
            }
        }
        return allowedByRelations(candidate);
    }

    /**
     * The relation rules minibosses have always fought by — formerly applied to Spell Engine's relation
     * check by {@code EntityRelationsMixin}, now the core's own: an untamed boss only fights another boss it
     * is targeting, nobody fights a downed boss, and a tamed boss never turns on its owner or the owner's
     * other pets. Everything else falls to {@link Relations#baseCanHarm}.
     */
    private boolean allowedByRelations(Entity target) {
        if (target instanceof MinibossEntity other) {
            boolean allowed = Relations.baseCanHarm(this, target);
            if (!this.isTamed()) {
                allowed = this.getTarget() != null && this.getTarget().equals(target);
            }
            if (other.isIncapacitated()) {
                allowed = false;
            }
            return allowed;
        }
        var owner = this.getOwner();
        if (owner == null) {
            return true;
        }
        if (Objects.equals(owner, target)) {
            return false;
        }
        if (!(target instanceof Tameable) && !(target instanceof TameableEntity)) {
            return true;
        }
        var targetOwner = target instanceof Tameable tameable ? tameable.getOwner() : null;
        boolean sameOwner = targetOwner != null && Objects.equals(owner, targetOwner);
        boolean ownersMayFight = targetOwner == null
                || (owner instanceof PlayerEntity player && Relations.baseCanHarm(player, targetOwner));
        return !sameOwner && ownersMayFight;
    }

    /**
     * True when these two are actually fighting each other, or their factions are at war, rather than
     * merely standing near one another.
     * Used to decide whether the friendly-fire reduction applies — see {@link #damage}.
     */
    public boolean isEnemyOf(MinibossEntity other) {
        return super.getTarget() == other || other.getTarget() == this
                || com.cleannrooster.anarchy_rpg.faction.Factions.atWar(this, other);
    }

    public void playIntro(PlayerEntity player) {
            if(!this.notPetrified()) {
                this.removeStatusEffect(this.getIntroEffect());
                playReleaseParticlesAndSound();
            }
    }

   /* @Override
    public double getTick(Object entity) {
        if(entity instanceof LivingEntity living){
            if(!notPetrified()){
                return 0;
            }
        }
        return GeoEntity.super.getTick(entity);
    }*/

    @Override
    public double getEyeY() {
        return super.getEyeY();
    }


    public void playReleaseParticlesAndSound(){
        if(!this.getWorld().isClient()) {
            // What the old pound spell released: a golem's grunt and a puff of dust.
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_IRON_GOLEM_HURT,
                    this.getSoundCategory(), 1.0f, 1.0f);
            if (this.getWorld() instanceof ServerWorld world) {
                FxEmitter.emit(world, FxCues.SMOKE, CueArgs.at(MagicSchool.GENERIC, this.getPos().add(0.0, 0.5, 0.0),
                        1.5f, 0, world.random.nextLong()));
            }
        }
    }
    public int delay(){
        return 1;
    }
    public void applyIntroEffect(){
        this.addStatusEffect(new StatusEffectInstance(getIntroEffect(),-1,2,false,false));
    }
    public RegistryEntry<StatusEffect> getIntroEffect(){
        return Effects.PETRIFIED.registryEntry;
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
    }



    @Override
    public boolean isAiDisabled() {
        return (this.isIncapacitated() || !notPetrified() || super.isAiDisabled());
    }
    public boolean isTamed() {
        return this.getOwnerUuid() != null;
    }
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity) && !(target instanceof ArmorStandEntity)) {
            if (target instanceof WolfEntity) {
                WolfEntity wolfEntity = (WolfEntity)target;
                return !wolfEntity.isTamed() || wolfEntity.getOwner() != owner;
            }
            else if (target instanceof MinibossEntity tameable) {
                return (!tameable.isTamed() || !Objects.equals(tameable.getOwner(), this.getOwner())) &&!tameable.isIncapacitated();
            }
            else    if (target instanceof Tameable tameable) {
                return tameable.getOwnerUuid() == null || tameable.getOwnerUuid()!=this.getOwnerUuid();
            }

            else{
                if (target instanceof PlayerEntity) {
                    PlayerEntity playerEntity = (PlayerEntity)target;
                    if (owner instanceof PlayerEntity) {
                        PlayerEntity playerEntity2 = (PlayerEntity)owner;
                        if (!playerEntity2.shouldDamagePlayer(playerEntity)) {
                            return false;
                        }
                    }
                }

                if (target instanceof AbstractHorseEntity) {
                    AbstractHorseEntity abstractHorseEntity = (AbstractHorseEntity)target;
                    if (abstractHorseEntity.isTame()) {
                        return false;
                    }
                }

                boolean var10000;
                if (target instanceof TameableEntity) {
                    TameableEntity tameableEntity = (TameableEntity)target;
                    if (tameableEntity.isTamed()) {
                        var10000 = false;
                        return var10000;
                    }
                }

                var10000 = true;
                return var10000;
            }
        } else {
            return false;
        }
    }
    public boolean shouldWander = false;
    public boolean shouldWander(){
        return shouldWander && this.conversationPartner == null && this.getCustomer() == null;
    }
    public class WanderAroundFarGoalConditional extends WanderAroundGoal {
        public static final float CHANCE = 0.001F;
        protected final float probability;

        public WanderAroundFarGoalConditional(MinibossEntity pathAwareEntity, double d) {
            this(pathAwareEntity, d, 0.001F);
        }

        @Override
        public boolean canStart() {
            return ((MinibossEntity)this.mob).shouldWander() && !com.cleannrooster.anarchy_rpg.service.CampGuard.onWatch((MinibossEntity) this.mob)
                    && super.canStart();
        }

        @Override
        public boolean canStop() {
            return super.canStop() || !((MinibossEntity)this.mob).shouldWander()
                    || com.cleannrooster.anarchy_rpg.service.CampGuard.onWatch((MinibossEntity) this.mob);
        }

        public WanderAroundFarGoalConditional(PathAwareEntity mob, double speed, float probability) {
            super(mob, speed);
            this.probability = probability;
        }

        @Nullable
        protected Vec3d getWanderTarget() {
            if (this.mob.isInsideWaterOrBubbleColumn()) {
                Vec3d vec3d = FuzzyTargeting.find(this.mob, 15, 7);
                return vec3d == null ? super.getWanderTarget() : vec3d;
            } else {
                return this.mob.getRandom().nextFloat() >= this.probability ? FuzzyTargeting.find(this.mob, 10, 7) : super.getWanderTarget();
            }
        }
    }

    protected void initGoals() {
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(7, new WanderAroundFarGoalConditional(this, 1.0));
        this.goalSelector.add(6, new MinibossFollowOwner(this, 1.6, 6.0F, 2.0F));
        this.goalSelector.add(1, new DoorInteractGoalLong(this, true));

        this.goalSelector.add(0, new SwimGoal(this));
        if(RPGMinibossesEntities.config.betrayal) {
            this.targetSelector.add(2, new ActiveTargetGoal(this, MinibossEntity.class, true, (target) -> !this.isTamed() || (target instanceof LivingEntity living && this.isTamed() && this.canAttackWithOwner((LivingEntity) living,this.getOwner()))));

        }
        this.targetSelector.add(0, new MinibossTrackOwnerAttackerGoal(this));

        this.targetSelector.add(1, (new MinibossRevengeGoal(this, MinibossEntity.class).setGroupRevenge(new Class[0])));
        this.targetSelector.add(2, new MinibossAttackWithOwner(this));

        if(RPGMinibossesEntities.config.trueAnarchy) {
            this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true,  (player) ->!this.isTamed() ));
        }
        else{
            this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true, (player) ->!this.isTamed() && player instanceof ServerPlayerEntity playerEntity
                    && com.cleannrooster.anarchy_rpg.faction.Factions.disposition(this, playerEntity) == com.cleannrooster.anarchy_rpg.faction.Disposition.HOSTILE));

        }

        if (!RPGMinibossesEntities.config.betrayal && RPGMinibossesEntities.config.factionWarfare) {
            this.targetSelector.add(3, new ActiveTargetGoal<>(this, MinibossEntity.class, true, (target) -> !this.isTamed()
                    && target instanceof MinibossEntity other && !other.isTamed() && !other.isIncapacitated()
                    && com.cleannrooster.anarchy_rpg.faction.Factions.atWar(this, other)));
        }

        this.targetSelector.add(4, new UniversalAngerGoalMiniboss<>(this, true));


        this.initCustomGoals();
    }
    float movementspeedCoeff = 1;
    @Override
    public void setMovementSpeed(float movementSpeed) {
        this.movementspeedCoeff = movementSpeed;;
        super.setMovementSpeed(movementSpeed);
    }



    /**
     * Build this mob's brain purely so its {@link com.cleannrooster.anarchy_rpg.entity.combat.CombatAction}s
     * register their geometry. Called once per mob class on the client, where {@code initGoals} — and
     * so the real brain — never runs. The brain built here is discarded immediately; only the
     * side effect on {@code AttackRegistry} matters. Mobs with no brain need not override this.
     */
    public void registerAttackPrototypes() {
    }

    protected void initCustomGoals() {
    }
    public boolean isTwoHand(){
        return false;
    }

    public final boolean cannotFollowOwner() {
        return   this.isSitting() || this.hasVehicle() || this.mightBeLeashed() || this.getOwner() != null && this.getOwner().isSpectator();
    }



    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(),  0.05F, 0.8F);

        super.playStepSound(pos, state);
    }
    public void tryTeleportToOwner() {
        LivingEntity livingEntity = this.getOwner();
        if (livingEntity != null) {
            this.tryTeleportNear(livingEntity.getBlockPos());
        }

    }
    private boolean tryTeleportTo(int x, int y, int z) {
        if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.refreshPositionAndAngles((double)x + 0.5, (double)y, (double)z + 0.5, this.getYaw(), this.getPitch());
            this.navigation.stop();
            return true;
        }
    }
    protected boolean canTeleportOntoLeaves() {
        return false;
    }
    private boolean canTeleportTo(BlockPos pos) {
        PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(this, pos);
        if (pathNodeType != PathNodeType.WALKABLE ) {
            return false;
        } else {
            BlockState blockState = this.getWorld().getBlockState(pos.down());
            if (!this.canTeleportOntoLeaves() && blockState.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockPos = pos.subtract(this.getBlockPos());
                return this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(blockPos));
            }
        }
    }


    public boolean shouldTryTeleportToOwner() {
        LivingEntity livingEntity = this.getOwner();
        return livingEntity != null && this.squaredDistanceTo(this.getOwner()) >= 144.0;
    }

    private void tryTeleportNear(BlockPos pos) {
        for(int i = 0; i < 10; ++i) {
            int j = this.random.nextBetween(-3, 3);
            int k = this.random.nextBetween(-3, 3);
            if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
                int l = this.random.nextBetween(-1, 1);
                if (this.tryTeleportTo(pos.getX() + j, pos.getY() + l, pos.getZ() + k)) {
                    return;
                }
            }
        }

    }



 /*   private PlayState predicate2(AnimationState<MinibossEntity> state) {
        state.setControllerSpeed(this.isIncapacitated() ? 1F : (float) (state.isMoving() ? this.getVelocity().length()/0.1F : 1F));
        if(this.isIncapacitated()){
            return  state.setAndContinue(DOWNANIM);
        }
        if(state.isMoving()){

            if(this.isTwoHand()){
                if( this.getVelocity().length() > 0.01F && this.getVelocity().normalize().dotProduct(this.getRotationVector().normalize()) < -0.0 ){
                    return this.isAttacking() ? state.setAndContinue(WALK_B_2h_T)  : state.setAndContinue(WALK_NO_AGGRO);
                }
                return this.isAttacking() ?  state.setAndContinue(WALK2H) : this.getVelocity().length() > 0.2F ? state.setAndContinue(SPRINT):  state.setAndContinue(WALK_NO_AGGRO) ;


            }
            if(this.getVelocity().length() > 0.001F && this.getVelocity().normalize().dotProduct(this.getRotationVector().normalize()) < -0 ){
                return this.isAttacking() && this.isTwoHand() ? state.setAndContinue(WALK_B_2h_T) : this.isAttacking() ? this.isTwoHand() ?   state.setAndContinue(WALK_B_2h_T) : state.setAndContinue(WALK_B_T) :  state.setAndContinue(WALKING_BACKWARDS);
            }
            return   this.getVelocity().length() > 0.2F ?
                    this.isAttacking() ? state.setAndContinue(WALK) :  state.setAndContinue(SPRINT) :
                    this.isAttacking() ? state.setAndContinue(WALK) :  state.setAndContinue(WALK_NO_AGGRO);


        }
        if(this.isTwoHand()) {

            return this.isAttacking() ? state.setAndContinue(IDLE2H) : state.setAndContinue(IDLE);
        }
        return this.isAttacking() ? state.setAndContinue(IDLE_AGGRO) : state.setAndContinue(IDLE);

    }
*/


    public boolean isMobile() {
        return false;
    }


    private int ageWhenTargetSet;

    public void setTarget(@Nullable LivingEntity target) {
        if(this.getWorld().getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
            super.setTarget(null);
            return;
        }
        if (target == null) {
            this.ageWhenTargetSet = 0;
        } else {
            this.ageWhenTargetSet = this.age;
        }
        if (target instanceof ServerPlayerEntity player && this.getOwnerUuid() == null) {
            com.cleannrooster.anarchy_rpg.faction.TargetDiagnostics.onTarget(this, this.getTarget(), player);
        }
        super.setTarget(target);

    }



    private static final UniformIntProvider ANGER_TIME_RANGE;

    public int angerTime;

    private UUID angryAt;
    static{
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    }
    public void setOwnerUuid(@Nullable UUID uuid) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(uuid));
    }
    public void setCantHire(boolean cantHire ) {
        this.dataTracker.set(CANTHIRE, cantHire);
    }
    public boolean getCantHire() {
        return (boolean) this.dataTracker.get(CANTHIRE);
    }
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("shouldWander", this.shouldWander());
        if (this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }
        if (this.getDataTracker().get(NAME) != -1) {
            nbt.putInt("exileName", this.getDataTracker().get(NAME));
        }
        nbt.putBoolean("Sitting", this.sitting);
        nbt.putBoolean("cantHire", this.getDataTracker().get(CANTHIRE));
        nbt.putBoolean("FromStructure", this.fromStructure);
        if (this.partyMembership != null) {
            nbt.put("Party", this.partyMembership.toNbt());
        }
        nbt.putBoolean("Forsaken", this.dataTracker.get(FORSAKEN));
        nbt.put("ExchangeStock", this.exchangeStock.toNbt(this.getRegistryManager()));
        nbt.put("RumorCharges", this.rumorCharges.toNbt());
        nbt.put("CampServiceUses", this.campServiceUses.toNbt());
        nbt.putLong("GuardUntil", this.guardUntil);
        nbt.putBoolean("CompanionDowned", this.getDataTracker().get(COMPANION_DOWNED));
        nbt.putLong("CompanionIncarnation", this.companionIncarnation);
        if (this.departing) {
            nbt.putBoolean("Departing", true);
        }

    }


    @Nullable
    @Override
    public LivingEntity getOwner() {
        return Tameable.super.getOwner();
    }

    private boolean sitting;
    private static final Logger LOGGER;

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        UUID uUID;

        if (nbt.containsUuid("Owner")) {
            uUID = nbt.getUuid("Owner");
        } else {
            String string = nbt.getString("Owner");
            uUID = ServerConfigHandler.getPlayerUuidByName(this.getServer(), string);
        }
        boolean cantHire;
        if (nbt.contains("cantHire")) {
            cantHire = nbt.getBoolean("cantHire");
        } else {
            cantHire = false;
        }
        if (nbt.contains("shouldWander")) {
            shouldWander = nbt.getBoolean("shouldWander");
        } else {
            shouldWander = true;
        }
        if (nbt.contains("exileName")) {
            this.getDataTracker().set(NAME,nbt.getInt("exileName"));
        }
        if (uUID != null) {
            this.setOwnerUuid(uUID);

        }
        this.getDataTracker().set(CANTHIRE,cantHire);
        this.sitting = nbt.getBoolean("Sitting");
        this.fromStructure = nbt.getBoolean("FromStructure");
        if (nbt.contains("Party")) {
            this.partyMembership = com.cleannrooster.anarchy_rpg.faction.PartyMembership.fromNbt(nbt.getCompound("Party"));
        }
        this.dataTracker.set(FORSAKEN, nbt.getBoolean("Forsaken"));
        if (nbt.contains("ExchangeStock")) {
            this.exchangeStock.readNbt(nbt.getCompound("ExchangeStock"), this.getRegistryManager());
        }
        if (nbt.contains("RumorCharges")) {
            this.rumorCharges.readNbt(nbt.getCompound("RumorCharges"));
        }
        if (nbt.contains("CampServiceUses")) {
            this.campServiceUses.readNbt(nbt.getCompound("CampServiceUses"));
        }
        this.guardUntil = nbt.getLong("GuardUntil");
        this.getDataTracker().set(COMPANION_DOWNED, nbt.getBoolean("CompanionDowned"));
        this.companionIncarnation = nbt.getLong("CompanionIncarnation");
        // A departing ex-companion that loads again simply finishes leaving.
        this.departing = nbt.getBoolean("Departing");

    }
    static {
        LOGGER = LogUtils.getLogger();
    }


    public boolean isSitting() {
        return sitting;
    }

    public void setSitting(boolean sitting) {
        this.sitting = sitting;
    }

    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
    }

    public void setAngerTime(int angerTime) {
        this.angerTime = angerTime;
    }

    public int getAngerTime() {
        return this.angerTime;
    }

    public void setAngryAt(@Nullable UUID angryAt) {
        if(angryAt != this.angryAt && this.angerTime == 0 && !this.getWorld().isClient() &&   ((ServerWorld)this.getWorld()).getEntity(angryAt) instanceof ServerPlayerEntity player){
          com.cleannrooster.anarchy_rpg.faction.Factions.recordDeed(this, player,
                  com.cleannrooster.anarchy_rpg.faction.StandingCause.PROVOKED, RPGMinibossesEntities.config.standingProvokeMember);

        }
        this.angryAt = angryAt;
    }

    @Override
    protected float getJumpVelocity(float strength) {
        return 1.5F*super.getJumpVelocity(strength);
    }
    @Nullable
    public UUID getAngryAt() {
        return this.angryAt;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return (UUID)((Optional)this.dataTracker.get(OWNER_UUID)).orElse((Object)null);
    }


    public static boolean canSpawn(EntityType<? extends PathAwareEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return spawnReason.equals(SpawnReason.PATROL) || ( world.getDifficulty() != Difficulty.PEACEFUL && (SpawnReason.isTrialSpawner(spawnReason) ||   isSpawnDark((ServerWorldAccess)world, pos, random)) && canMobSpawn(type, world, spawnReason, pos, random));

    }

    @Override
    protected boolean isDisallowedInPeaceful() {
        return !this.isTamed();
    }
    /**
     * Whoever has one of this adventurer's windows open (Exchange, Storage, Equipment, Hire). It stands still
     * for them and talks to nobody else meanwhile. Server-side; not saved.
     */
    @Nullable
    private PlayerEntity customer;

    public void setCustomer(@Nullable PlayerEntity customer) {
        this.customer = customer;
    }

    @Nullable
    public PlayerEntity getCustomer() {
        return this.customer;
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
                this.lookAtTimer = 30;

        }

        public void tick() {
            if (this.entity.getWorld().isClient()) return; // handled in MinibossEntity.tickHeadRotation()
            if (this.lookAtTimer > 0) {
                this.getTargetYaw().ifPresent((yaw) -> {
                    this.entity.setHeadYaw(this.changeAngle(this.entity.headYaw, yaw, this.maxYawChange));
                    this.entity.prevHeadYaw = this.entity.headYaw;
                });
                this.getTargetPitch().ifPresent((pitch) -> {
                    this.entity.setPitch(this.changeAngle(this.entity.getPitch(), pitch, this.maxPitchChange));
                    this.entity.prevPitch = this.entity.getPitch();
                });
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
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isTamed();
    }


    @Override
    public void checkDespawn() {
        super.checkDespawn();
    }

    public boolean shouldRun(){

        return isRunning;
    }
    public boolean wasRunningLastTick;
    public float smoothedSpeed = 0.2F;
    Runnable animationRunner;
    Runnable prevAnimationRunner;
    boolean wasRunning;
    boolean isRunning;
    AnimState prevState;
    /** Speed the last locomotion clip was dispatched at; re-sent only when it drifts meaningfully. */
    private float prevAnimSpeed = -1F;
    /** Candidate state and how long it has held, so a single frame can't thrash the clip. */
    private AnimState candidateState;
    private int candidateTicks;

    enum AnimState {
        // Out of combat — the pre-overhaul behaviour, unchanged.
        IDLE, WALK, RUN,
        // In combat: the directional set. Which one plays is decided from the mob's actual displacement
        // resolved against its body facing, so what the model does matches where the body goes.
        STANCE, ADVANCE, LATERAL_LEFT, LATERAL_RIGHT, BACKSTEP,
        DOWN
    }

    /** Below this displacement per tick the mob is treated as standing. */
    private static final float MOVE_EPSILON = 0.012F;
    /** Ground speed a locomotion clip is authored for; the clip is time-scaled around it. */
    private static final float NOMINAL_COMBAT_SPEED = 0.16F;
    /** Ticks a new direction must hold before the clip changes. */
    private static final int DIRECTION_DEBOUNCE = 2;

    public void animTick(){

        if (this.getWorld().isClient) {
            // Real displacement, not reported velocity: vanilla decays velocity by ground friction after
            // it has been applied, so the velocity field understates what actually happened this tick.
            double dx = this.getX() - this.prevX;
            double dz = this.getZ() - this.prevZ;
            float horizontalSpeed = (float) Math.sqrt(dx * dx + dz * dz);

            // Asymmetric filter: snap up quickly when accelerating, decay slowly when stopping.
            // Fast ramp-up (0.6) makes new movement feel responsive; slow decay (0.2) prevents
            // a pop-to-idle on momentary zero-velocity frames and smooths the stop tail-off.
            float alpha = horizontalSpeed > this.smoothedSpeed ? 0.6F : 0.2F;
            this.smoothedSpeed = MathHelper.lerp(alpha, this.smoothedSpeed, horizontalSpeed);

            boolean isMoving = this.smoothedSpeed > MOVE_EPSILON;
            this.isRunning = this.wasRunning
                    ? this.smoothedSpeed > 0.15F
                    : this.smoothedSpeed >= 0.20F;
            this.wasRunning = this.isRunning;

            AnimState newState;
            float animSpeed;

            if (this.isIncapacitated()) {
                newState = AnimState.DOWN;
                animSpeed = 1F;
            } else if (this.isAttacking()) {
                animSpeed = MathHelper.clamp(this.smoothedSpeed / NOMINAL_COMBAT_SPEED, 0.55F, 1.9F);
                newState = isMoving ? combatDirection(dx, dz, horizontalSpeed) : AnimState.STANCE;
            } else if (isMoving) {
                animSpeed = MathHelper.clamp(this.smoothedSpeed / 0.2F, 0.5F, 2.0F);
                newState = this.isRunning ? AnimState.RUN : AnimState.WALK;
            } else {
                newState = AnimState.IDLE;
                animSpeed = 1F;
            }

            // Debounce direction changes only. Entering or leaving combat, going down, or stopping should
            // all read immediately; it is the left/right/forward flicker that needs settling.
            if (newState != this.prevState) {
                boolean directional = isDirectional(newState) && isDirectional(this.prevState);
                if (directional) {
                    if (newState == this.candidateState) {
                        this.candidateTicks++;
                    } else {
                        this.candidateState = newState;
                        this.candidateTicks = 1;
                    }
                    if (this.candidateTicks < DIRECTION_DEBOUNCE) {
                        newState = this.prevState;
                    }
                } else {
                    this.candidateState = newState;
                    this.candidateTicks = DIRECTION_DEBOUNCE;
                }
            }

            boolean speedChanged = Math.abs(animSpeed - this.prevAnimSpeed) > 0.22F;
            if (newState != this.prevState || (speedChanged && isDirectional(newState))) {
                dispatchLocomotion(newState, animSpeed);
                this.prevState = newState;
                this.prevAnimSpeed = animSpeed;
            }
        }
    }

    /** Resolve this tick's displacement against the body's facing into one of the four directional clips. */
    private AnimState combatDirection(double dx, double dz, float speed) {
        Vec3d forward = Vec3d.fromPolar(0F, this.bodyYaw);
        Vec3d right   = Vec3d.fromPolar(0F, this.bodyYaw + 90F);
        double along   = dx * forward.x + dz * forward.z;
        double lateral = dx * right.x + dz * right.z;

        // Lateral wins only when it clearly dominates, so a curving advance still reads as an advance.
        if (Math.abs(lateral) > Math.abs(along) * 1.25) {
            return lateral > 0 ? AnimState.LATERAL_RIGHT : AnimState.LATERAL_LEFT;
        }
        if (along < -speed * 0.2) {
            return AnimState.BACKSTEP;
        }
        return AnimState.ADVANCE;
    }

    private static boolean isDirectional(AnimState state) {
        return state == AnimState.ADVANCE || state == AnimState.LATERAL_LEFT
                || state == AnimState.LATERAL_RIGHT || state == AnimState.BACKSTEP;
    }

    private void dispatchLocomotion(AnimState state, float animSpeed) {
        switch (state) {
            case DOWN -> dispatcher.setDown();
            case STANCE -> dispatcher.stance();
            case ADVANCE -> dispatcher.advance(animSpeed);
            case LATERAL_LEFT -> dispatcher.lateral(1, animSpeed);
            case LATERAL_RIGHT -> dispatcher.lateral(-1, animSpeed);
            case BACKSTEP -> dispatcher.backstep(animSpeed);
            case RUN -> dispatcher.run(animSpeed);
            case WALK -> dispatcher.walk(animSpeed);
            case IDLE -> dispatcher.idle();
        }
    }
/*
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return instanceCache;
    }
*/
}
