package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import mod.azure.azurelib.core.animation.*;
import mod.azure.azurelib.core.object.PlayState;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.fx.ParticleBatch;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_engine.fx.SpellEngineSounds;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.network.Packets;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TemplarEntity extends MinibossEntity{
    private boolean performing;
    private boolean is_staff = false;
    private boolean is_twirl = false;
    List<Item> bonusList = List.of();
    protected TemplarEntity(EntityType<? extends PatrolEntity> entityType, World world) {
        super(entityType, world);
        super.bonusList = Registries.ITEM.stream().filter(item -> {
            return
                    (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_3_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_4_weapons")))
                            || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_5_weapons"))))
                            && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                            new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                    ;
        }).toList();
    }
    protected TemplarEntity(EntityType<? extends PatrolEntity> entityType, World world, boolean lesser) {
        super(entityType, world);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);
        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_3_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_4_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_5_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
        }

    }
    protected TemplarEntity(EntityType<? extends PatrolEntity> entityType, World world, boolean lesser,float spawnCoeff) {
        super(entityType, world,spawnCoeff);
        if(lesser) {
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_2_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_1_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
            this.getDataTracker().set(MinibossEntity.LESSER,true);
        }
        else{
            super.bonusList = Registries.ITEM.stream().filter(item -> {
                return
                        (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_3_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_4_weapons")))
                                || new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "loot_tier/tier_5_weapons"))))
                                && (new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/claymore"))) ||
                                new ItemStack(item).isIn(TagKey.of(RegistryKeys.ITEM, Identifier.of("rpg_series", "weapon_type/glaive"))))
                        ;
            }).toList();
        }

    }
    public boolean skipMainHand(){
        return true;
    }
    @Override
    public EquipmentSlot getPreferredEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.OFFHAND;
    }

    public static final RawAnimation STAFF = RawAnimation.begin().thenPlay("animation.valkyrie.staff");
    public static final RawAnimation DASHRIGHT = RawAnimation.begin().thenPlay("animation.valkyrie.dashright");
    public static final RawAnimation DASHLEFT = RawAnimation.begin().thenPlay("animation.valkyrie.dashleft");
    public static final RawAnimation DASH_ATTACK = RawAnimation.begin().thenPlay("animation.valkyrie.dash_attack");

    public static final RawAnimation TWIRL = RawAnimation.begin().thenPlay("animation.valkyrie.twirl");
    public static final RawAnimation IDLE = RawAnimation.begin().thenPlay("animation.valkyrie.idle");
    public static final RawAnimation SWING1 = RawAnimation.begin().then("animation.mob.swing1", Animation.LoopType.PLAY_ONCE);
    public static final RawAnimation SWING2 = RawAnimation.begin().then("animation.mob.swing2", Animation.LoopType.PLAY_ONCE);

    @Override
    protected void initCustomGoals() {

        this.goalSelector.add(2, new MeleeAttackGoal(this,1.0F,true));
        super.initCustomGoals();
    }
    @Override
    public boolean isTwoHand() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        super.registerControllers(animationData);


        animationData.add(
                new AnimationController<>(this, "twirl", event -> PlayState.CONTINUE)
                        .triggerableAnim("twirl", TWIRL));
        animationData.add(
                new AnimationController<>(this, "dash_attack", event -> PlayState.CONTINUE)
                        .triggerableAnim("dash_attack", DASH_ATTACK));
        animationData.add(
                new AnimationController<>(this, "staff", event -> PlayState.CONTINUE)
                        .triggerableAnim("staff", STAFF));
        animationData.add(
                new AnimationController<>(this, "dashleft", event -> PlayState.CONTINUE)
                        .triggerableAnim("dashleft", DASHLEFT));
        animationData.add(
                new AnimationController<>(this, "dashright", event -> PlayState.CONTINUE)
                        .triggerableAnim("dashright", DASHRIGHT));
        animationData.add(
                new AnimationController<>(this, "swing1", event -> PlayState.CONTINUE)
                        .triggerableAnim("swing1", SWING1));
        animationData.add(
                new AnimationController<>(this, "swing2", event -> PlayState.CONTINUE)
                        .triggerableAnim("swing2", SWING2));
        animationData.add(
                new AnimationController<>(this, "down", event -> PlayState.CONTINUE)
                        .triggerableAnim("down", DOWNANIM));
    }

            public void applyIntroEffect(){
        super.applyIntroEffect();
    }
    public RegistryEntry<StatusEffect> getIntroEffect(){
        return Effects.PETRIFIED.registryEntry;
    }
    public Item getDefaultItem(){
        return Items.AIR;
    }
    public ItemStack getBackWeapon(){
        ItemStack stack = new ItemStack(getDefaultItem());
        if(this.getRandom().nextBoolean() && !bonusList.isEmpty()){
            Item item = bonusList.get(this.getRandom().nextInt(bonusList.size()));
            stack = new ItemStack(item);

        }
        return stack;

    }
    @Override
    protected void mobTick() {
        super.mobTick();

        if(this.getTarget() != null) {
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES,this.getTarget().getEyePos());
        }
        if(!this.getWorld().isClient()) {
            stafftimer++;
            twirltimer++;
            dashtimer++;
            dash_attack_timer++;
            cooldown++;
        }

        if(!this.getWorld().isClient() && dashtimer > 80 && !this.performing && this.getTarget() != null  ) {
            if(this.getTarget().getPos().subtract(this.getPos()).crossProduct(new Vec3d(0,1,0)).dotProduct(this.getRotationVector()) > 0 ) {
                ((TemplarEntity) this).triggerAnim("dashleft", "dashleft");
                this.setVelocity(this.getRotationVector().crossProduct(new Vec3d(0,-1,0)).multiply(2));
            }
            else{
                ((TemplarEntity) this).triggerAnim("dashright", "dashright");
                this.setVelocity(this.getRotationVector().crossProduct(new Vec3d(0,1,0)).multiply(2));

            }
            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

            ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                        this.performing = false;

                    }
            );
            this.dashtimer = 140 - (int)(140*this.getCooldownCoeff());
            this.performing = true;

        }
        else
        if(!this.getWorld().isClient() && dash_attack_timer > 80 && !this.performing && this.getTarget() != null &&  this.distanceTo(this.getTarget()) > 4) {
            ((TemplarEntity)this).triggerAnim("dash_attack","dash_attack");
            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);
            Vec3d vec31 = new Vec3d(this.getTarget().getX() - this.getX(), 0, this.getTarget().getZ() - this.getZ());
            Vec3d vec3 = new Vec3d(vec31.normalize().x * 1.5, 0.45, vec31.normalize().z * 1.5);
            this.setVelocity(vec3);

            ((WorldScheduler) this.getWorld()).schedule(20, () -> {
                        this.performing = false;

                    }
            );
            ((WorldScheduler) this.getWorld()).schedule(16, () -> {


                ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);
            });
            this.dash_attack_timer = 80 - (int)(80*this.getCooldownCoeff());
            this.performing = true;
        }
        else
        if(!this.getWorld().isClient() && twirltimer > 180 && !this.performing && this.getTarget() != null && this.distanceTo(this.getTarget())<= 4) {
            ((TemplarEntity)this).triggerAnim("twirl","twirl");
            ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);

            ((WorldScheduler) this.getWorld()).schedule(100, () -> {
                        this.performing = false;
                    }
            );
            ((WorldScheduler) this.getWorld()).schedule(70, () ->{
                ParticleHelper.sendBatches(this, SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "holy_burst")).release.particles);
                for(Entity entity : TargetHelper.targetsFromArea(this,SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "holy_burst")).range,SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "holy_burst")).target.area, entity ->{ return entity != this;})) {


                    SpellHelper.performImpacts(this.getWorld(), this, entity, this, SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "holy_burst")).get(),
                            SpellRegistry.from(this.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "holy_burst")).impacts, new SpellHelper.ImpactContext().power(SpellPower.getSpellPower(SpellSchools.HEALING, this)).position(this.getPos()));

                }
                this.is_twirl = false;

            });
            this.twirltimer = 180 - (int)(180*this.getCooldownCoeff());
            this.performing = true;
            this.is_twirl = true;

        }
        else
        if(!this.getWorld().isClient() && stafftimer > 300 && !this.performing && this.getTarget() != null ) {
            this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK);

            ((TemplarEntity)this).triggerAnim("staff","staff");
            ((WorldScheduler) this.getWorld()).schedule(160, () -> {
                this.performing = false;
                this.is_staff = false;

                    }
            );
            var spell = SpellRegistry.from(this.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "divine_fall"));
            this.getNavigation().stop();
            for(int i = 0 ; i < 5; i++) {
                ((WorldScheduler) this.getWorld()).schedule(20*(i+1), () -> {
                            if (this.getTarget() != null) {

                                SpellHelper.ImpactContext context = new SpellHelper.ImpactContext(1.0F, 1.0F, this.getTarget().getPos(), SpellPower.getSpellPower(SpellSchools.HEALING, this), SpellTarget.FocusMode.DIRECT, 0);
                                SoundHelper.playSound(this.getWorld(), this, new Sound(SpellEngineSounds.GENERIC_HEALING_RELEASE.id()));
                                Vec3d pos = this.getTarget().getBoundingBox().getCenter();
                                ((WorldScheduler) this.getWorld()).schedule(25, () -> {

                                            for (Entity entity : this.getWorld().getOtherEntities(this, Box.of(pos, 6, 6, 6))) {
                                                if (entity instanceof LivingEntity living && raycastObstacleFree(living, pos, living.getBoundingBox().getCenter())) {
                                                    SpellHelper.performImpacts(this.getWorld(), this, this.getTarget(), this.getTarget(), spell.get(), spell.get().value().impacts,
                                                            context, false);
                                                }
                                            }

                                            sendBatches(pos, this, spell.get().value().area_impact.particles, 1, PlayerLookup.tracking(this), false);

                                        }

                                );
                            }
                }
                );

            }
            this.stafftimer = 300 - (int)(300*this.getCooldownCoeff());
            this.is_staff = true;
            this.performing = true;
        }
        if(this.is_twirl && this.getTarget() != null && !this.getWorld().isClient()){
            if(this.age % 4 == 0) {
                ((ServerWorld) this.getWorld()).playSound(this, this.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.8F, 1F);
            }
        }
        else
        if(this.is_staff){
            this.getNavigation().stop();

        }

    }
    public static void sendBatches(Vec3d target, Entity trackedEntity, ParticleBatch[] batches, float countMultiplier, Collection<ServerPlayerEntity> trackers, boolean includeSourceEntity) {
        if (batches != null && batches.length != 0) {
            int sourceEntityId = trackedEntity.getId();
            Packets.ParticleBatches.SourceType sourceType = Packets.ParticleBatches.SourceType.COORDINATE;
            ArrayList<Packets.ParticleBatches.Spawn> spawns = new ArrayList();
            ParticleBatch[] var8 = batches;
            int var9 = batches.length;

            for(int var10 = 0; var10 < var9; ++var10) {
                ParticleBatch batch = var8[var10];
                Vec3d sourceLocation = Vec3d.ZERO;
                sourceLocation =target;

                spawns.add(new Packets.ParticleBatches.Spawn(includeSourceEntity ? sourceEntityId : 0, trackedEntity.getYaw(), trackedEntity.getPitch(), sourceLocation, batch));
            }

            Packets.ParticleBatches packet = new Packets.ParticleBatches(sourceType, countMultiplier, spawns);
            if (trackedEntity instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity)trackedEntity;
                if (ServerPlayNetworking.canSend(serverPlayer, Packets.ParticleBatches.ID)) {
                    ServerPlayNetworking.send(serverPlayer, packet);
                }
            }

            trackers.forEach((serverPlayerx) -> {
                if (ServerPlayNetworking.canSend(serverPlayerx, Packets.ParticleBatches.ID)) {
                    ServerPlayNetworking.send(serverPlayerx, packet);
                }

            });
        }
    }
    private static BlockHitResult raycastObstacle(Entity entity, Vec3d start, Vec3d end) {
        return entity.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
    }

    static boolean raycastObstacleFree(Entity entity, Vec3d start, Vec3d end) {
        BlockHitResult hit = raycastObstacle(entity, start, end);
        return hit.getType() != HitResult.Type.BLOCK;
    }
    public int cooldown;

    public int dashtimer;
    public int dash_attack_timer;

    public int twirltimer = 100;
    public int stafftimer = 160;
    public boolean   swingBool;

    @Override
    public void tick() {

        super.tick();
    }
    public boolean tryAttack(Entity target) {
        if(!performing) {
            if (swingBool) {
                (this).triggerAnim("swing1", "swing1");
                swingBool = false;

            } else {
                (this).triggerAnim("swing2", "swing2");
                swingBool = true;

            }
        }
        return super.tryAttack(target);

    }

}
