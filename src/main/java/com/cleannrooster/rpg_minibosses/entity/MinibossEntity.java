package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.AI.*;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.*;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.object.PlayState;
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
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
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
import net.spell_engine.api.effect.Synchronized;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.utils.SoundHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static java.lang.Math.max;
import static net.minecraft.entity.mob.HostileEntity.canSpawnIgnoreLightLevel;
import static net.minecraft.entity.mob.HostileEntity.isSpawnDark;

public class MinibossEntity extends PathAwareEntity implements Tameable, GeoEntity, Angerable, Merchant {
    private UUID ownerUuid;
    public boolean performing;

    protected MinibossEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.lookControl = new MinibossLookControl(this);
        this.moveControl = new MinibossMoveConrol(this);

    }

    public TradeOffer create(ItemStack stack, int price, int maxUses, int experience, int multiplier,  Entity entity, Random random) {
        return new TradeOffer( new TradedItem(Registries.ITEM.get(Identifier.tryParse(RPGMinibossesEntities.config.tradeItem)), price),new ItemStack(stack.getItem()), maxUses, experience, multiplier*RPGMinibossesEntities.config.tradeMultiplier);
    }


    public static boolean canSpawnIgnoreLightLevel(EntityType<? extends PathAwareEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getDifficulty() != Difficulty.PEACEFUL && canMobSpawn(type, world, spawnReason, pos, random);
    }

    protected MinibossEntity(EntityType<? extends PathAwareEntity> entityType, World world, float spawnCoeff) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.spawnCoeff = spawnCoeff;
        this.moveControl = new MinibossMoveConrol(this);

        this.lookControl = new MinibossLookControl(this);
    }
    public List<String> NAMES = List.of(
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.1",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.2",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.3",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.4");
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HAS_ROLLED, false);
        builder.add(LESSER, false);
        builder.add(NAME, -1);
        builder.add(DOWN, false);
        builder.add(INDICATOR, 40);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(CANTHIRE, false);

    }

    @Override
    public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        if(spawnReason.equals(SpawnReason.TRIAL_SPAWNER)){
            this.setCantHire(true);
        }
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    List<Item> bonusList = List.of();

    @Override
    public Text getName() {
        if(this.getDataTracker().get(NAME) != -1){
            return Text.translatable(this.NAMES.get(this.getDataTracker().get(NAME)));
        }

        return super.getName();
    }
    @Environment(value = EnvType.CLIENT)
    public static void setRotationFromVelocity(Entity entity) {
        Vec3d vec3d = entity.getVelocity();
        if (vec3d.lengthSquared() != 0.0 && entity instanceof PathAwareEntity pathAwareEntity) {

            vec3d = pathAwareEntity.getVelocity().multiply(-1);
            double d = vec3d.horizontalLength();
            float yaw = (float)(MathHelper.atan2(vec3d.z, vec3d.x) * 57.2957763671875) + 90.0F;
            yaw = MathHelper.clamp(yaw,pathAwareEntity.headYaw -45, pathAwareEntity.headYaw+45);



            while(yaw -  ((PathAwareEntity) entity).prevBodyYaw < -180.0F) {
                ((PathAwareEntity) entity).prevBodyYaw -= 360.0F;
            }

            while(yaw -  ((PathAwareEntity) entity).prevBodyYaw >= 180.0F) {
                ((PathAwareEntity) entity).prevBodyYaw += 360.0F;
            }

            ((PathAwareEntity) entity).bodyYaw = (MathHelper.lerp(0.2F, (((PathAwareEntity) entity).prevBodyYaw), yaw));
            ((PathAwareEntity) entity).prevBodyYaw = ((PathAwareEntity) entity).bodyYaw;
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
            ((PathAwareEntity) entity).prevHeadYaw = ((PathAwareEntity) entity).headYaw;

            ((PathAwareEntity) entity).bodyYaw = (MathHelper.lerp(0.2F, (((PathAwareEntity) entity).prevBodyYaw), yaw));
            ((PathAwareEntity) entity).prevBodyYaw = ((PathAwareEntity) entity).bodyYaw;
        }
    }
    private boolean isLesser(){
        return this.getDataTracker().get(MinibossEntity.LESSER);

    }
    private static final TrackedData<Boolean> HAS_ROLLED ;
    public static final TrackedData<Boolean> DOWN;

    public static final TrackedData<Boolean> LESSER ;
    public static final TrackedData<Integer> NAME ;

    public static final TrackedData<Integer> INDICATOR ;
    public static final TrackedData<Optional<UUID>> OWNER_UUID ;
    public static final TrackedData<Boolean> CANTHIRE ;

    static{
        HAS_ROLLED = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        LESSER = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        NAME = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);
        DOWN = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        INDICATOR = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);
        OWNER_UUID = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
        CANTHIRE = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    }

    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.unknown.walk");
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

    public float spawnCoeff = 1;
    public AnimatableInstanceCache instanceCache = AzureLibUtil.createInstanceCache(this);
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {

        animationData.add(new AnimationController<MinibossEntity>(this, "walk",
                0, this::predicate2)
        );


    }
        public int getIndicator(){
            return this.getDataTracker().get(INDICATOR);

        }
        public void playBoom(){
            SoundHelper.playSound(this.getWorld(), this,new Sound( RPGMinibosses.ANTICIPATION_SOUND.getId().toString()));
        }
    public void resetIndicator(){
        this.getDataTracker().set(INDICATOR,0);
        this.playBoom();
    }
    public void tickIndicator(){
        this.getDataTracker().set(INDICATOR,this.getDataTracker().get(INDICATOR)+1);
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
        if(!this.getCantHire()) {
            if (!damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !this.getDataTracker().get(DOWN) && this.getWorld() instanceof ServerWorld && this.getOwnerUuid() == null) {
                this.setHealth(0.01F);
                (this).triggerAnim("down", "down");

                this.getDataTracker().set(DOWN, true);
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 9, false, false));
                return;
            }
        }

        if( damageSource.getAttacker() instanceof ServerPlayerEntity player){
            player.getStatHandler().setStat(player,Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY),player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY))+5);
        }
        super.onDeath(damageSource);
    }


    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if(this.getDataTracker().get(DOWN)) {
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
        if(this.getTarget() != null){
            if(this.getTarget() instanceof MinibossEntity entity && entity.getDataTracker().get(DOWN) && this.isTamed()){
                this.setTarget(null);
            }
        }
        if(this.firstUpdate && !this.getWorld().isClient()){
            if(this.getDataTracker().get(DOWN)){
                (this).triggerAnim("down","down");

            }
            if(this.getDataTracker().get(NAME) == -1){
                if((this.isLesser() && RPGMinibossesEntities.config.lesserPetrify > this.getRandom().nextFloat()) || (!this.isLesser() && RPGMinibossesEntities.config.greaterPetrify > this.getRandom().nextFloat())) {
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
        super.tick();
        if(this.getWorld() instanceof ServerWorld){
            this.tickIndicator();
        }
            if (this.getWorld().isClient() && this instanceof ArtilleristEntity artilleristEntity && artilleristEntity.getDataTracker().get(ArtilleristEntity.RUNNING) && artilleristEntity.getVelocity().length() > 0.01F) {
                setRotationAndHeadFromVelocity(this);

            } else if (this.getWorld().isClient() ) {
                setRotationFromVelocity(this);
            }

    }

    @Nullable
    @Override
    public LivingEntity getTarget() {
        if(this.getWorld().getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
            return null;
        }
        if(super.getTarget() != null){
            if(super.getTarget() instanceof MinibossEntity entity && entity.getDataTracker().get(DOWN) && this.isTamed()){
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

    @Override
    public void setAiDisabled(boolean aiDisabled) {
        super.setAiDisabled(aiDisabled);
    }

    @Override
    public boolean isPersistent() {
        return super.isPersistent();
    }

    @Override
    protected void mobTick() {

        super.mobTick();

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
        super.dropEquipment(world, source, causedByPlayer);
    }
    private void beginTradeWith(PlayerEntity customer) {
        this.setCustomer(customer);
        this.sendOffers(customer, this.getDisplayName(), 0);
    }
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if(hand.equals(Hand.MAIN_HAND)) {
            if(this.getWorld().getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
                this.beginTradeWith(player);
                return ActionResult.SUCCESS_NO_ITEM_USED;
            }
            if (!this.notPetrified()) {
                for (MinibossEntity boss : this.getWorld().getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class), this.getBoundingBox().expand(8), Predicates.alwaysTrue())) {
                    if (!boss.notPetrified()) {
                        boss.playIntro(player);

                    }
                }
                playIntro(player);
                return ActionResult.SUCCESS_NO_ITEM_USED;

            }

            if (this.getOwnerUuid() != null && Objects.equals(this.getOwner(), player)) {
                this.setSitting(!this.sitting);
                if (this.getWorld() instanceof ServerWorld) {
                    if (this.sitting) {
                        player.sendMessage(this.getName().copy().append(Text.translatable(": ")).append(Text.of("I will stay here.")));
                    } else {
                        player.sendMessage(this.getName().copy().append(Text.translatable(": ")).append(Text.of("I will follow.")));

                    }
                }
                return ActionResult.SUCCESS_NO_ITEM_USED;

            }
            if (this.getDataTracker().get(DOWN) && !this.getCantHire()) {
                if (this.getOwnerUuid() == null) {
                    this.setOwnerUuid(player.getUuid());
                    this.getDataTracker().set(DOWN, false);
                    this.heal(this.getMaxHealth());
                    return ActionResult.SUCCESS_NO_ITEM_USED;

                }
            }
        }
        return ActionResult.PASS;
    }
    public String introTranslation(){
        return "text.rpg-minibosses.petrified";
    }
    public boolean notPetrified(){
       return  Synchronized.effectsOf(this).stream().noneMatch(effect -> effect.effect() == this.getIntroEffect().value());
    }
    @Override
    public boolean isInvulnerable() {
        return  Synchronized.effectsOf(this).stream().noneMatch(effect -> effect.effect() == this.getIntroEffect().value()) && super.isInvulnerable();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(amount < 1000000 && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && this.getWorld().getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
            return false;
        }
        if(this.getDataTracker().get(DOWN) && source.getAttacker() instanceof MinibossEntity && !RPGMinibossesEntities.config.betrayal){
            return false;
        }
        if(source.getAttacker() instanceof MinibossEntity entity && !RPGMinibossesEntities.config.betrayal && entity.getOwnerUuid() == null){
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
        return  (  Synchronized.effectsOf(this).stream().noneMatch(effect -> effect.effect() == this.getIntroEffect().value()) && super.damage(source, amount));
    }


    public void playIntro(PlayerEntity player) {
            if(!this.notPetrified()) {
                this.removeStatusEffect(this.getIntroEffect());
                playReleaseParticlesAndSound();
            }
    }

    @Override
    public double getTick(Object entity) {
        if(entity instanceof LivingEntity living){
            if(!notPetrified()){
                return 0;
            }
        }
        return GeoEntity.super.getTick(entity);
    }

    @Override
    public double getEyeY() {
        return super.getEyeY();
    }


    public void playReleaseParticlesAndSound(){
        if(!this.getWorld().isClient()) {
            ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
            SoundHelper.playSound(this.getWorld(), this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.sound);
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
        return (this.getDataTracker().get(DOWN) || !notPetrified() || super.isAiDisabled());
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
                return (!tameable.isTamed() || !Objects.equals(tameable.getOwner(), this.getOwner())) &&!tameable.getDataTracker().get(DOWN);
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
    protected void initGoals() {
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new MinibossFollowOwner(this, 1.6, 6.0F, 2.0F));

        this.goalSelector.add(1, new SwimGoal(this));
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
                    && playerEntity.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY)) > 5 + playerEntity.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.BENEVOLENCE))));

        }

        this.targetSelector.add(4, new UniversalAngerGoalMiniboss<>(this, true));


        this.initCustomGoals();
    }

    @Override
    public float getMovementSpeed() {
        return (this.getDataTracker().get(INDICATOR) < 40F ? 0.25F : 1F) * (this.sitting ? 0 : (float) (this.getOwner() != null && !this.isAttacking() ? (float) this.getOwner().getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * (2.4F) : this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));
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



    private PlayState predicate2(AnimationState<MinibossEntity> state) {
        state.setControllerSpeed(this.getDataTracker().get(DOWN) ? 1F : (float) (state.isMoving() ? this.getVelocity().length()/0.1F : 1F));
        if(this.getDataTracker().get(DOWN)){
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
        if (this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }
        if (this.getDataTracker().get(NAME) != -1) {
            nbt.putInt("exileName", this.getDataTracker().get(NAME));
        }
        if (!this.getWorld().isClient) {
            TradeOfferList tradeOfferList = this.getOffers();
            if (tradeOfferList != null && !tradeOfferList.isEmpty()) {
                nbt.put("Offers", (NbtElement)TradeOfferList.CODEC.encodeStart(this.getRegistryManager().getOps(NbtOps.INSTANCE), tradeOfferList).getOrThrow());
            }
        }
        nbt.putBoolean("Sitting", this.sitting);
        nbt.putBoolean("cantHire", this.getDataTracker().get(CANTHIRE));

    }
    protected TradeOfferList offers;


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
        if (nbt.containsUuid("cantHire")) {
            cantHire = nbt.getBoolean("cantHire");
        } else {
            cantHire = false;
        }

        if (nbt.contains("Offers")) {
            DataResult var10000 = TradeOfferList.CODEC.parse(this.getRegistryManager().getOps(NbtOps.INSTANCE), nbt.get("Offers"));
            Logger var10002 = LOGGER;
            Objects.requireNonNull(var10002);
            var10000.resultOrPartial(Util.addPrefix("Failed to load offers: ", var10002::warn)).ifPresent((offers) -> {
                this.offers =(TradeOfferList) offers;
            });
        }
        if (nbt.contains("exileName")) {
            this.getDataTracker().set(NAME,nbt.getInt("exileName"));
        }
        if (uUID != null) {
            this.setOwnerUuid(uUID);

        }
        this.getDataTracker().set(CANTHIRE,cantHire);
        this.sitting = nbt.getBoolean("Sitting");

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
          player.getStatHandler().setStat(player,Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY),player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY))+1);

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
    public PlayerEntity customer;

    @Override
    public void setCustomer(@Nullable PlayerEntity customer) {
        this.customer = customer;
    }

    @Nullable
    @Override
    public PlayerEntity getCustomer() {
        return customer;
    }
    public TradeOfferList getOffers() {
        if (this.getWorld().isClient) {
            throw new IllegalStateException("Cannot load Villager offers on the client");
        } else {
            if (this.offers == null && !this.bonusList.isEmpty()) {
                this.offers = new TradeOfferList();
                RegistryKey<LootTable> registryKey = EntityType.ENDER_DRAGON.getLootTableId();

                LootTable lootTable = this.getWorld().getServer().getReloadableRegistries().getLootTable(registryKey);
                LootContextParameterSet.Builder builder = (new LootContextParameterSet.Builder((ServerWorld) this.getWorld())).add(LootContextParameters.THIS_ENTITY, this).add(LootContextParameters.ORIGIN, this.getPos()).add(LootContextParameters.DAMAGE_SOURCE, this.getDamageSources().generic());


                LootContextParameterSet lootContextParameterSet = builder.build(LootContextTypes.ENTITY);

                this.offers.addAll(
                        List.of(
                                create(this.bonusList.get(this.getRandom().nextInt(this.bonusList.size())).getDefaultStack(),this.getRandom().nextInt(5)+RPGMinibossesEntities.config.tradeAmount,this.getRandom().nextInt(5)+1, 20,1,this,this.getRandom()),
                                create(this.bonusList.get(this.getRandom().nextInt(this.bonusList.size())).getDefaultStack(),this.getRandom().nextInt(5)+RPGMinibossesEntities.config.tradeAmount,this.getRandom().nextInt(5)+1, 20,1,this,this.getRandom()),

                                create(this.bonusList.get(this.getRandom().nextInt(this.bonusList.size())).getDefaultStack(),this.getRandom().nextInt(5)+RPGMinibossesEntities.config.tradeAmount,this.getRandom().nextInt(5)+1, 20,1,this,this.getRandom())))

                ;
                lootTable.generateLoot(lootContextParameterSet, this.getLootTableSeed(), (itemStack) ->{
                        this.offers.add(create(itemStack,(this.getRandom().nextInt(5)+RPGMinibossesEntities.config.tradeAmount)*RPGMinibossesEntities.config.rareTradeMultiplier,this.getRandom().nextInt(5)+1, 20,1,this,this.getRandom()));

                });

            }

            return this.offers;
        }
    }
    protected void fillRecipes() {
        if (this.getWorld().getEnabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            this.fillRebalancedRecipes();
        } else {
            TradeOffers.Factory[] factorys = (TradeOffers.Factory[])TradeOffers.WANDERING_TRADER_TRADES.get(1);
            TradeOffers.Factory[] factorys2 = (TradeOffers.Factory[])TradeOffers.WANDERING_TRADER_TRADES.get(2);
            if (factorys != null && factorys2 != null) {
                TradeOfferList tradeOfferList = this.getOffers();
                this.fillRecipesFromPool(tradeOfferList, factorys, 5);
                int i = this.random.nextInt(factorys2.length);
                TradeOffers.Factory factory = factorys2[i];
                TradeOffer tradeOffer = factory.create(this, this.random);
                if (tradeOffer != null) {
                    tradeOfferList.add(tradeOffer);
                }

            }
        }
    }



    private void fillRebalancedRecipes() {
        TradeOfferList tradeOfferList = this.getOffers();
        Iterator var2 = TradeOffers.REBALANCED_WANDERING_TRADER_TRADES.iterator();

        while(var2.hasNext()) {
            org.apache.commons.lang3.tuple.Pair<TradeOffers.Factory[], Integer> pair = (Pair)var2.next();
            TradeOffers.Factory[] factorys = (TradeOffers.Factory[])pair.getLeft();
            this.fillRecipesFromPool(tradeOfferList, factorys, (Integer)pair.getRight());
        }

    }
    protected void fillRecipesFromPool(TradeOfferList recipeList, TradeOffers.Factory[] pool, int count) {
        ArrayList<TradeOffers.Factory> arrayList = Lists.newArrayList(pool);
        int i = 0;

        while(i < count && !arrayList.isEmpty()) {
            TradeOffer tradeOffer = ((TradeOffers.Factory)arrayList.remove(this.random.nextInt(arrayList.size()))).create(this, this.random);
            if (tradeOffer != null) {
                recipeList.add(tradeOffer);
                ++i;
            }
        }

    }

    @Override
    public void setOffersFromServer(TradeOfferList offers) {
        this.offers = offers;
    }

    @Override
    public void trade(TradeOffer offer) {
        offer.use();
        this.ambientSoundChance = -this.getMinAmbientSoundDelay();
        this.afterUsing(offer);

    }

    private void afterUsing(TradeOffer offer) {
    }

    @Override
    public void onSellingItem(ItemStack stack) {
        if (!this.getWorld().isClient && this.ambientSoundChance > -this.getMinAmbientSoundDelay() + 20) {
            this.ambientSoundChance = -this.getMinAmbientSoundDelay();
            this.playSound(this.getTradingSound(!stack.isEmpty()));
        }
    }
    protected SoundEvent getTradingSound(boolean sold) {
        return sold ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO;
    }
    @Override
    public int getExperience() {
        return 0;
    }

    @Override
    public void setExperienceFromServer(int experience) {

    }

    @Override
    public boolean isLeveledMerchant() {
        return false;
    }

    @Override
    public SoundEvent getYesSound() {
        return null;
    }

    @Override
    public boolean isClient() {
        return this.getWorld().isClient;
    }

    public class MinibossMoveConrol extends MoveControl{

        public MinibossMoveConrol(MobEntity entity) {
            super(entity);
        }

        public void tick() {
            float n;
            if (this.state == MoveControl.State.STRAFE) {
                float f = (float)this.entity.getMovementSpeed();
                float g = (float)this.speed * f;
                float h = this.forwardMovement;
                float i = this.sidewaysMovement;
                float j = MathHelper.sqrt(h * h + i * i);
                if (j < 1.0F) {
                    j = 1.0F;
                }

                j = g / j;
                h *= j;
                i *= j;
                float k = MathHelper.sin(this.entity.getYaw() * 0.017453292F);
                float l = MathHelper.cos(this.entity.getYaw() * 0.017453292F);
                float m = h * l - i * k;
                n = i * l + h * k;
                if (!this.isPosWalkable(m, n) && this.state == MoveControl.State.JUMPING) {
                    this.forwardMovement = 1.0F;
                    this.sidewaysMovement = 0.0F;
                }
                BlockPos blockPos = this.entity.getBlockPos();
                BlockState blockState = this.entity.getWorld().getBlockState(blockPos);
                VoxelShape voxelShape = blockState.getCollisionShape(this.entity.getWorld(), blockPos);
                if (isOnGround() && (horizontalCollision || this.entity.getWorld().getBlockState(BlockPos.ofFloored(this.entity.getPos().add(0,0,0).add(this.entity.getMovement().subtract(0,this.entity.getMovement().getY(),0).multiply(20)))).isSolidBlock(this.entity.getWorld(),BlockPos.ofFloored(this.entity.getPos().add(0,0,0).add(this.entity.getMovement().subtract(0,this.entity.getMovement().getY(),0).multiply(20))))) ) {
                    this.entity.getJumpControl().setActive();
                    this.state = MoveControl.State.JUMPING;
                }

                this.entity.setMovementSpeed(g);
                this.entity.setForwardSpeed((float) (this.forwardMovement*speed));
                this.entity.setSidewaysSpeed((float) (this.sidewaysMovement*speed));
                this.state = MoveControl.State.WAIT;
            } else if (this.state == MoveControl.State.MOVE_TO) {
                this.state = MoveControl.State.WAIT;
                double d = this.targetX - this.entity.getX();
                double e = this.targetZ - this.entity.getZ();
                double o = this.targetY - this.entity.getY();
                double p = d * d + o * o + e * e;
                if (p < 2.500000277905201E-7) {
                    this.entity.setForwardSpeed(0.0F);
                    return;
                }

                n = (float)(MathHelper.atan2(e, d) * 57.2957763671875) - 90.0F;
                this.entity.setYaw(this.wrapDegrees(this.entity.getYaw(), n, 90.0F));
                this.entity.setMovementSpeed((float)(this.speed * this.entity.getMovementSpeed()));
                BlockPos blockPos = this.entity.getBlockPos();
                BlockState blockState = this.entity.getWorld().getBlockState(blockPos);
                VoxelShape voxelShape = blockState.getCollisionShape(this.entity.getWorld(), blockPos);
                if (o > 0 && d * d + e * e < (double)Math.max(1.0F, this.entity.getWidth()) || !voxelShape.isEmpty() && this.entity.getY() < voxelShape.getMax(Direction.Axis.Y) + (double)blockPos.getY() && !blockState.isIn(BlockTags.DOORS) && !blockState.isIn(BlockTags.FENCES)) {
                    this.entity.getJumpControl().setActive();
                    this.state = MoveControl.State.JUMPING;
                }
            } else if (this.state == MoveControl.State.JUMPING) {
                this.entity.setMovementSpeed((float)(this.speed * this.entity.getMovementSpeed()));

                if (this.entity.isOnGround()) {
                    this.state = MoveControl.State.WAIT;
                }
            } else {
                this.entity.setForwardSpeed(0.0F);
            }

        }
        private boolean isPosWalkable(float x, float z) {
            EntityNavigation entityNavigation = this.entity.getNavigation();
            if (entityNavigation != null) {
                PathNodeMaker pathNodeMaker = entityNavigation.getNodeMaker();
                if (pathNodeMaker != null && pathNodeMaker.getDefaultNodeType(this.entity, BlockPos.ofFloored(this.entity.getX() + (double)x, (double)this.entity.getBlockY(), this.entity.getZ() + (double)z)) != PathNodeType.WALKABLE) {
                    return false;
                }
            }

            return true;
        }

        public void strafeTo(float forward, float sideways,float speed) {
            super.strafeTo(forward, sideways);
            this.speed = speed;

        }

        @Override
        public double getSpeed() {
            return super.getSpeed();
        }

        public boolean isStrafing(){
            return this.state.equals(State.STRAFE);
        }

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
            if (this.lookAtTimer > 0) {

                this.getTargetYaw().ifPresent((yaw) -> {
                    this.entity.setHeadYaw(this.changeAngle(this.entity.headYaw, yaw, this.maxYawChange));
                    this.entity.prevHeadYaw = this.entity.headYaw;
                });
                this.getTargetPitch().ifPresent((pitch) -> {
                    this.entity.setPitch(this.changeAngle(this.entity.getPitch(), pitch, this.maxPitchChange));
                    this.entity.prevPitch = this.entity.getPitch();

                });  } else {
                this.entity.headYaw = this.changeAngle(this.entity.headYaw, this.entity.bodyYaw, 10.0F);
            }

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
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return !this.isTamed();
    }


    @Override
    public void checkDespawn() {
        super.checkDespawn();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return instanceCache;
    }
}
