package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MagusPrimeAnimationProvider;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.google.common.base.Predicates;
import me.shedaniel.math.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
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
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.particle.ParticleTypes;
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
import net.spell_engine.api.spell.fx.ParticleGroup;
import net.spell_engine.api.spell.fx.ParticleGroupBuilder;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_engine.fx.SpellEngineSounds;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import net.spell_engine.internals.delivery.ProjectileLauncher;
import net.spell_engine.internals.delivery.CloudPlacer;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.cleannrooster.rpg_minibosses.entity.TemplarEntity.raycastObstacleFree;
import static com.cleannrooster.rpg_minibosses.entity.TemplarEntity.sendBatches;
import static java.lang.Math.max;
import static net.spell_engine.internals.impact.SpellImpacts.lookupAndPerformAreaImpact;
import static net.spell_power.api.SpellSchools.*;

public class MagusPrimeEntity extends PathAwareEntity {
    private int arctic;
    private double lastTeleportAngle = Double.NaN;

    public MagusPrimeEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = (ServerBossBar)(new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS)).setDarkenSky(true);
        this.experiencePoints = 500;
        this.moveControl = new MinibossMoveConrol(this);

        this.lookControl = new MinibossLookControl(this);
    }






    public static final TrackedData<Boolean> CASTINGBOOL;

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
    public static final Identifier DISDAIN_MODIFIER_ID = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "disdain_damage");
    public static final Identifier PHASE3_SPEED_ID = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "phase3_speed");
    public static final Identifier PHASE3_ATKSPEED_ID = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "phase3_atkspeed");
    public static final Identifier CONTEMPT_ATKSPEED_ID = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "contempt_atkspeed");
    public static final SpellSchool[] BARRIER_CYCLE = { ARCANE, FROST, FIRE };
    public static final int BASE_BARRIER_CYCLE_TICKS = 400;
    public static final int DOMINION_ACTIVATION_TICKS = 100;
    public static int rgba(int alpha, int red, int green, int blue) {
        return (red << 16) | (green << 8) | (blue) ;
    }
    static {
        ARCTICARMORPARTICLES = shieldAura("spell_engine:area_effect_714", FROST.color);
        SHIELDPARTICLES_BLUE = shieldAura(SpellEngineParticles.area_effect_658.id().toString(), FROST.color);
        SHIELDPARTICLES_RED = shieldAura(SpellEngineParticles.area_effect_480.id().toString(), 4284889343L);
        SHIELDPARTICLES_PURPLE = shieldAura(SpellEngineParticles.area_effect_293.id().toString(), 4284940287L);

        modes.addAll(List.of("PROJECTILE","NOVA"));
        CASTINGBOOL = DataTracker.registerData(MagusPrimeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        NOVA.addAll(List.of(
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"fire_nova"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"arctic_armor")
                ));
        LONG_NOVA.addAll(List.of(
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"phoenix_nova"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"arcane_nova"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"deathchill_nova"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"frostferno"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"self_immolate"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"rain_of_fire"),

                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"supernova")
        ));

        SHOCKWAVES.addAll(List.of(
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"lightning_fall"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"soul_burst")

        ));
        SHORTCASTPROJECTILE.addAll(List.of(
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"fire_volley"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"arcane_projectile"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"ice_bolt")
                ));
        LONGCASTPROJECTILE.addAll(List.of(
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"greater_fireball"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"amethyst_chunk"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"ice_chunk"),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"plasma_blast")));

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
        if (transitioning) return false;
        if(source.getAttacker() != null){
            if(source.getAttacker()  instanceof ServerPlayerEntity player){
                this.bossBar.addPlayer(player);

                if(!source.isOf(DamageTypes.THORNS) &&  this.hasStatusEffect(Effects.ARCTICARMOR.registryEntry) && this.distanceTo(player) < 4 && source.isDirect() && arctic >= 10){
                    arctic = 0;

                    Spell spell = SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"ice_bolt"));
                    Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"ice_bolt"));

                    boolean bool = SpellImpacts.performImpacts(this.getWorld(),this,player,player,spellReference.get(),spell.impacts,
                            new SpellExecution.ImpactContext().power(SpellPower.getSpellPower(FROST, this)).position(this.getPos()));
                }
            }


        }
        if(source.isOf(DamageTypes.FALL)){
            return false;
        }
        boolean isSpellDamage = source.isOf(FIRE.damageType) || source.isOf(FROST.damageType)
                || source.isOf(ARCANE.damageType) || source.isOf(LIGHTNING.damageType)
                || source.isOf(SOUL.damageType) || source.isOf(HEALING.damageType);
        if (this.hasStatusEffect(Effects.MAGUS_BARRIER.registryEntry)) {
            if (source.isOf(this.getSpellSchool().damageType) || source.isOf(HEALING.damageType)) {
                correctHitsThisCycle++;
                if (correctHitsThisCycle >= correctHitsRequired) {
                    this.removeStatusEffect(Effects.MAGUS_BARRIER.registryEntry);
                    playBarrierCue(BarrierCue.BREAK);
                    this.consecutiveWrongHits = 0;
                    this.correctHitsThisCycle = 0;
                    clearDominion();
                } else {
                    playBarrierCue(BarrierCue.CORRECT_HIT);
                }
            } else if (isSpellDamage ) {

                this.heal(5.0f + 3.0f * this.consecutiveWrongHits);
                this.consecutiveWrongHits++;
                this.empowerMultiplier = Math.min(3.0f, 1.0f + 0.3f * this.consecutiveWrongHits);
                if (!this.getWorld().isClient()) {
                    String particleId;
                    long color;
                    if (this.spellSchool.equals(FIRE)) {
                        particleId = SpellEngineParticles.area_effect_480.id().toString();
                        color = 4284889343L;
                    } else if (this.spellSchool.equals(FROST)) {
                        particleId = SpellEngineParticles.area_effect_658.id().toString();
                        color = FROST.color;
                    } else {
                        particleId = SpellEngineParticles.area_effect_293.id().toString();
                        color = 4284940287L;
                    }
                    var absorbBurst = ParticleGroupBuilder.of(particleId)
                            .color(color)
                            .scale(4)
                            .attached()
                            .playbackSpeed(1F / 2.5F)
                            .batch(b -> b.shape(ParticleGroup.Shape.SPHERE).count(20).speed(0F, 0.5F));
                    ParticleHelper.sendBatches(this, List.of(absorbBurst));
                    if (this.getWorld() instanceof ServerWorld sw) {
                        // Drawn inward, not blown outward: a ring of motes flying back into Magus is
                        // what tells the player their damage went the wrong way. Count 0 with an
                        // explicit delta makes each mote travel along that vector.
                        int motes = 12 + this.consecutiveWrongHits * 4;
                        for (int i = 0; i < motes; i++) {
                            double ang = i * (2 * Math.PI / motes);
                            double radius = 2.6;
                            sw.spawnParticles(ParticleTypes.WITCH,
                                    this.getX() + Math.cos(ang) * radius,
                                    this.getY() + 0.6 + this.getRandom().nextDouble() * 1.6,
                                    this.getZ() + Math.sin(ang) * radius,
                                    0, -Math.cos(ang) * 0.55, 0.06, -Math.sin(ang) * 0.55, 0.7);
                        }
                        sw.spawnParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + 1.8, this.getZ(),
                                2 + this.consecutiveWrongHits, 0.5, 0.3, 0.5, 0.1);
                    }
                    this.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                            1.5f, 0.7f + this.consecutiveWrongHits * 0.1f);
                    MagusPrimeAnimationProvider.BARRIER_ABSORB_COMMAND.sendForEntity(this);
                }
                return false;
            } else {
                amount *= 0.4f;
            }
        }
        boolean result = super.damage(source, amount);
        if (!this.getWorld().isClient() && result) {
            if (phase == 1 && this.getHealth() / this.getMaxHealth() < 0.60F) {
                startPhaseTransition(2);
            } else if (phase == 2 && this.getHealth() / this.getMaxHealth() < 0.25F) {
                startPhaseTransition(3);
            }
        }
        return result;
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

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        // Re-state the locomotion loop for a player arriving mid-fight rather than leaving them on
        // a T-pose until the next refresh tick.
        if (!this.getWorld().isClient()) {
            lastLocomotion = -1;
        }
    }
    public int thornstimer = 0;
    /** Last locomotion state dispatched: 1 walking, 0 standing, -1 nothing sent yet. */
    private int lastLocomotion = -1;
    @Override
    public void tick() {
        if (!this.getWorld().isClient()) {
            // Only dispatch on a state change, plus a slow refresh so a player who starts tracking
            // mid-fight still gets the loop. This used to fire every tick, which both spammed the
            // network and hid the moment Magus actually stops moving to cast.
            int locomotion = this.getVelocity().horizontalLengthSquared() > 0.0001 ? 1 : 0;
            if (locomotion != lastLocomotion || this.age % 40 == 0) {
                lastLocomotion = locomotion;
                (locomotion == 1 ? MagusPrimeAnimationProvider.WALK_COMMAND
                        : MagusPrimeAnimationProvider.IDLE_COMMAND).sendForEntity(this);
            }
        }
        if(this.age % 10 == 0 && !this.getWorld().isClient()){

            if(this.hasStatusEffect(Effects.ARCTICARMOR.registryEntry)){
                ParticleHelper.sendBatches(this, List.of(ARCTICARMORPARTICLES));
            }
            if(this.hasStatusEffect(Effects.MAGUS_BARRIER.registryEntry)){
                if(this.getSpellSchool().equals(FIRE)) {
                    ParticleHelper.sendBatches(this, List.of(SHIELDPARTICLES_RED));
                } else if (this.getSpellSchool().equals(FROST)) {
                    ParticleHelper.sendBatches(this, List.of(SHIELDPARTICLES_BLUE));
                }
                else{
                    ParticleHelper.sendBatches(this, List.of(SHIELDPARTICLES_PURPLE));
                }
            }
            // Phase ambience. Phase 1 is clean; phase 2 carries a steady arcane bleed; phase 3
            // sheds soul-fire, so the state Magus is in is readable from across the arena without
            // anyone having to count the boss bar's notches.
            if (this.getTarget() != null && phase >= 2 && this.getWorld() instanceof ServerWorld ambient) {
                if (phase == 2) {
                    ambient.spawnParticles(ParticleTypes.ENCHANT,
                            this.getX(), this.getBodyY(0.7), this.getZ(), 4, 0.5, 0.7, 0.5, 0.02);
                } else {
                    ambient.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            this.getX(), this.getBodyY(0.5), this.getZ(), 6, 0.45, 0.8, 0.45, 0.03);
                    ambient.spawnParticles(ParticleTypes.LARGE_SMOKE,
                            this.getX(), this.getBodyY(0.9), this.getZ(), 2, 0.4, 0.3, 0.4, 0.01);
                }
            }
        }

        if(this.firstUpdate) {
            if (!this.getWorld().isClient()) {
                MagusPrimeAnimationProvider.INTRO_COMMAND.sendForEntity(this);
                beginCast(30);
                ((WorldScheduler) this.getWorld()).schedule(30, () ->
                        this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.registryEntry,-1,0,false,false))
                );
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
            // The bar already carries the barrier school as its colour; the notch count carries the
            // phase, so both pieces of state Magus expects you to track are readable at a glance.
            BossBar.Style phaseStyle = phase == 1 ? BossBar.Style.PROGRESS
                    : phase == 2 ? BossBar.Style.NOTCHED_6 : BossBar.Style.NOTCHED_12;
            if (this.bossBar.getStyle() != phaseStyle) {
                this.bossBar.setStyle(phaseStyle);
            }
        }
        this.arctic++;
        super.tick();
        if(!this.getWorld().isClient){
            tickIndicator();

        }
        if (this.getWorld().isClient) {
            setRotationFromVelocity(this);
            tickHeadRotation();
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

    public void playReleaseParticlesAndSound(){
        if(!this.getWorld().isClient()) {
            ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "pound")).release.visuals.particles);
            SoundHelper.playSound(this.getWorld(), this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "pound")).release.sound);
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
        if(id.equals(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"arctic_armor"))){
            this.addStatusEffect(new StatusEffectInstance(Effects.ARCTICARMOR.registryEntry,160,0));
            SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));

            for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Target.Area(), null)) {
                boolean bool = SpellImpacts.performImpacts(this.getWorld(), this, entity, this, spellReference.get(),
                        spell.impacts, new SpellExecution.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.FIRE, this)).position(this.getPos()));

            }
            Spell spell = SpellRegistry.from(this.getWorld()).get(id);
            Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);
            ParticleHelper.sendBatches(this, spell.release.visuals.particles);
        }


    }*/

    @Override
    protected float getJumpVelocity(float strength) {
        return 1.5F*super.getJumpVelocity(strength);
    }

    public void performSpell(String string, String string2){
        Spell spell = null;
        if(string.equals("short")) {
            final float shortEmpower = this.empowerMultiplier;
            this.empowerMultiplier = 1.0f;
            if(string2.equals("projectile")) {
                if(this.getTarget() != null) {

                    Identifier id = SHORTCASTPROJECTILE.get(this.getRandom().nextInt(SHORTCASTPROJECTILE.size()));

                    SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
                    Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);

                    spell = SpellRegistry.from(this.getWorld()).get(id);
                    ProjectileLauncher.shootProjectile(this.getWorld(), this, this.getTarget(), spellReference.get(),
                            new SpellExecution.ImpactContext(shortEmpower, shortEmpower, this.getPos(), SpellPower.getSpellPower(spellReference.get().value().school, this), SpellTarget.FocusMode.DIRECT, 0));

                    ParticleHelper.sendBatches(this, spell.release.visuals.particles);
                }
            }
            if(string2.equals("nova")) {
                Identifier id = NOVA.get(this.getRandom().nextInt(NOVA.size()));
                 spell = SpellRegistry.from(this.getWorld()).get(id);
                SoundHelper.playSound(this.getWorld(),this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));
                Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);

                for(Entity entity : TargetHelper.targetsFromArea(this,6,new Spell.Target.Area(), null)) {
                    boolean bool = SpellImpacts.performImpacts(this.getWorld(), this, entity, this, spellReference.get(),
                            spell.impacts, new SpellExecution.ImpactContext(shortEmpower, shortEmpower, this.getPos(), SpellPower.getSpellPower(spellReference.get().value().school, this), SpellTarget.FocusMode.DIRECT, 0));

                }
                ParticleHelper.sendBatches(this,spell.release.visuals.particles);
            }

        }
        if(string.equals("long")) {
            Optional<RegistryEntry.Reference<Spell>> spellReference = null;
            Identifier id = null;
            if (string2.equals("projectile")) {
                 id = LONGCASTPROJECTILE.get(this.getRandom().nextInt(LONGCASTPROJECTILE.size()));

                spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);

                SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));

                spell = SpellRegistry.from(this.getWorld()).get(id);
            }
            if (string2.equals("nova")) {
                 id = LONG_NOVA.get(this.getRandom().nextInt(LONG_NOVA.size()));

                spell = SpellRegistry.from(this.getWorld()).get(id);
                spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);
            }

            Identifier idShockWave = SHOCKWAVES.get(this.getRandom().nextInt(SHOCKWAVES.size()));

            Spell spellShockwave = SpellRegistry.from(this.getWorld()).get(idShockWave);
            Optional<RegistryEntry.Reference<Spell>> spellReferenceShockwave = SpellRegistry.from(this.getWorld()).getEntry(idShockWave);

            final Spell finalSpell = spell;
            final Optional<RegistryEntry.Reference<Spell>> finalSpellReference = spellReference;
            final float capturedEmpower = this.empowerMultiplier;
            this.empowerMultiplier = 1.0f;
            ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                if (this.getTarget() != null) {

                    if (string2.equals("projectile")) {

                        SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));

                        ProjectileLauncher.shootProjectile(this.getWorld(), this, this.getTarget(), finalSpellReference.get(),
                                new SpellExecution.ImpactContext(capturedEmpower, capturedEmpower, this.getPos(), SpellPower.getSpellPower(finalSpellReference.get().value().school, this), SpellTarget.FocusMode.DIRECT, 0));

                        ParticleHelper.sendBatches(this, finalSpell.release.visuals.particles);
                    }
                    if (string2.equals("nova")) {

                        SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_FIRE_RELEASE.id()));

                        for (Entity entity : TargetHelper.targetsFromArea(this, 6, new Spell.Target.Area(), null)) {
                            boolean bool = SpellImpacts.performImpacts(this.getWorld(), this, entity, this, finalSpellReference.get(),
                                    finalSpell.impacts, new SpellExecution.ImpactContext(capturedEmpower, capturedEmpower, this.getPos(), SpellPower.getSpellPower(finalSpellReference.get().value().school, this), SpellTarget.FocusMode.DIRECT, 0));

                        }
                        ParticleHelper.sendBatches(this, finalSpell.release.visuals.particles);

                    }
                    if (this.getRandom().nextFloat() < 0.3F) {
                        // Ground-directed follow-up gets its own shape: the staff is driven down
                        // and forward, so the pulses that arrive under the player have a visible
                        // cause instead of appearing out of the heavy cast's recovery.
                        MagusPrimeAnimationProvider.SHOCKWAVE_COMMAND.sendForEntity(this);

                        for (int i = 0; i < 5; i++) {
                            ((WorldScheduler) this.getWorld()).schedule(4 * (i + 1), () -> {
                                        if (this.getTarget() != null) {

                                            SpellExecution.ImpactContext context = new SpellExecution.ImpactContext(1.0F, 1.0F, this.getTarget().getPos(), SpellPower.getSpellPower(SpellSchools.HEALING, this), SpellTarget.FocusMode.DIRECT, 0).position(this.getTarget().getPos());
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
            });
        }

    }
    /** True while the boss is mid-action; gates every ability so only one runs at a time. */
    public boolean isPerforming() {
        return this.age < this.performingUntil;
    }

    /**
     * Begins a locked action lasting {@code lockTicks}. The lock is an age-based deadline, so it
     * always auto-expires even if a scheduled effect callback is dropped (e.g. the target is lost
     * mid-cast). Every ability MUST start via this instead of setting the lock by hand — that is
     * what prevents the boss from ever locking out of acting. Effects are still scheduled by the
     * caller; they may be guarded freely, but they must never touch the lock.
     */
    private void beginCast(int lockTicks) {
        this.performingUntil = this.age + lockTicks;
        // Clear the client-side casting animation flag when the lock ends, unless a newer cast
        // has already taken over. Unconditional (no target guard) so it can never be stranded.
        ((WorldScheduler) this.getWorld()).schedule(lockTicks, () -> {
            if (!this.isPerforming()) {
                this.getDataTracker().set(CASTINGBOOL, false);
            }
        });
    }

    private void startPhaseTransition(int newPhase) {
        if (this.getWorld().isClient()) return;
        this.phase = newPhase;
        this.transitioning = true;
        beginCast(60);
        // A dedicated 60-tick transition pose rather than the spawn intro clip: contract, hold,
        // then throw the guard open on the beat the new phase's rules come into force.
        MagusPrimeAnimationProvider.PHASE_TRANSITION_COMMAND.sendForEntity(this);
        this.playSound(SoundEvents.ENTITY_EVOKER_CELEBRATE);
        if (this.getWorld() instanceof ServerWorld transitionWorld) {
            // Anticipation pulls inward for the first 36 ticks, then breaks outward on the open.
            for (int i = 0; i < 3; i++) {
                final int delay = i * 12;
                ((WorldScheduler) this.getWorld()).schedule(delay, () ->
                        transitionWorld.spawnParticles(ParticleTypes.ENCHANT,
                                this.getX(), this.getBodyY(0.6), this.getZ(), 30, 1.6, 1.2, 1.6, -0.6));
            }
            ((WorldScheduler) this.getWorld()).schedule(37, () -> {
                transitionWorld.spawnParticles(ParticleTypes.FLASH,
                        this.getX(), this.getBodyY(0.6), this.getZ(), 1, 0, 0, 0, 0);
                transitionWorld.spawnParticles(newPhase >= 3 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD,
                        this.getX(), this.getBodyY(0.5), this.getZ(), 60, 0.6, 0.9, 0.6, 0.45);
            });
        }
        ((WorldScheduler) this.getWorld()).schedule(60, () -> {
            this.transitioning = false;
            clearDominion();
            this.barrierCycleTimer = 0;
            this.correctHitsThisCycle = 0;
            List<PlayerEntity> nearby = this.getWorld().getPlayers(
                    TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(48));
            if (newPhase == 2) {
                this.correctHitsRequired = 2;
                nearby.forEach(p -> p.sendMessage(Text.literal("— Provoked —"), true));
            } else if (newPhase == 3) {
                this.removeStatusEffect(Effects.MAGUS_BARRIER.registryEntry);
                this.darkMatterCooldown = 600;
                this.darkMatterTimer = this.darkMatterCooldown;
                EntityAttributeInstance moveSpeed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (moveSpeed != null) {
                    moveSpeed.removeModifier(PHASE3_SPEED_ID);
                    moveSpeed.addPersistentModifier(new EntityAttributeModifier(
                            PHASE3_SPEED_ID, 0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                EntityAttributeInstance atkSpeed = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
                if (atkSpeed != null) {
                    atkSpeed.removeModifier(PHASE3_ATKSPEED_ID);
                    atkSpeed.addPersistentModifier(new EntityAttributeModifier(
                            PHASE3_ATKSPEED_ID, 0.3, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                nearby.forEach(p -> p.sendMessage(Text.literal("— Unrestrained —"), true));
            }
        });
    }

    public void clearDominion() {
        dominionActive = false;
        dominionTimer = 0;
        frostDominionActive = false;
        fireDominionActive = false;
        lightningDominionActive = false;
        frostDominionTimer = 0;
        fireDominionTimer = 0;
        lightningDominionTimer = 0;
        frostDominionDuration = 0;
        fireDominionDuration = 0;
        lightningDominionDuration = 0;
        arcaneDominionDuration = 0;
        frostMeter.clear();
        for (MagusDominionOrbEntity orb : activeOrbEntities) {
            if (orb != null) orb.discard();
        }
        activeOrbEntities.clear();
    }

    private void activateDominion(SpellSchool school) {
        dominionActive = true;
        if (school.equals(ARCANE)) {
            int orbCount = Math.min(6, 1 + disdainStacks);
            for (int i = 0; i < orbCount; i++) {
                MagusDominionOrbEntity orb = new MagusDominionOrbEntity(RPGMinibossesEntities.MAGUS_DOMINION_ORB, this.getWorld());
                orb.setOwnerEntity(this);
                orb.disdainStacks = this.disdainStacks;
                orb.contemptStacks = this.contemptFulfilledStacks;
                orb.setStartAngle((float)(i * 2 * Math.PI / orbCount));
                orb.setPosition(this.getPos());
                this.getWorld().spawnEntity(orb);
                activeOrbEntities.add(orb);
            }
        } else if (school.equals(FROST)) {
            frostDominionActive = true;
        } else if (school.equals(FIRE)) {
            fireDominionActive = true;
        } else if (school.equals(LIGHTNING)) {
            lightningDominionActive = true;
        }
    }

    public static final TrackedData<Integer> INDICATOR ;

    public static ParticleGroup ARCTICARMORPARTICLES;
    public static ParticleGroup SHIELDPARTICLES_PURPLE;
    public static ParticleGroup SHIELDPARTICLES_BLUE;
    public static ParticleGroup SHIELDPARTICLES_RED;

    /** A school-tinted ground ring riding the entity — the shared barrier/armor aura layout. */
    private static ParticleGroup shieldAura(String particleId, long color) {
        return ParticleGroupBuilder.of(particleId)
                .color(color)
                .scale(2)
                .attached()
                .batch(b -> b.shape(ParticleGroup.Shape.SPHERE).count(1)
                        .verticalOrigin(ParticleGroupBuilder.Batches.FEET));
    }

    public class MinibossMoveConrol extends MoveControl {

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
            // Turn rate escalates with the phase. Phase 3 already gets faster movement and attacks;
            // pivoting harder is what makes that escalation legible instead of just numerical —
            // and it avoids the trap of communicating urgency by playing every clip faster.
            float turn = MagusPrimeEntity.this.phase >= 3 ? 46.0F
                    : MagusPrimeEntity.this.phase == 2 ? 37.0F : 30.0F;
            this.maxYawChange = turn;
            this.maxPitchChange = turn;
            this.lookAtTimer = 8;
        }

        public void tick() {
            if (this.entity.getWorld().isClient()) return; // handled in MagusPrimeEntity.tickHeadRotation()
            if (this.lookAtTimer > 0) {
                this.getTargetYaw().ifPresent((yaw) -> {
                    this.entity.setHeadYaw(this.changeAngle(this.entity.headYaw, yaw, this.maxYawChange));
                    this.entity.setYaw(this.changeAngle(this.entity.getYaw(), yaw, this.maxYawChange));
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
    protected void mobTick() {

        if(this.age < 30){
            return;
        }
        if(this.getTarget() != null) {
            if(this.distanceTo(this.getTarget())< 8){
                ((MinibossMoveConrol)this.getMoveControl()).strafeTo(-2, this.getTarget().getPos().subtract(this.getPos()).crossProduct(new Vec3d(0, 1, 0)).dotProduct(this.getRotationVector()) > 0 ? -0.6F : 0.6F,0.25F);
            }
        }
        if (!this.getWorld().isClient() && phase == 3 && darkMatterTimer >= darkMatterCooldown && !isPerforming() && this.getTarget() != null) {
            this.resetIndicator();
            darkMatterTimer = 0;
            darkMatterCooldown = Math.max(200, darkMatterCooldown - 60);
            darkMatterCastCount++;
            // 50 ticks of channel, then 8 ticks of recovery so the orb landing is not immediately
            // followed by Magus snapping back into locomotion.
            beginCast(58);
            // A catastrophic cast that resolves behind terrain is wasted, so take a clear line
            // first — but only when there isn't one, which keeps normal positioning untouched.
            if (!raycastObstacleFree(this, this.getEyePos(), this.getTarget().getEyePos())) {
                teleportRandomly(TeleportIntent.CHANNEL);
            }

            ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                if (this.getTarget() != null) {
                    MagusPrimeAnimationProvider.CHANNEL_COMMAND.sendForEntity(this);
                    this.getDataTracker().set(CASTINGBOOL, true);
                    this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK);
                    ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

                    ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                        if (this.getTarget() != null) {
                            for (ServerPlayerEntity player : PlayerLookup.tracking(this)) {
                                player.addStatusEffect(new StatusEffectInstance(Effects.DARK_MATTER.registryEntry, 200, 0));
                            }
                            OrbEntity orb = new OrbEntity(RPGMinibosses.ORBENTITY, this.getWorld());
                            orb.setOwner(this);
                            orb.setCastCount(darkMatterCastCount - 1);
                            orb.setDeathStacks(contemptFulfilledStacks);
                            orb.setPosition(this.getTarget().getPos());
                            this.getWorld().spawnEntity(orb);
                            this.activeDarkMatterOrb = orb;
                        }
                    });
                }
            });
            this.casting_timer = 0;
            this.quickcast_timer -= 80;
        }
        if(!this.getWorld().isClient() && casting_timer > 120 && !isPerforming() && this.getTarget() != null ) {
            this.resetIndicator();
            // The heavy families release on tick 40 and settle over the following 8, so the lock
            // runs to 58 (10 lead-in + 48 of clip). Recovery is visual continuity, not downtime.
            beginCast(58);

            ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                if(this.getTarget() != null) {
                    String delivery = this.getTarget().distanceTo(this) < 4 ? "nova" : "projectile";
                    (delivery.equals("nova") ? MagusPrimeAnimationProvider.NOVA_LONG_COMMAND
                            : MagusPrimeAnimationProvider.HEAVY_PROJECTILE_COMMAND).sendForEntity(this);
                    this.getDataTracker().set(CASTINGBOOL, true);
                    this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK);
                    ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);
                    this.performSpell("long", delivery);
                }

            });
            this.casting_timer = 0;
            this.quickcast_timer -= 80;
        }
        if(!this.getWorld().isClient() && quickcast_timer > 80 && !isPerforming() && this.getTarget() != null ) {
            String delivery = this.getTarget().distanceTo(this) < 4 ? "nova" : "projectile";
            (delivery.equals("nova") ? MagusPrimeAnimationProvider.NOVA_QUICK_COMMAND
                    : MagusPrimeAnimationProvider.QUICK_PROJECTILE_COMMAND).sendForEntity(this);
            this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON);

            // The quick clips release on tick 5. Firing on tick 0 meant the bolt left before the
            // arm moved; holding it for those 5 ticks puts the spell on the pose that throws it.
            ((WorldScheduler) this.getWorld()).schedule(5, () -> this.performSpell("short", delivery));
            // A tiny phase-aware settle keeps the release from snapping directly to locomotion.
            beginCast(phase == 1 ? 14 : phase == 2 ? 12 : 10);
            ((WorldScheduler) this.getWorld()).schedule(10, () -> {
                        this.casting_timer -= 20;
                    }
            );

            this.quickcast_timer = 0;
            this.casting_timer -= 20;
        }


        if(!this.getWorld().isClient() && jumptimer > 200 && !isPerforming() && this.getTarget() != null  && this.distanceTo(this.getTarget()) < 4 ) {
            MagusPrimeAnimationProvider.DASH.sendForEntity(this);

            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

            Vec3d vec31 = new Vec3d(-this.getTarget().getX() + this.getX(), 0, -this.getTarget().getZ() + this.getZ());
            Vec3d vec3 = new Vec3d(vec31.normalize().x * 1, 0.75, vec31.normalize().z * 1);
            this.setPosition(this.getPos().add(0, 0.2, 0));
            this.setOnGround(false);
            this.setVelocity(vec3);
            this.jumptimer = 0;
            beginCast(20);
        }
        if(!this.getWorld().isClient() && dash_attack_timer > 240 && !isPerforming() && this.getTarget() != null &&  this.distanceTo(this.getTarget()) > 4) {
            MagusPrimeAnimationProvider.DASH.sendForEntity(this);
            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

            Vec3d vec31 = new Vec3d(this.getTarget().getX() - this.getX(), 0, this.getTarget().getZ() - this.getZ());
            Vec3d vec3 = new Vec3d(vec31.normalize().x * 1, 0.75, vec31.normalize().z * 1);
            this.setPosition(this.getPos().add(0, 0.2, 0));
            this.setOnGround(false);
            this.setVelocity(vec3.multiply(Math.min(1,this.distanceTo(this.getTarget())/6)));
            this.quickcast_timer += 40;
            this.casting_timer += 40;

            this.dash_attack_timer = 0;
            beginCast(20);
        }


        if(this.isSwimming() && this.age % 10 == 0){
            teleportRandomly();
        }

        if(!this.getWorld().isClient()) {
            if(this.getTarget()!= null) {
                dash_attack_timer++;
                quickcast_timer++;
                casting_timer++;
                jumptimer++;
                darkmatter++;

                int cycleInterval = Math.max(100, BASE_BARRIER_CYCLE_TICKS - disdainStacks * 10);
                barrierCycleTimer++;
                if (barrierCycleTimer >= cycleInterval && phase < 3) {
                    barrierCycleTimer = 0;
                    cycleElementIndex = (cycleElementIndex + 1) % BARRIER_CYCLE.length;
                    this.spellSchool = BARRIER_CYCLE[cycleElementIndex];
                    this.consecutiveWrongHits = 0;
                    this.correctHitsThisCycle = 0;
                    this.dominionTimer = 0;
                    if (!this.hasStatusEffect(Effects.MAGUS_BARRIER.registryEntry)) {
                        this.addStatusEffect(new StatusEffectInstance(Effects.MAGUS_BARRIER.registryEntry, -1, 0));
                    }
                    playBarrierCue(BarrierCue.CHANGE);
                    List<PlayerEntity> nearbyPlayers = this.getWorld().getPlayers(TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(32));
                    nearbyPlayers.forEach(p -> p.sendMessage(Text.literal("Only " + this.spellSchool.id.getPath().toUpperCase() + " damages."), true));
                }
            }
            if(this.getTarget()!= null) {
                disdainTimer++;
                if (disdainTimer >= 1200) {
                    disdainTimer = 0;
                    disdainStacks++;
                    EntityAttributeInstance dmg = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    if (dmg != null) {
                        dmg.removeModifier(DISDAIN_MODIFIER_ID);
                        dmg.addPersistentModifier(new EntityAttributeModifier(
                                DISDAIN_MODIFIER_ID, disdainStacks * 0.05, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                }
            }

            if (this.getTarget()!= null && phase == 3) darkMatterTimer++;

            // Dominion activation counter — per-school so overlap is possible
            if (this.getTarget() != null && phase < 3) {
                boolean currentSchoolActive =
                        (spellSchool.equals(ARCANE) && !activeOrbEntities.isEmpty())
                        || (spellSchool.equals(FROST) && frostDominionActive)
                        || (spellSchool.equals(FIRE) && fireDominionActive)
                        || (spellSchool.equals(LIGHTNING) && lightningDominionActive);
                if (!currentSchoolActive && this.hasStatusEffect(Effects.MAGUS_BARRIER.registryEntry)) {
                    dominionTimer++;
                    if (dominionTimer >= DOMINION_ACTIVATION_TICKS) {
                        activateDominion(spellSchool);
                        dominionTimer = 0;
                    }
                }
            }

            // Per-dominion duration auto-expire at 2 * BASE_BARRIER_CYCLE_TICKS
            int dominionMaxDuration = 2 * BASE_BARRIER_CYCLE_TICKS;
            if (frostDominionActive) {
                if (++frostDominionDuration >= dominionMaxDuration) {
                    frostDominionActive = false; frostDominionDuration = 0; frostMeter.clear();
                }
            }
            if (fireDominionActive) {
                if (++fireDominionDuration >= dominionMaxDuration) {
                    fireDominionActive = false; fireDominionDuration = 0;
                }
            }
            if (lightningDominionActive) {
                if (++lightningDominionDuration >= dominionMaxDuration) {
                    lightningDominionActive = false; lightningDominionDuration = 0;
                }
            }
            if (!activeOrbEntities.isEmpty()) {
                if (++arcaneDominionDuration >= dominionMaxDuration) {
                    activeOrbEntities.forEach(MagusDominionOrbEntity::discard);
                    activeOrbEntities.clear(); arcaneDominionDuration = 0;
                }
            }

            // Frost Dominion: circling blizzard ring + per-player meter
            if (frostDominionActive) {
                // Circling storm particles at aura radius, density scales with disdain
                if (this.age % 2 == 0 && this.getWorld() instanceof ServerWorld sw) {
                    // Ring density cut by roughly a third (was 16 + stacks * 8). Three concentric
                    // rings at radius 8/16/24 overlap heavily from any viewing angle, so the removed
                    // third was mostly occluded by the other two.
                    int ringCount = 11 + disdainStacks * 5;
                    double radius = 16.0;
                    for (int i = 0; i < ringCount; i++) {
                        double angle = this.age * 0.04 + i * (2 * Math.PI / ringCount);
                        double px = this.getX() + Math.cos(angle) * radius;
                        double pz = this.getZ() + Math.sin(angle) * radius;
                        double py = this.getY() + 1.5 + Math.sin(this.age * 0.12 + i * 0.8) * 2.0;
                        sw.spawnParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 4, 0.3, 0.5, 0.3, 0.05);
                    }
                }
                if (this.age % 2 == 0 && this.getWorld() instanceof ServerWorld sw) {
                    // Ring density cut by roughly a third (was 16 + stacks * 8). Three concentric
                    // rings at radius 8/16/24 overlap heavily from any viewing angle, so the removed
                    // third was mostly occluded by the other two.
                    int ringCount = 11 + disdainStacks * 5;
                    double radius = 8.0;
                    for (int i = 0; i < ringCount; i++) {
                        double angle = this.age * 0.04 + i * (2 * Math.PI / ringCount);
                        double px = this.getX() + Math.cos(angle) * radius;
                        double pz = this.getZ() + Math.sin(angle) * radius;
                        double py = this.getY() + 1.5 + Math.sin(this.age * 0.12 + i * 0.8) * 2.0;
                        sw.spawnParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 4, 0.3, 0.5, 0.3, 0.05);
                    }
                }
                if (this.age % 2 == 0 && this.getWorld() instanceof ServerWorld sw) {
                    // Ring density cut by roughly a third (was 16 + stacks * 8). Three concentric
                    // rings at radius 8/16/24 overlap heavily from any viewing angle, so the removed
                    // third was mostly occluded by the other two.
                    int ringCount = 11 + disdainStacks * 5;
                    double radius = 24.0;
                    for (int i = 0; i < ringCount; i++) {
                        double angle = this.age * 0.04 + i * (2 * Math.PI / ringCount);
                        double px = this.getX() + Math.cos(angle) * radius;
                        double pz = this.getZ() + Math.sin(angle) * radius;
                        double py = this.getY() + 1.5 + Math.sin(this.age * 0.12 + i * 0.8) * 2.0;
                        sw.spawnParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 4, 0.3, 0.5, 0.3, 0.05);
                    }
                }
                frostDominionTimer++;
                int frostInterval = Math.max(4, 8 - disdainStacks / 5);
                if (frostDominionTimer >= frostInterval) {
                    frostDominionTimer = 0;
                    List<PlayerEntity> nearFrost = this.getWorld().getPlayers(
                            TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(24));
                    for (PlayerEntity fp : nearFrost) {
                        UUID pid = fp.getUuid();
                        int meter = frostMeter.getOrDefault(pid, 0) + 1;
                        if (meter >= 100) {
                            fp.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                            if (contemptFulfilledStacks > 3) {
                                fp.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 0));
                            }
                            meter = 0;
                        }
                        frostMeter.put(pid, meter);
                        // Frost particles proportional to meter fill
                        if (meter > 0 && this.getWorld() instanceof ServerWorld sw) {
                            int particleCount = Math.max(1, meter / 20);
                            sw.spawnParticles(ParticleTypes.SNOWFLAKE,
                                    fp.getX(), fp.getY() + 1.0, fp.getZ(),
                                    particleCount, 0.4, 0.6, 0.4, 0.02);
                        }
                    }
                }
            }

            // Fire Dominion: place up to 12 patches; assign to players first, fill remainder randomly
            if (fireDominionActive) {
                fireDominionTimer++;
                int fireInterval = Math.max(80, 200 - disdainStacks * 5);
                if (fireDominionTimer >= fireInterval) {
                    fireDominionTimer = 0;
                    Optional<RegistryEntry.Reference<Spell>> fireDominionSpell =
                            SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "magus_fire_dominion"));
                    if (fireDominionSpell.isPresent()) {
                        List<PlayerEntity> nearFire = this.getWorld().getPlayers(
                                TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(32));
                        int patchCount = Math.min(12, 2 + disdainStacks);
                        SpellExecution.ImpactContext fireCtx = new SpellExecution.ImpactContext()
                                .power(SpellPower.getSpellPower(FIRE, this));
                        // Player-targeted patches
                        int playerPatches = Math.min(patchCount, nearFire.size());
                        for (int i = 0; i < playerPatches; i++) {
                            Vec3d pos = nearFire.get(i).getPos();
                            CloudPlacer.placeCloud(this.getWorld(), this, nearFire.get(i), pos,
                                    fireDominionSpell.get(), fireCtx.position(pos));
                        }
                        // Random fill for remaining patches
                        for (int i = playerPatches; i < patchCount; i++) {
                            double ang = this.getRandom().nextDouble() * 2 * Math.PI;
                            double dist = 4 + this.getRandom().nextDouble() * 12;
                            Vec3d pos = this.getPos().add(Math.cos(ang) * dist, 0, Math.sin(ang) * dist);
                            PlayerEntity anchor = nearFire.isEmpty() ? null : nearFire.get(0);
                            CloudPlacer.placeCloud(this.getWorld(), this, anchor != null ? anchor : this, pos,
                                    fireDominionSpell.get(), fireCtx.position(pos));
                        }
                    }
                }
            }

            // Lightning Dominion: warn then strike player positions
            if (lightningDominionActive) {
                lightningDominionTimer++;
                int lightningInterval = Math.max(60, 140 - disdainStacks * 4);
                if (lightningDominionTimer >= lightningInterval) {
                    lightningDominionTimer = 0;
                    List<PlayerEntity> nearLightning = this.getWorld().getPlayers(
                            TargetPredicate.createNonAttackable(), this, this.getBoundingBox().expand(32));
                    for (PlayerEntity lp : nearLightning) {
                        Vec3d strikePos = lp.getPos();
                        if (this.getWorld() instanceof ServerWorld sw) {
                            sw.spawnParticles(ParticleTypes.END_ROD,
                                    strikePos.x, strikePos.y + 0.5, strikePos.z, 20, 0.3, 1.0, 0.3, 0.05);
                        }
                        ((WorldScheduler) this.getWorld()).schedule(25, () -> {
                            if (this.getWorld() instanceof ServerWorld sw) {
                                sw.spawnParticles(ParticleTypes.FLASH,
                                        strikePos.x, strikePos.y, strikePos.z, 1, 0, 0, 0, 0);
                                sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                                        strikePos.x, strikePos.y + 0.5, strikePos.z, 30, 0.5, 0.5, 0.5, 0.1);
                            }
                            List<PlayerEntity> struck = this.getWorld().getEntitiesByType(
                                    TypeFilter.instanceOf(PlayerEntity.class),
                                    new Box(strikePos.x - 1.5, strikePos.y - 0.5, strikePos.z - 1.5,
                                            strikePos.x + 1.5, strikePos.y + 3, strikePos.z + 1.5),
                                    e -> true);
                            struck.forEach(e -> e.damage(
                                    this.getWorld().getDamageSources().magic(),
                                    6.0f + contemptFulfilledStacks * 0.5f));
                        });
                    }
                }
            }
        }
        if(this.getTarget() != null) {
            this.getLookControl().lookAt(this.getTarget(),360,360);
        }

        super.mobTick();

    }


    @Environment(value = EnvType.CLIENT)
    public static void setRotationFromVelocity(Entity entity) {
        Vec3d vel = entity.getVelocity();
        if (vel.horizontalLengthSquared() > 1.0E-7 && entity instanceof PathAwareEntity pathAwareEntity) {
            float movDirYaw = (float)(MathHelper.atan2(vel.z, vel.x) * 57.2957763671875) - 90.0F;
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
    /**
     * Deadline (in entity {@code age} ticks) until which the boss is busy performing an action.
     * Because it is a deadline it auto-expires, so the action lock can never be permanently
     * stranded by a dropped callback — this replaces the old boolean {@code performing} flag.
     */
    private int performingUntil = 0;

    public int dash_attack_timer;
    public int quickcast_timer;
    public int casting_timer;
    public int darkmatter;

    public int jumptimer;
    public int barrierCycleTimer = 0;
    public int cycleElementIndex = 0;
    public int consecutiveWrongHits = 0;
    public float empowerMultiplier = 1.0f;
    public int disdainStacks = 0;
    public int disdainTimer = 0;
    public int contemptFulfilledStacks = 0;
    public int phase = 1;
    public boolean transitioning = false;
    public int correctHitsThisCycle = 0;
    public int correctHitsRequired = 1;
    public int darkMatterCooldown = 600;
    public int darkMatterTimer = 0;
    public int darkMatterCastCount = 0;
    public boolean dominionActive = false;
    public int dominionTimer = 0;
    public boolean frostDominionActive = false;
    public boolean fireDominionActive = false;
    public boolean lightningDominionActive = false;
    public int frostDominionTimer = 0;
    public int fireDominionTimer = 0;
    public int lightningDominionTimer = 0;
    public int frostDominionDuration = 0;
    public int fireDominionDuration = 0;
    public int lightningDominionDuration = 0;
    public int arcaneDominionDuration = 0;
    private final Map<UUID, Integer> frostMeter = new HashMap<>();
    private final List<MagusDominionOrbEntity> activeOrbEntities = new ArrayList<>();
    public OrbEntity activeDarkMatterOrb = null;
    /**
     * What the next beat needs from the destination. It only shifts the range band — the angle
     * spread, line-of-sight check and minimum distance from the target are unchanged, so this
     * biases the existing selection rather than replacing it.
     */
    public enum TeleportIntent {
        /** Getting off bad footing. Any usable casting range will do. */
        REPOSITION,
        /** About to open a long channel: take the room and the clear line it needs. */
        CHANNEL
    }

    protected boolean teleportRandomly() {
        return teleportRandomly(TeleportIntent.REPOSITION);
    }

    protected boolean teleportRandomly(TeleportIntent intent) {
        if (!this.getWorld().isClient() && this.isAlive()) {
            LivingEntity target = this.getTarget();
            Vec3d anchor = target == null ? this.getPos() : target.getPos();
            for (int attempt = 0; attempt < 10; attempt++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                if (!Double.isNaN(lastTeleportAngle)
                        && Math.abs(MathHelper.wrapDegrees(Math.toDegrees(angle - lastTeleportAngle))) < 55.0) {
                    angle += Math.PI * 0.6;
                }
                double range;
                if (intent == TeleportIntent.CHANNEL) {
                    range = 13.0 + this.random.nextDouble() * 7.0;
                } else {
                    range = target == null ? 8.0 + this.random.nextDouble() * 12.0
                            : 9.0 + this.random.nextDouble() * 9.0;
                }
                double y = anchor.y + this.random.nextInt(7) - 3;
                if (teleportTo(anchor.x + Math.cos(angle) * range, y, anchor.z + Math.sin(angle) * range)) {
                    lastTeleportAngle = angle;
                    // Arrive already looking at the target, so the blink reads as a decision.
                    if (target != null) this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private enum BarrierCue { CHANGE, CORRECT_HIT, BREAK }

    /**
     * Every barrier event gets its own pose, its own particle shape and its own sound, because the
     * barrier is the one mechanic where guessing wrong actively helps Magus. The four states a
     * player has to tell apart are: it changed school, you chipped it, you fed it (handled inline
     * in {@link #damage}) and you broke it.
     */
    private void playBarrierCue(BarrierCue cue) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) return;
        switch (cue) {
            case CHANGE -> {
                // A ring of the new school's colour sweeping out, so the change is visible even
                // from behind, plus a pitch that rises with the cycle position.
                MagusPrimeAnimationProvider.BARRIER_COMMAND.sendForEntity(this);
                serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                        this.getX(), this.getBodyY(0.55), this.getZ(), 16, 0.65, 1.0, 0.65, 0.05);
                ParticleHelper.sendBatches(this, List.of(barrierParticles()));
                this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F + cycleElementIndex * 0.15F);
            }
            case CORRECT_HIT -> {
                // Destabilisation: a tight crack of shards at chest height, nothing more. It has to
                // stay small so it never competes with the break.
                serverWorld.spawnParticles(ParticleTypes.CRIT,
                        this.getX(), this.getBodyY(0.6), this.getZ(), 10, 0.55, 0.5, 0.55, 0.12);
                this.playSound(SoundEvents.BLOCK_GLASS_HIT, 1.0F, 1.35F);
            }
            case BREAK -> {
                // Collapse: the guard comes apart outward and Magus reacts to losing it.
                MagusPrimeAnimationProvider.BARRIER_BREAK_COMMAND.sendForEntity(this);
                serverWorld.spawnParticles(ParticleTypes.CRIT,
                        this.getX(), this.getBodyY(0.55), this.getZ(), 40, 0.9, 1.1, 0.9, 0.32);
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getBodyY(0.55), this.getZ(), 24, 0.8, 0.9, 0.8, 0.25);
                serverWorld.spawnParticles(ParticleTypes.FLASH,
                        this.getX(), this.getBodyY(0.6), this.getZ(), 1, 0, 0, 0, 0);
                this.playSound(SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY);
                this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.6F, 0.7F);
            }
        }
    }

    /** The aura batch for whichever school the barrier is currently locked to. */
    private ParticleGroup barrierParticles() {
        if (this.spellSchool.equals(FIRE)) return SHIELDPARTICLES_RED;
        if (this.spellSchool.equals(FROST)) return SHIELDPARTICLES_BLUE;
        return SHIELDPARTICLES_PURPLE;
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
        Vec3d destination = mutable.up().toCenterPos();
        boolean clearSight = this.getTarget() == null || raycastObstacleFree(this, destination,
                this.getTarget().getEyePos());
        if (bl && !bl2 && clearSight && (this.getTarget() == null || destination.squaredDistanceTo(this.getTarget().getPos()) > 16.0)) {
            Vec3d vec3d = this.getPos();
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.WITCH, this.getX(), this.getBodyY(0.55), this.getZ(),
                        16, 0.45, 0.8, 0.45, 0.06);
            }
            boolean bl3 = this.teleport(x, y, z, true);
            if (bl3) {
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getBodyY(0.55), this.getZ(),
                            22, 0.55, 0.9, 0.55, 0.1);
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




}
