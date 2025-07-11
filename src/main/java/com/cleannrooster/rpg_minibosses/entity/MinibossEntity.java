package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.AI.MinibossRevengeGoal;
import com.cleannrooster.rpg_minibosses.entity.AI.UniversalAngerGoalMiniboss;
import com.google.common.base.Predicates;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.spell_engine.api.effect.Synchronized;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.utils.SoundHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.Math.max;

public class MinibossEntity extends PatrolEntity implements GeoEntity, Angerable {
    protected MinibossEntity(EntityType<? extends PatrolEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.lookControl = new MinibossLookControl(this);
        this.moveControl = new MinibossMoveConrol(this);
    }



    protected MinibossEntity(EntityType<? extends PatrolEntity> entityType, World world, float spawnCoeff) {
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

    }


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
            yaw = MathHelper.clamp(yaw,pathAwareEntity.headYaw -70, pathAwareEntity.headYaw+70);



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
    private boolean isLesser(){
        return this.getDataTracker().get(MinibossEntity.LESSER);

    }
    private static final TrackedData<Boolean> HAS_ROLLED ;
    public static final TrackedData<Boolean> DOWN;

    public static final TrackedData<Boolean> LESSER ;
    public static final TrackedData<Integer> NAME ;

    public static final TrackedData<Integer> INDICATOR ;

    static{
        HAS_ROLLED = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        LESSER = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        NAME = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);
        DOWN = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        INDICATOR = DataTracker.registerData(MinibossEntity.class, TrackedDataHandlerRegistry.INTEGER);

    }
    public List<Item> bonusList;

    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.unknown.walk");
    public static final RawAnimation WALK_B = RawAnimation.begin().thenLoop("animation.unknown.walk_backwards");
    public static final RawAnimation WALK_B_2h = RawAnimation.begin().thenLoop("animation.unknown.walk_backwards2_2h");
    public static final RawAnimation WALK_B_T = RawAnimation.begin().thenLoop("animation.unknown.walk_backwards_transition").thenPlay("animation.unknown.walk_backwards");
    public static final RawAnimation WALK_B_2h_T = RawAnimation.begin().thenPlay("animation.unknown.walk_backwards2_2h2_transition").thenLoop("animation.unknown.walk_backwards2_2h");

    public static final RawAnimation WALK2H = RawAnimation.begin().thenLoop("animation.unknown.walk_2h");

    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("animation.mob.idle");
    public static final RawAnimation IDLE2H = RawAnimation.begin().thenPlay("animation.unknown.idle_2h");
    public static final RawAnimation DOWNANIM = RawAnimation.begin().thenPlayAndHold("animation.generic.down");

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
        if(!damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !this.getDataTracker().get(DOWN) && this.getWorld() instanceof ServerWorld){
            this.setHealth(0.01F);
            (this).triggerAnim("down","down");

            this.getDataTracker().set(DOWN,true);
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE ,40,9,false,false));
            return;
        }

        if(this.getDataTracker().get(DOWN) && damageSource.getAttacker() instanceof ServerPlayerEntity player){
            player.getStatHandler().setStat(player,Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY),player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY))+5);
        }
        super.onDeath(damageSource);
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
        if(this.getWorld() instanceof ServerWorld && this.getDataTracker().get(DOWN)){
            this.playReleaseParticlesAndSound();
            this.discard();
            ((ServerPlayerEntity)player).getStatHandler().setStat(player,Stats.CUSTOM.getOrCreateStat(RPGMinibosses.BENEVOLENCE),((ServerPlayerEntity)player).getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.BENEVOLENCE))+4);

        }
        return super.interactAt(player, hitPos, hand);
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
        if(this.firstUpdate && !this.getWorld().isClient()){
            if(this.getDataTracker().get(DOWN)){
                (this).triggerAnim("down","down");

            }
            if(this.getDataTracker().get(NAME) == -1){
                if((this.isLesser() && RPGMinibossesEntities.config.lesserPetrify > this.getRandom().nextFloat()) || (!this.isLesser() && RPGMinibossesEntities.config.greaterPetrify > this.getRandom().nextFloat())) {
                    this.applyIntroEffect();
                }
                this.getDataTracker().set(NAME,this.getRandom().nextInt(4));
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
        if(this.getWorld().isClient() && !(this instanceof TemplarEntity)){
            setRotationFromVelocity(this);
        }

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

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if(!this.notPetrified()) {
            for(MinibossEntity boss : this.getWorld().getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class),this.getBoundingBox().expand(8), Predicates.alwaysTrue())){
                if(!boss.notPetrified()) {
                    boss.playIntro(player);

                }
            }
            playIntro(player);
            return ActionResult.SUCCESS_NO_ITEM_USED;

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
        if(this.getDataTracker().get(DOWN) && source.getAttacker() instanceof MinibossEntity){
            return false;
        }
        if(source.getAttacker() instanceof MinibossEntity entity && !RPGMinibossesEntities.config.betrayal){
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

    protected void initGoals() {
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(1, new SwimGoal(this));


        if(RPGMinibossesEntities.config.trueAnarchy) {
            this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true));
        }
        else{
            this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true, (player) ->player instanceof ServerPlayerEntity playerEntity
                    && playerEntity.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.INFAMY)) > 5 + playerEntity.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(RPGMinibosses.BENEVOLENCE))));

        }

        this.targetSelector.add(1, (new MinibossRevengeGoal(this, MinibossEntity.class).setGroupRevenge(new Class[0])));
        this.targetSelector.add(4, new UniversalAngerGoalMiniboss<>(this, true));


        this.initCustomGoals();
    }

    protected void initCustomGoals() {
    }
    public boolean isTwoHand(){
        return false;
    }




    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(),  0.05F, 0.8F);

        super.playStepSound(pos, state);
    }

    private PlayState predicate2(AnimationState<MinibossEntity> state) {
        if(state.isMoving()){

            if(this.isTwoHand()){
                if( this.getVelocity().length() > 0.06F && this.getVelocity().normalize().dotProduct(this.getRotationVector().normalize()) < -0.2 ){
                    return state.setAndContinue(WALK_B_2h_T);
                }
                return state.setAndContinue(WALK2H);


            }
            if(this.getVelocity().length() > 0.06F && this.getVelocity().normalize().dotProduct(this.getRotationVector().normalize()) < -0.2 ){
                return state.setAndContinue(WALK_B_T);
            }
            return state.setAndContinue(WALK);


        }
        if(this.isTwoHand()) {

            return state.setAndContinue(IDLE2H);
        }
        return state.setAndContinue(IDLE);

    }
    private int ageWhenTargetSet;

    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target == null) {
            this.ageWhenTargetSet = 0;
        } else {
            this.ageWhenTargetSet = this.age;
        }

    }



    private static final UniformIntProvider ANGER_TIME_RANGE;

    public int angerTime;

    private UUID angryAt;
    static{
        ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    }

    @Override
    public boolean saveNbt(NbtCompound nbt) {
        this.writeAngerToNbt( nbt);

        return super.saveNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.readAngerFromNbt(this.getWorld(), nbt);

        super.readNbt(nbt);
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
    public class MinibossMoveConrol extends MoveControl{

        public MinibossMoveConrol(MobEntity entity) {
            super(entity);
        }

        public void tick() {
            float n;
            if (this.state == MoveControl.State.STRAFE) {
                float f = (float)this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
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
                this.entity.setForwardSpeed(this.forwardMovement);
                this.entity.setSidewaysSpeed(this.sidewaysMovement);
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
                this.entity.setMovementSpeed((float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));
                BlockPos blockPos = this.entity.getBlockPos();
                BlockState blockState = this.entity.getWorld().getBlockState(blockPos);
                VoxelShape voxelShape = blockState.getCollisionShape(this.entity.getWorld(), blockPos);
                if (o > 0 && d * d + e * e < (double)Math.max(1.0F, this.entity.getWidth()) || !voxelShape.isEmpty() && this.entity.getY() < voxelShape.getMax(Direction.Axis.Y) + (double)blockPos.getY() && !blockState.isIn(BlockTags.DOORS) && !blockState.isIn(BlockTags.FENCES)) {
                    this.entity.getJumpControl().setActive();
                    this.state = MoveControl.State.JUMPING;
                }
            } else if (this.state == MoveControl.State.JUMPING) {
                this.entity.setMovementSpeed((float)(this.speed * this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)));

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
            this.lookAtTimer = 8;
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
        return true;
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
