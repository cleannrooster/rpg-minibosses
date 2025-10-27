package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.google.common.base.Predicates;
import me.shedaniel.math.Color;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.sblforked.api.core.behaviour.custom.look.LookAtAttackTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import net.spell_engine.api.effect.Synchronized;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_engine.fx.SpellEngineSounds;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.cleannrooster.rpg_minibosses.entity.TemplarEntity.raycastObstacleFree;
import static com.cleannrooster.rpg_minibosses.entity.TemplarEntity.sendBatches;
import static java.lang.Math.max;
import static net.spell_engine.internals.SpellHelper.lookupAndPerformAreaImpact;
import static net.spell_power.api.SpellSchools.*;

public class MagusPrimeEntity extends PathAwareEntity implements GeoEntity{
    private int arctic;

    public MagusPrimeEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = (ServerBossBar)(new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS)).setDarkenSky(true);
        this.experiencePoints = 500;
        this.moveControl = new MinibossMoveConrol(this);

        this.lookControl = new MinibossLookControl(this);
    }






    public static final TrackedData<Boolean> CASTINGBOOL;

    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.magus.idle");
    public static final RawAnimation IDLE_M = RawAnimation.begin().thenLoop("animation.magus.walk_1");

    public static final RawAnimation IDLE2 = RawAnimation.begin().thenPlay("animation.magus.idle2");

    public static final RawAnimation GLOVE = RawAnimation.begin().thenPlay("animation.magus.glovepull");
    public static final RawAnimation DASH = RawAnimation.begin().thenPlay("animation.magus.dashforward");
    public static final RawAnimation CAST_QUICK = RawAnimation.begin().thenPlay("animation.magus.cast.quick");

    public static final RawAnimation CASTING = RawAnimation.begin().thenPlay("animation.magus.casting");
    public static final RawAnimation CAST_QUICK_M = RawAnimation.begin().thenPlay("animation.magus.cast.quick2");

    public static final RawAnimation CASTING_M = RawAnimation.begin().thenPlay("animation.magus.casting2");
    public static final RawAnimation INTRO = RawAnimation.begin().thenPlay("animation.magus.intro");

    public AnimatableInstanceCache instanceCache = AzureLibUtil.createInstanceCache(this);
    public void playBoom(){
        this.playSound(RPGMinibosses.ANTICIPATION_SOUND);
    }
    public void resetIndicator(){
        this.getDataTracker().set(INDICATOR,0);
        this.playBoom();
    }
    public void tickIndicator(){
        this.getDataTracker().set(INDICATOR,this.getDataTracker().get(INDICATOR)+1);
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        animationData.add(new AnimationController<MagusPrimeEntity>(this,"walk",
                0,this::predicate2)
        );

        animationData.add(
                new AnimationController<>(this, "idle2", event -> PlayState.CONTINUE)
                        .triggerableAnim("idle2", IDLE2));
        animationData.add(
                new AnimationController<>(this, "glove", event -> PlayState.CONTINUE)
                        .triggerableAnim("glove", GLOVE));
        animationData.add(
                new AnimationController<>(this, "dash", event -> PlayState.CONTINUE)
                        .triggerableAnim("dash", DASH));
        animationData.add(
                new AnimationController<>(this, "castquickm", event -> PlayState.CONTINUE)
                        .triggerableAnim("castquickm", CAST_QUICK));
        animationData.add(
                new AnimationController<>(this, "castquick", event -> PlayState.CONTINUE)
                        .triggerableAnim("castquick", CAST_QUICK_M));
        animationData.add(
                new AnimationController<>(this, "casting", event -> PlayState.CONTINUE)
                        .triggerableAnim("casting", CASTING));
        animationData.add(
                new AnimationController<>(this, "castingm", event -> PlayState.CONTINUE)
                        .triggerableAnim("castingm", CASTING_M));
        animationData.add(
                new AnimationController<>(this, "intro", event -> PlayState.CONTINUE)
                        .triggerableAnim("intro", INTRO));
    }

    private PlayState predicate2(AnimationState<MagusPrimeEntity> state) {
        state.setControllerSpeed((float) (state.isMoving() ? this.getVelocity().length()/0.1F : 1F));

        if(state.isMoving()){
            return state.setAndContinue(IDLE_M);
        }
        return state.setAndContinue(IDLE);

    }
    private PlayState predicate3(AnimationState<MagusPrimeEntity> state) {
        if(state.isMoving()){
            state.setAndContinue(CASTING_M);
        }
        return state.setAndContinue(CASTING);

    }
    private PlayState predicate4(AnimationState<MagusPrimeEntity> state) {
        if(state.isMoving()){
            state.setAndContinue(CAST_QUICK_M);
        }
        return state.setAndContinue(CAST_QUICK);

    }
    @Override
    public int getSafeFallDistance() {
        return 100;
    }

    public static ArrayList<Identifier> SHOCKWAVES = new ArrayList<>();
    public static ArrayList<Identifier> LONG_NOVA = new ArrayList<>();

    public static ArrayList<Identifier> NOVA = new ArrayList<>();
    public static ArrayList<Identifier> LONGRANGE = new ArrayList<>();
    public static ArrayList<String> modes = new ArrayList<>();

    public int getIndicator(){
        return this.getDataTracker().get(INDICATOR);

    }
    public static ArrayList<Identifier> SHORTCASTPROJECTILE = new ArrayList<>();
    public static ArrayList<Identifier> LONGCASTPROJECTILE = new ArrayList<>();
    public static ArrayList<Identifier> CUSTOMSPELLS = new ArrayList<>();
    public static int rgba(int alpha, int red, int green, int blue) {
        return (red << 16) | (green << 8) | (blue) ;
    }
    static {
        ARCTICARMORPARTICLES = new ParticleBatch("spell_engine:area_effect_714", ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,1,0,0,0,0,0,false,FROST.color,2,true,1F);
        SHIELDPARTICLES_BLUE = new ParticleBatch(SpellEngineParticles.area_effect_658.id().toString(), ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,1,0,0,0,0,0,false, FROST.color, 2,true,1F);
        SHIELDPARTICLES_RED = new ParticleBatch(SpellEngineParticles.area_effect_480.id().toString(), ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,1,0,0,0,0,0,false, 		4284889343L,2,true,1F);
        SHIELDPARTICLES_PURPLE = new ParticleBatch(SpellEngineParticles.area_effect_293.id().toString(), ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,1,0,0,0,0,0,false, 4284940287L, 2,true,1F);

        modes.addAll(List.of("PROJECTILE","NOVA"));
        CASTINGBOOL = DataTracker.registerData(MagusPrimeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        NOVA.addAll(List.of(
                Identifier.of(RPGMinibosses.MOD_ID,"fire_nova"),
                Identifier.of(RPGMinibosses.MOD_ID,"arctic_armor")
                ));
        LONG_NOVA.addAll(List.of(
                Identifier.of(RPGMinibosses.MOD_ID,"phoenix_nova"),
                Identifier.of(RPGMinibosses.MOD_ID,"arcane_nova"),
                Identifier.of(RPGMinibosses.MOD_ID,"deathchill_nova"),
                Identifier.of(RPGMinibosses.MOD_ID,"frostferno"),
                Identifier.of(RPGMinibosses.MOD_ID,"self_immolate"),
                Identifier.of(RPGMinibosses.MOD_ID,"rain_of_fire"),

                Identifier.of(RPGMinibosses.MOD_ID,"supernova")
        ));

        SHOCKWAVES.addAll(List.of(
                Identifier.of(RPGMinibosses.MOD_ID,"lightning_fall"),
                Identifier.of(RPGMinibosses.MOD_ID,"soul_burst")

        ));
        SHORTCASTPROJECTILE.addAll(List.of(
                Identifier.of(RPGMinibosses.MOD_ID,"fireball"),
                Identifier.of(RPGMinibosses.MOD_ID,"arcane_projectile"),
                Identifier.of(RPGMinibosses.MOD_ID,"ice_bolt")
                ));
        LONGCASTPROJECTILE.addAll(List.of(
                Identifier.of(RPGMinibosses.MOD_ID,"greater_fireball"),
                Identifier.of(RPGMinibosses.MOD_ID,"amethyst_chunk"),
                Identifier.of(RPGMinibosses.MOD_ID,"ice_chunk"),
                Identifier.of(RPGMinibosses.MOD_ID,"plasma_blast")));

        INDICATOR = DataTracker.registerData(MagusPrimeEntity.class, TrackedDataHandlerRegistry.INTEGER);

    }
    public SpellSchool spellSchool = SpellSchools.ARCANE;
    public SpellSchool getSpellSchool(){
        return spellSchool;
    }
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CASTINGBOOL, false);
        builder.add(INDICATOR, 40);


    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(source.getAttacker() != null){
            if(source.getAttacker()  instanceof ServerPlayerEntity player){
                this.bossBar.addPlayer(player);

                if(!source.isOf(DamageTypes.THORNS) &&  this.hasStatusEffect(Effects.ARCTICARMOR.registryEntry) && this.distanceTo(player) < 4 && source.isDirect() && arctic >= 10){
                    arctic = 0;

                    Spell spell = SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID,"ice_bolt"));
                    Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID,"ice_bolt"));

                    boolean bool = SpellHelper.performImpacts(this.getWorld(),this,player,player,spellReference.get(),spell.impacts,
                            new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(FROST, this)).position(this.getPos()));
                }
            }


        }
        if(source.isOf(DamageTypes.FALL)){
            return false;
        }
        if(source.isOf(this.getSpellSchool().damageType) || source.isOf(SpellSchools.HEALING.damageType)){
            if(this.hasStatusEffect(Effects.MAGUS_BARRIER.registryEntry)){
                this.removeStatusEffect(Effects.MAGUS_BARRIER.registryEntry);
                this.playSound(SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY);
            }
        }
            return super.damage(source, amount);
    }
    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
    }

    public ItemStack getMainWeapon(){
        return ItemStack.EMPTY;

    }
    private final ServerBossBar bossBar;

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }
    public int thornstimer = 0;
    @Override
    public void tick() {
        if(this.age % 10 == 0 && !this.getWorld().isClient()){

            if(this.hasStatusEffect(Effects.ARCTICARMOR.registryEntry)){
                ParticleHelper.sendBatches(this, new ParticleBatch[]{
                    ARCTICARMORPARTICLES
                });
            }
            if(this.hasStatusEffect(Effects.MAGUS_BARRIER.registryEntry)){
                if(this.getSpellSchool().equals(FIRE)) {
                    ParticleHelper.sendBatches(this, new ParticleBatch[]{
                            SHIELDPARTICLES_RED
                    });
                } else if (this.getSpellSchool().equals(FROST)) {

                    ParticleHelper.sendBatches(this, new ParticleBatch[]{
                            SHIELDPARTICLES_BLUE
                    });
                }
                else{
                        ParticleHelper.sendBatches(this, new ParticleBatch[]{
                                SHIELDPARTICLES_PURPLE
                        });
                }
            }
        }

        if(this.firstUpdate) {
            if (!this.getWorld().isClient()) {
                (this).triggerAnim("intro", "intro");
                ((WorldScheduler) this.getWorld()).schedule(30, () -> {
                            this.performing = false;
                            this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.registryEntry,-1,0,false,false));
                        }
                );
                this.performing = true;
            }
        }
        if(this.getTarget() != null){
            if(this.getTarget() instanceof ServerPlayerEntity player){
                this.bossBar.addPlayer(player);
            }


        }
        if(this.bossBar != null){
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

            SpellSchool magicSchool = this.getSpellSchool();
            if (magicSchool.equals(ARCANE)) {
                this.bossBar.setColor(BossBar.Color.PURPLE);
            } else if (magicSchool.equals(FROST)) {
                this.bossBar.setColor(BossBar.Color.WHITE);
            } else if (magicSchool.equals(FIRE)) {
                this.bossBar.setColor(BossBar.Color.RED);
            }
        }
        this.arctic++;
        super.tick();
        if(!this.getWorld().isClient){
            tickIndicator();

        }
        if(this.getWorld().isClient){
            setRotationFromVelocity(this);

        }

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
 /*   public void performCustomSpell(Identifier id) {
        if(id.equals(Identifier.of(RPGMinibosses.MOD_ID,"arctic_armor"))){
            this.addStatusEffect(new StatusEffectInstance(Effects.ARCTICARMOR.registryEntry,160,0));
            SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));

            for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Target.Area(), null)) {
                boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spellReference.get(),
                        spell.impacts, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

            }
            Spell spell = SpellRegistry.from(this.getWorld()).get(id);
            Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);
            ParticleHelper.sendBatches(this, spell.release.particles);
        }


    }*/

    @Override
    protected float getJumpVelocity(float strength) {
        return 1.5F*super.getJumpVelocity(strength);
    }

    public void performSpell(String string, String string2){
        Spell spell = null;
        if(string.equals("short")) {
            if(string2.equals("projectile")) {
                if(this.getTarget() != null) {

                    Identifier id = SHORTCASTPROJECTILE.get(this.getRandom().nextInt(SHORTCASTPROJECTILE.size()));

                    SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
                    Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);

                    spell = SpellRegistry.from(this.getWorld()).get(id);
                    SpellHelper.shootProjectile(this.getWorld(), this, this.getTarget(), spellReference.get(),
                            new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(spellReference.get().value().school, this)).position(this.getPos()));

                    ParticleHelper.sendBatches(this, spell.release.particles);
                }
            }
            if(string2.equals("nova")) {
                Identifier id = NOVA.get(this.getRandom().nextInt(NOVA.size()));
                 spell = SpellRegistry.from(this.getWorld()).get(id);
                SoundHelper.playSound(this.getWorld(),this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
                Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);

                for(Entity entity : TargetHelper.targetsFromArea(this,6,new Spell.Target.Area(), null)) {
                    boolean bool = SpellHelper.performImpacts(this.getWorld(), this, entity, this, spellReference.get(),
                            spell.impacts,new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(spellReference.get().value().school,this)).position(this.getPos()));

                }
                ParticleHelper.sendBatches(this,spell.release.particles);
            }

        }
        if(string.equals("long")) {
            Optional<RegistryEntry.Reference<Spell>> spellReference = null;
            if (string2.equals("projectile")) {
                Identifier id = LONGCASTPROJECTILE.get(this.getRandom().nextInt(LONGCASTPROJECTILE.size()));

                spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);

                SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));

                spell = SpellRegistry.from(this.getWorld()).get(id);
            }
            if (string2.equals("nova")) {
                Identifier id = LONG_NOVA.get(this.getRandom().nextInt(LONG_NOVA.size()));

                spell = SpellRegistry.from(this.getWorld()).get(id);
                spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);
            }

            Identifier idShockWave = SHOCKWAVES.get(this.getRandom().nextInt(SHOCKWAVES.size()));

            Spell spellShockwave = SpellRegistry.from(this.getWorld()).get(idShockWave);
            Optional<RegistryEntry.Reference<Spell>> spellReferenceShockwave = SpellRegistry.from(this.getWorld()).getEntry(idShockWave);

            final Spell finalSpell = spell;
            Optional<RegistryEntry.Reference<Spell>> finalSpellReference = spellReference;
            List<PlayerEntity> players = this.getWorld().getPlayers(TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(32));
            players.forEach(player -> {

                player.sendMessage(Text.translatable("Barrier change / Only " + finalSpell.school.id.getPath().toUpperCase() + " damages."), true);
            });


                ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                    if (this.getTarget() != null) {
                        if (string2.equals("projectile")) {

                            SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));


                            SpellHelper.shootProjectile(this.getWorld(), this, this.getTarget(), finalSpellReference.get(),
                                    new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(finalSpellReference.get().value().school, this)).position(this.getPos()));

                            ParticleHelper.sendBatches(this, finalSpell.release.particles);
                        }
                        if (string2.equals("nova")) {

                            SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));

                            for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Target.Area(), null)) {
                                boolean bool =  SpellHelper.performImpacts(this.getWorld(), this, entity, this, finalSpellReference.get(),
                                        finalSpell.impacts, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(finalSpellReference.get().value().school, this)).position(this.getPos()));

                            }
                            ParticleHelper.sendBatches(this, finalSpell.release.particles);

                        }
                        if (this.getRandom().nextFloat() < 0.3F) {

                            for (int i = 0; i < 5; i++) {
                                ((WorldScheduler) this.getWorld()).schedule(4 * (i + 1), () -> {
                                            if (this.getTarget() != null) {

                                                SpellHelper.ImpactContext context = new SpellHelper.ImpactContext(1.0F, 1.0F, this.getTarget().getPos(), SpellPower.getSpellPower(SpellSchools.HEALING, this), SpellTarget.FocusMode.DIRECT, 0).position(this.getTarget().getPos());
                                                SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_HEALING_RELEASE.id()));
                                                Vec3d pos = this.getTarget().getBoundingBox().getCenter();
                                                ((WorldScheduler) this.getWorld()).schedule(25, () -> {

                                                            if(this.getTarget() != null) {
                                                                boolean bool = lookupAndPerformAreaImpact(spellReferenceShockwave.get().value().area_impact, spellReferenceShockwave.get(), this, this, this, spellReferenceShockwave.get().value().impacts, context, false);
                                                                
                                                            }

                                                        }

                                                );
                                            }
                                        }
                                );

                            }
                        }
                    }
                    if (finalSpell != null && finalSpell.school != null) {
                        this.spellSchool = finalSpell.school;


                    }
                    this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.registryEntry, -1, 0));

                });
        }

    }
    public static final TrackedData<Integer> INDICATOR ;

    public static ParticleBatch ARCTICARMORPARTICLES;
    public static ParticleBatch SHIELDPARTICLES_PURPLE;
    public static ParticleBatch SHIELDPARTICLES_BLUE;
    public static ParticleBatch SHIELDPARTICLES_RED;

    public class MinibossMoveConrol extends MoveControl {

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

                this.entity.setMovementSpeed(g);
                this.entity.setForwardSpeed(this.forwardMovement);
                this.entity.setSidewaysSpeed(this.sidewaysMovement);
                BlockPos blockPos = this.entity.getBlockPos();
                BlockState blockState = this.entity.getWorld().getBlockState(blockPos);
                VoxelShape voxelShape = blockState.getCollisionShape(this.entity.getWorld(), blockPos);

                this.state = MoveControl.State.WAIT;

                if (isOnGround() && (horizontalCollision || this.entity.getWorld().getBlockState(BlockPos.ofFloored(this.entity.getPos().add(0,0,0).add(this.entity.getMovement().subtract(0,this.entity.getMovement().getY(),0).multiply(20)))).isSolidBlock(this.entity.getWorld(),BlockPos.ofFloored(this.entity.getPos().add(0,0,0).add(this.entity.getMovement().subtract(0,this.entity.getMovement().getY(),0).multiply(20))))) ) {
                    this.entity.getJumpControl().setActive();
                    this.state = MoveControl.State.JUMPING;
                }
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

        public void strafeTo(float forward, float sideways, float speed) {
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
                    this.entity.setYaw((this.changeAngle(this.entity.getYaw(),yaw,this.maxYawChange)));
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
    protected void mobTick() {

        if(this.age < 30){
            return;
        }
        if(this.getTarget() != null) {
            if(this.distanceTo(this.getTarget())< 8){
                ((MinibossMoveConrol)this.getMoveControl()).strafeTo(-2, this.getTarget().getPos().subtract(this.getPos()).crossProduct(new Vec3d(0, 1, 0)).dotProduct(this.getRotationVector()) > 0 ? -0.6F : 0.6F,0.25F);
            }
        }
        if(!this.getWorld().isClient() && this.getHealth()/this.getMaxHealth() < 0.25F && darkmatter > 400 && !this.performing && this.getTarget() != null ) {
            this.resetIndicator();

            ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                if(this.getTarget() != null) {

                    (this).triggerAnim("casting", "casting");
                    this.getDataTracker().set(CASTINGBOOL, true);
                    this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK);
                    ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);
                    String delivery = this.getTarget().distanceTo(this) < 4 ? "nova" : "projectile";
                    List<PlayerEntity> players = this.getWorld().getPlayers(TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(32));
                    players.forEach(player -> {
                        player.sendMessage(Text.translatable("Barrier change / Only " + SOUL.id.getPath().toUpperCase() + " damages."), true);
                    });

                    ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                                if (this.getTarget() != null) {
                                    this.performing = false;
                                    this.getDataTracker().set(CASTINGBOOL, false);
                                    this.spellSchool = SOUL;
                                    for (ServerPlayerEntity player : PlayerLookup.tracking(this)) {
                                        player.addStatusEffect(new StatusEffectInstance(Effects.DARK_MATTER.registryEntry, 200, 0));


                                    }
                                    OrbEntity orb = new OrbEntity(RPGMinibosses.ORBENTITY, this.getWorld());
                                    orb.setOwner(this);
                                    orb.setPosition(this.getTarget().getPos());
                                    this.getWorld().spawnEntity(orb);

                                    this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.registryEntry, -1, 0));
                                }

                            }
                    );
                }

            });
            this.casting_timer = 0;
            this.quickcast_timer -= 80;
            this.darkmatter = 0;
            this.performing = true;
        }
        if(!this.getWorld().isClient() && casting_timer > 120 && !this.performing && this.getTarget() != null ) {
            this.resetIndicator();

            ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                if(this.getTarget() != null) {
                    if(this.moveControl.isMoving()){
                        (this).triggerAnim("castingm","castingm");

                    }
                    else {
                        (this).triggerAnim("casting", "casting");
                    }
                    this.getDataTracker().set(CASTINGBOOL, true);
                    this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK);
                    ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);
                    String delivery = this.getTarget().distanceTo(this) < 4 ? "nova" : "projectile";
                    this.performSpell("long", delivery);

                    ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                                this.performing = false;
                                this.getDataTracker().set(CASTINGBOOL, false);


                            }
                    );
                }

            });
            this.casting_timer = 0;
            this.quickcast_timer -= 80;

            this.performing = true;
        }
        if(!this.getWorld().isClient() && quickcast_timer > 80 && !this.performing && this.getTarget() != null ) {
            if(this.moveControl.isMoving()){
                (this).triggerAnim("castquickm","castquickm");

            }
            else {
                (this).triggerAnim("castquick", "castquick");
            }
            this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON);

            String delivery = this.getTarget().distanceTo(this) < 4 ? "nova" : "projectile";
            this.performSpell("short",delivery);
            ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                        this.performing = false;
                        this.casting_timer -= 20;


                    }
            );

            this.quickcast_timer = 0;
            this.casting_timer -= 20;

            this.performing = true;
        }


        if(!this.getWorld().isClient() && jumptimer > 200 && !this.performing && this.getTarget() != null  && this.distanceTo(this.getTarget()) < 4 ) {
            (this).triggerAnim("dash","dash");

            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

            Vec3d vec31 = new Vec3d(-this.getTarget().getX() + this.getX(), 0, -this.getTarget().getZ() + this.getZ());
            Vec3d vec3 = new Vec3d(vec31.normalize().x * 1, 0.75, vec31.normalize().z * 1);
            this.setPosition(this.getPos().add(0, 0.2, 0));
            this.setOnGround(false);
            this.setVelocity(vec3);
            this.jumptimer = 0;
            ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                        this.performing = false;

                    }
            );
            this.performing = true;

        }
        if(!this.getWorld().isClient() && dash_attack_timer > 240 && !this.performing && this.getTarget() != null &&  this.distanceTo(this.getTarget()) > 4) {
            (this).triggerAnim("dash","dash");
            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

            Vec3d vec31 = new Vec3d(this.getTarget().getX() - this.getX(), 0, this.getTarget().getZ() - this.getZ());
            Vec3d vec3 = new Vec3d(vec31.normalize().x * 1, 0.75, vec31.normalize().z * 1);
            this.setPosition(this.getPos().add(0, 0.2, 0));
            this.setOnGround(false);
            this.setVelocity(vec3.multiply(Math.min(1,this.distanceTo(this.getTarget())/6)));
            this.quickcast_timer += 40;
            this.casting_timer += 40;

            ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                        this.performing = false;

                    }
            );

            this.dash_attack_timer = 0;
            this.performing = true;
        }


        if(this.isSwimming() && this.age % 10 == 0){
            teleportRandomly();
        }

        if(!this.getWorld().isClient()) {
            dash_attack_timer++;
            quickcast_timer++;
            casting_timer++;
            jumptimer++;
            darkmatter++;

        }
        if(this.getTarget() != null) {
            this.getLookControl().lookAt(this.getTarget(),360,360);
        }

        super.mobTick();

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
    private boolean performing;

    public int dash_attack_timer;
    public int quickcast_timer;
    public int casting_timer;
    public int darkmatter;

    public int jumptimer;
    protected boolean teleportRandomly() {
        if (!this.getWorld().isClient() && this.isAlive()) {
            double d = this.getX() + (this.random.nextDouble() - 0.5) * 64.0;
            double e = this.getY() + (double)(this.random.nextInt(64) - 32);
            double f = this.getZ() + (this.random.nextDouble() - 0.5) * 64.0;
            return this.teleportTo(d, e, f);
        } else {
            return false;
        }
    }
    public boolean isSwimming() {
        return this.isTouchingWater() && this.getFluidHeight(FluidTags.WATER) > this.getSwimHeight() || this.isInLava();
    }
    private boolean teleportTo(double x, double y, double z) {
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);

        while(mutable.getY() > this.getWorld().getBottomY() && !this.getWorld().getBlockState(mutable).blocksMovement()) {
            mutable.move(Direction.DOWN);
        }

        BlockState blockState = this.getWorld().getBlockState(mutable);
        boolean bl = blockState.blocksMovement();
        boolean bl2 = blockState.getFluidState().isIn(FluidTags.WATER);
        if (bl && !bl2) {
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
    @Override
    public boolean isAiDisabled() {
        return !notPetrified() && super.isAiDisabled();
    }

    protected void initGoals() {


        this.targetSelector.add(1, (new RevengeGoal(this, new Class[0])).setGroupRevenge());



        this.initCustomGoals();
    }

    protected void initCustomGoals() {
    }
    public boolean isTwoHand(){
        return false;
    }




    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return instanceCache;
    }

}
