package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.google.common.base.Predicates;
import me.shedaniel.math.Color;
import mod.azure.azurelib.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.util.AzureLibUtil;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.*;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import net.spell_engine.api.effect.Synchronized;
import net.spell_engine.api.spell.ParticleBatch;
import net.spell_engine.api.spell.Sound;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellInfo;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.internals.WorldScheduler;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.particle.Particles;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.cleannrooster.rpg_minibosses.entity.TemplarEntity.raycastObstacleFree;
import static java.lang.Math.*;
import static net.spell_engine.particle.ParticleHelper.sendBatches;
import static net.spell_power.api.SpellSchools.*;

public class MagusPrimeEntity extends PatrolEntity implements GeoEntity {
    private int arctic;

    public MagusPrimeEntity(EntityType<? extends PatrolEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = (ServerBossBar)(new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS)).setDarkenSky(true);
        this.experiencePoints = 500;

        this.lookControl = new MinibossLookControl(this);
    }
    public static final TrackedData<Boolean> CASTINGBOOL;

    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.magus.idle");
    public static final RawAnimation IDLE2 = RawAnimation.begin().thenPlay("animation.magus.idle2");

    public static final RawAnimation GLOVE = RawAnimation.begin().thenPlay("animation.magus.glovepull");
    public static final RawAnimation DASH = RawAnimation.begin().thenPlay("animation.magus.dashforward");
    public static final RawAnimation CAST_QUICK = RawAnimation.begin().thenPlay("animation.magus.cast.quick");

    public static final RawAnimation CASTING = RawAnimation.begin().thenPlay("animation.magus.casting");
    public static final RawAnimation INTRO = RawAnimation.begin().thenPlay("animation.magus.intro");

    public AnimatableInstanceCache instanceCache = AzureLibUtil.createInstanceCache(this);
    public void playBoom(){
        this.playSound(RPGMinibosses.ANTICIPATION_SOUND,1,1);
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
                new AnimationController<>(this, "castquick", event -> PlayState.CONTINUE)
                        .triggerableAnim("castquick", CAST_QUICK));
        animationData.add(
                new AnimationController<>(this, "casting", event -> PlayState.CONTINUE)
                        .triggerableAnim("casting", CASTING));
        animationData.add(
                new AnimationController<>(this, "intro", event -> PlayState.CONTINUE)
                        .triggerableAnim("intro", INTRO));
    }

    private PlayState predicate2(AnimationState<MagusPrimeEntity> state) {

        return state.setAndContinue(IDLE);

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
        ARCTICARMORPARTICLES = new ParticleBatch(Particles.snowflake.id.toString(), ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,50,0.1F,0.1F,0,0,20,false);
        SHIELDPARTICLES_BLUE = new ParticleBatch(Particles.snowflake.id.toString(), ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,50,0.1F,0.1F,0,0,20,false);
        SHIELDPARTICLES_RED = new ParticleBatch(Particles.flame.id.toString(), ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,50,0.1F,0.1F,0,0,20,false);
        SHIELDPARTICLES_PURPLE = new ParticleBatch(Particles.arcane_spell.id.toString(), ParticleBatch.Shape.SPHERE, ParticleBatch.Origin.FEET,null,0,0,50,0.1F,0.1F,0,0,20,false);

        modes.addAll(List.of("PROJECTILE","NOVA"));
        CASTINGBOOL = DataTracker.registerData(MagusPrimeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        NOVA.addAll(List.of(
                Identifier.of(RPGMinibosses.MOD_ID,"fire_nova"),
                Identifier.of(RPGMinibosses.MOD_ID,"arctic_armor")
                ));
        LONG_NOVA.addAll(List.of(
                Identifier.of(RPGMinibosses.MOD_ID,"phoenix_nova"),
                Identifier.of(RPGMinibosses.MOD_ID,"arcane_nova"),
                Identifier.of(RPGMinibosses.MOD_ID,"deathchill_nova")
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
                Identifier.of(RPGMinibosses.MOD_ID,"ice_chunk")));
        INDICATOR = DataTracker.registerData(MagusPrimeEntity.class, TrackedDataHandlerRegistry.INTEGER);

    }
    public SpellSchool spellSchool = SpellSchools.ARCANE;
    public SpellSchool getSpellSchool(){
        return spellSchool;
    }
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CASTINGBOOL, false);
        this.dataTracker.startTracking(INDICATOR, 40);


    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(source.getAttacker() != null){
            if(source.getAttacker()  instanceof ServerPlayerEntity player){
                this.bossBar.addPlayer(player);

                if(!source.isOf(DamageTypes.THORNS) &&  this.hasStatusEffect(Effects.ARCTICARMOR.effect) && this.distanceTo(player) < 4 && !source.isIndirect() && arctic >= 10){
                    arctic = 0;

                    Spell spell = SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID,"ice_bolt"));

                    SpellHelper.performImpacts(this.getWorld(),this,player,player,new SpellInfo(spell,Identifier.of(RPGMinibosses.MOD_ID,"ice_bolt")),
                            new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(FROST, this)).position(this.getPos()),false);
                }
            }


        }
        if(source.isOf(DamageTypes.FALL)){
            return false;
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

            if(this.hasStatusEffect(Effects.ARCTICARMOR.effect)){
                sendBatches(this, new ParticleBatch[]{
                    ARCTICARMORPARTICLES
                });
            }
            if(this.hasStatusEffect(Effects.MAGUS_BARRIER.effect)){
                if(this.getSpellSchool().equals(FIRE)) {
                    sendBatches(this, new ParticleBatch[]{
                            SHIELDPARTICLES_RED
                    });
                } else if (this.getSpellSchool().equals(FROST)) {

                    sendBatches(this, new ParticleBatch[]{
                            SHIELDPARTICLES_BLUE
                    });
                }
                else{
                        sendBatches(this, new ParticleBatch[]{
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
                            this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.effect,-1,0,false,false));
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
            sendBatches(this, SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
            SoundHelper.playSound(this.getWorld(), this, SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.sound);
        }
    }
    public int delay(){
        return 1;
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
            SoundHelper.playSound(this.getWorld(), this, new Sound("spell_engine:generic_fire_release"));

            for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Target.Area(), null)) {
                SpellHelper.performImpacts(this.getWorld(), this, entity, this, spellReference.get(),
                        spell.impacts, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

            }
            Spell spell = SpellRegistry.getSpell(id);
            Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.getSpell(id);
            ParticleHelper.sendBatches(this, spell.release.particles);
        }


    }*/
    public void performSpell(String string,String string2){
        Spell spell = null;
        if(string.equals("short")) {
            if(string2.equals("projectile")) {
                Identifier id = SHORTCASTPROJECTILE.get(this.getRandom().nextInt(SHORTCASTPROJECTILE.size()));

                SoundHelper.playSound(this.getWorld(), this, new Sound("spell_engine:generic_fire_release"));

                 spell = SpellRegistry.getSpell(id);
                SpellHelper.shootProjectile(this.getWorld(), this, this.getTarget(), new SpellInfo(spell,id),
                        new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

                sendBatches(this, spell.release.particles);
            }
            if(string2.equals("nova")) {
                Identifier id = NOVA.get(this.getRandom().nextInt(NOVA.size()));
                 spell = SpellRegistry.getSpell(id);
                SoundHelper.playSound(this.getWorld(),this, new Sound("spell_engine:generic_fire_release"));

                for(Entity entity : TargetHelper.targetsFromArea(this,6,new Spell.Release.Target.Area(), null)) {
                    SpellHelper.performImpacts(this.getWorld(), this, entity, this, new SpellInfo(spell,id),
                            new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE,this)).position(this.getPos()));

                }
                sendBatches(this,spell.release.particles);
            }

        }
        if(string.equals("long")) {
            Identifier id = Identifier.of("","");
            Optional<RegistryEntry.Reference<Spell>> spellReference = null;
            if (string2.equals("projectile")) {
                 id = LONGCASTPROJECTILE.get(this.getRandom().nextInt(LONGCASTPROJECTILE.size()));


                SoundHelper.playSound(this.getWorld(), this, new Sound("spell_engine:generic_fire_release"));

                spell = SpellRegistry.getSpell(id);
            }
            
            if (string2.equals("nova")) {
                 id = LONG_NOVA.get(this.getRandom().nextInt(LONG_NOVA.size()));

                spell = SpellRegistry.getSpell(id);
                if(spell.school.equals(ARCANE)){
                    this.addStatusEffect(new StatusEffectInstance(ARCANE.boostEffect,160,3,true,true));
                }
                else if(spell.school.equals(FIRE)){
                    this.addStatusEffect(new StatusEffectInstance(Effects.FEATHER.effect,80,7,false,false));

                }
                else if(spell.school.equals(FROST)){
                    this.addStatusEffect(new StatusEffectInstance(Effects.ARCTICARMOR.effect,160,0,false,false));

                }
            }

            Identifier idShockWave = SHOCKWAVES.get(this.getRandom().nextInt(SHOCKWAVES.size()));

            Spell spellShockwave = SpellRegistry.getSpell(idShockWave);

            final Spell finalSpell = spell;
            Optional<RegistryEntry.Reference<Spell>> finalSpellReference = spellReference;
            List<PlayerEntity> players = this.getWorld().getPlayers(TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(32));
            players.forEach(player -> {

                player.sendMessage(Text.translatable("Barrier change / Only " + finalSpell.school.id.getPath().toUpperCase() + " damages."), true);
            });


            Identifier finalId = id;
            ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                    if (this.getTarget() != null) {
                        if (string2.equals("projectile")) {

                            SoundHelper.playSound(this.getWorld(), this, new Sound("spell_engine:generic_fire_release"));


                            SpellHelper.shootProjectile(this.getWorld(), this, this.getTarget(), new SpellInfo(finalSpell, finalId),
                                    new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

                            sendBatches(this, finalSpell.release.particles);
                        }
                        if (string2.equals("nova")) {

                            SoundHelper.playSound(this.getWorld(), this, new Sound("spell_engine:generic_fire_release"));

                            for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Release.Target.Area(), null)) {
                                SpellHelper.performImpacts(this.getWorld(), this, entity, this, new SpellInfo(finalSpell, finalId),
                                         new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

                            }
                            sendBatches(this, finalSpell.release.particles);

                        }

                    }
                    if (finalSpell != null && finalSpell.school != null) {
                        this.spellSchool = finalSpell.school;


                    }
                    this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.effect, -1, 0));

                });
        }

    }
    public static final TrackedData<Integer> INDICATOR ;

    public static ParticleBatch ARCTICARMORPARTICLES;
    public static ParticleBatch SHIELDPARTICLES_PURPLE;
    public static ParticleBatch SHIELDPARTICLES_BLUE;
    public static ParticleBatch SHIELDPARTICLES_RED;


    @Override
    protected void mobTick() {

        if(this.age < 30){
            return;
        }
        if(this.getTarget() != null) {
            this.getLookControl().lookAt(this.getTarget());
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,this.getTarget().getEyePos());
        }
        if(!this.getWorld().isClient() && this.getHealth()/this.getMaxHealth() < 0.25F && darkmatter > 400 && !this.performing && this.getTarget() != null ) {
            this.resetIndicator();

            ((WorldScheduler) this.getWorld()).schedule(10, () -> {

                (this).triggerAnim("casting", "casting");
                this.getDataTracker().set(CASTINGBOOL, true);
                this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,1,1);
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
                                    player.addStatusEffect(new StatusEffectInstance(Effects.DARK_MATTER.effect, 200, 0));


                                }
                                OrbEntity orb = new OrbEntity(RPGMinibosses.ORBENTITY, this.getWorld());
                                orb.setOwner(this);
                                orb.setPosition(this.getTarget().getPos());
                                this.getWorld().spawnEntity(orb);

                                this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.effect, -1, 0));
                            }

                        }
                );

            });
            this.casting_timer = 0;
            this.quickcast_timer -= 80;
            this.darkmatter = 0;
            this.performing = true;
        }
        if(!this.getWorld().isClient() && casting_timer > 120 && !this.performing && this.getTarget() != null ) {
            this.resetIndicator();

            ((WorldScheduler) this.getWorld()).schedule(10, () -> {

                (this).triggerAnim("casting", "casting");
                this.getDataTracker().set(CASTINGBOOL, true);
                this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,1,1);
                ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);
                String delivery = this.getTarget().distanceTo(this) < 4 ? "nova" : "projectile";
                this.performSpell("long", delivery);

                ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                            this.performing = false;
                            this.getDataTracker().set(CASTINGBOOL, false);


                        }
                );

            });
            this.casting_timer = 0;
            this.quickcast_timer -= 80;

            this.performing = true;
        }
        if(!this.getWorld().isClient() && quickcast_timer > 80 && !this.performing && this.getTarget() != null ) {
            (this).triggerAnim("castquick","castquick");
            this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON,1,1);

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
        if(!this.getWorld().isClient() && this.getRandom().nextFloat() < 0.01F && this.getTarget() != null && !this.performing){
            (this).triggerAnim("idle2","idle2");

            ((WorldScheduler) this.getWorld()).schedule(60, () -> {
                        this.performing = false;

                    }
            );
            this.performing = true;

        }
        if(!this.getWorld().isClient() && this.getRandom().nextFloat() < 0.01F &&  this.getTarget() != null && !this.performing){
            (this).triggerAnim("glove","glove");

            ((WorldScheduler) this.getWorld()).schedule(60, () -> {
                        this.performing = false;

                    }
            );
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
        super.mobTick();
        if(this.getTarget() != null) {
            this.getLookControl().lookAt(this.getTarget());
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,this.getTarget().getEyePos());

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

    @Override
    protected void applyDamage(DamageSource source, float amount) {
        if(source.getAttacker() instanceof PlayerEntity player  ) {
            SpellSchool magicSchool = this.getSpellSchool();
            if (magicSchool.equals(FROST) && SpellPower.getSpellPower(magicSchool,player).baseValue() > 4) {
                this.removeStatusEffect(Effects.MAGUS_BARRIER.effect);
            } else if (magicSchool.equals(FIRE) && SpellPower.getSpellPower(magicSchool,player).baseValue() > 4) {
                this.removeStatusEffect(Effects.MAGUS_BARRIER.effect);

            } else if (magicSchool.equals(ARCANE) && SpellPower.getSpellPower(magicSchool,player).baseValue() > 4) {
                this.removeStatusEffect(Effects.MAGUS_BARRIER.effect);

            }
            else if (magicSchool.equals(SOUL) && SpellPower.getSpellPower(magicSchool,player).baseValue() > 4) {
                this.removeStatusEffect(Effects.MAGUS_BARRIER.effect);

            }
        }
        if(this.getStatusEffect(Effects.MAGUS_BARRIER.effect) != null){
            amount *= 0.05F;
        }

        super.applyDamage(source, amount);
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

}
