package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.AI.MinibossRevengeGoal;
import com.cleannrooster.rpg_minibosses.entity.AI.UniversalAngerGoalMiniboss;
import com.google.common.base.Predicates;
import mod.azure.azurelib.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.util.AzureLibUtil;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.spell_engine.api.effect.Synchronized;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.utils.SoundHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MinibossEntity extends PatrolEntity implements GeoEntity, Angerable {
    protected MinibossEntity(EntityType<? extends PatrolEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.lookControl = new MinibossLookControl(this);

    }

    protected MinibossEntity(EntityType<? extends PatrolEntity> entityType, World world, float spawnCoeff) {
        super(entityType, world);
        this.experiencePoints = 100;
        this.spawnCoeff = spawnCoeff;

        this.lookControl = new MinibossLookControl(this);
    }
    public List<String> NAMES = List.of(
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.1",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.2",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.3",
            ((TranslatableTextContent)this.getType().getName().getContent()).getKey()+".name.4");
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(HAS_ROLLED, false);
        this.dataTracker.startTracking(LESSER, false);
        this.dataTracker.startTracking(NAME, -1);
        this.dataTracker.startTracking(DOWN, false);
        this.dataTracker.startTracking(INDICATOR, 40);

    }


    @Override
    public Text getName() {
        if(this.getDataTracker().get(NAME) != -1){
            return Text.translatable(this.NAMES.get(this.getDataTracker().get(NAME)));
        }
        return super.getName();
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
    public static final RawAnimation WALK2H = RawAnimation.begin().thenLoop("animation.unknown.walk_2h");

    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("animation.mob.idle");
    public static final RawAnimation IDLE2H = RawAnimation.begin().thenPlay("animation.unknown.idle_2h");
    public static final RawAnimation DOWNANIM = RawAnimation.begin().thenPlayAndHold("animation.generic.down");

    public float spawnCoeff = 1;
    public AnimatableInstanceCache instanceCache = AzureLibUtil.createInstanceCache(this);

    public EquipmentSlot getPreferredMinibossEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.MAINHAND;
    }

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
            SoundHelper.playSoundEvent(this.getWorld(), this, RPGMinibosses.ANTICIPATION_SOUND);
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
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);

    }
    public void generateAndEquipLoot(EquipmentSlot slot, int attempts){
        for(int i = 0; i < attempts; i++) {
            Identifier registryKey = EntityType.ENDER_DRAGON.getLootTableId();
            LootTable lootTable = this.getWorld().getServer().getLootManager().getLootTable(registryKey);
            LootContextParameterSet.Builder builder = (new LootContextParameterSet.Builder((ServerWorld) this.getWorld())).add(LootContextParameters.THIS_ENTITY, this).add(LootContextParameters.ORIGIN, this.getPos()).add(LootContextParameters.DAMAGE_SOURCE, this.getDamageSources().generic());


            LootContextParameterSet lootContextParameterSet = builder.build(LootContextTypes.ENTITY);
            lootTable.generateLoot(lootContextParameterSet, this.getLootTableSeed(), (itemStack -> checkAndEquipLoot(itemStack, slot)));
            if(this.hasStackEquipped(slot)){
                break;
            };

        }
        for(int i = 0; i < attempts; i++) {
            Identifier registryKey = EntityType.ENDER_DRAGON.getLootTableId();
            LootTable lootTable = this.getWorld().getServer().getLootManager().getLootTable(registryKey);
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

    @Override
    public void onDeath(DamageSource damageSource) {
        if(!damageSource.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !this.getDataTracker().get(DOWN) && this.getWorld() instanceof ServerWorld){
            this.setHealth(0.01F);
            (this).triggerAnim("down","down");

            this.getDataTracker().set(DOWN,true);
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE ,100,99,false,false));
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
            /*if(!skipMainHand()) {
                this.generateAndEquipLoot(EquipmentSlot.MAINHAND, 8);
            }
            if(!skipOffHand()) {
                this.generateAndEquipLoot(EquipmentSlot.OFFHAND, 8);
            }*/
            if(getMainHandStack().isEmpty()){
                this.equipStack(EquipmentSlot.MAINHAND,this.getMainWeapon());
            }
            this.getDataTracker().set(HAS_ROLLED,true);
        }
        super.tick();
        if(this.getWorld() instanceof ServerWorld){
            this.tickIndicator();
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
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if(!this.notPetrified()) {
            for(MinibossEntity boss : this.getWorld().getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class),this.getBoundingBox().expand(8), Predicates.alwaysTrue())){
                if(!boss.notPetrified()) {
                    boss.playIntro(player);

                }
            }
            playIntro(player);
            return ActionResult.SUCCESS;

        }

        return ActionResult.PASS;
    }
    public String introTranslation(){
        return "text.rpg-minibosses.petrified";
    }
    public boolean notPetrified(){
       return  Synchronized.effectsOf(this).stream().noneMatch(effect -> effect.effect() == this.getIntroEffect());
    }
    @Override
    public boolean isInvulnerable() {
        return  Synchronized.effectsOf(this).stream().noneMatch(effect -> effect.effect() == this.getIntroEffect()) && super.isInvulnerable();
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
        return  (  Synchronized.effectsOf(this).stream().noneMatch(effect -> effect.effect() == this.getIntroEffect()) && super.damage(source, amount));
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
            ParticleHelper.sendBatches(this, SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
            SoundHelper.playSound(this.getWorld(), this, SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.sound);
        }
    }
    public int delay(){
        return 1;
    }
    public void applyIntroEffect(){
        this.addStatusEffect(new StatusEffectInstance(getIntroEffect(),-1,2,false,false));
    }
    public StatusEffect getIntroEffect(){
        return Effects.PETRIFIED.effect;
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
    private PlayState predicate2(AnimationState<MinibossEntity> state) {
        if(state.isMoving()){
            if(this.isTwoHand()){
                return state.setAndContinue(WALK2H);


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

    @Nullable
    public UUID getAngryAt() {
        return this.angryAt;
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
            this.maxYawChange = maxYawChange;
            this.maxPitchChange = maxPitchChange;
            this.lookAtTimer = 2;
        }

        public void tick() {
            if (this.shouldStayHorizontal()) {
                this.entity.setPitch(0.0F);
            }


            if (this.lookAtTimer > 0) {
                --this.lookAtTimer;
                this.getTargetYaw().ifPresent((yaw) -> {
                    this.entity.headYaw = this.changeAngle(this.entity.headYaw, yaw, this.maxYawChange);
                });
                this.getTargetPitch().ifPresent((pitch) -> {
                    this.entity.setPitch(this.changeAngle(this.entity.getPitch(), pitch, this.maxPitchChange));
                });
            } else {
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
