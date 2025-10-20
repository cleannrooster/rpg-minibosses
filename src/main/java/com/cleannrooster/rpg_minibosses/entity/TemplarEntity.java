package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import mod.azure.azurelib.core.animation.*;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.object.PlayState;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.spell_engine.api.spell.*;

import net.spell_engine.entity.SpellProjectile;

import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.internals.WorldScheduler;
import net.spell_engine.network.Packets;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.particle.Particles;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.VectorHelper;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.*;
import static net.spell_engine.internals.SpellHelper.fallProjectile;
import static net.spell_engine.internals.SpellHelper.lookupAndPerformAreaImpact;

public class TemplarEntity extends MinibossEntity{
    private boolean performing;
    private boolean is_staff = false;
    private boolean is_twirl = false;
    List<Item> bonusList = List.of();
    private int parryTimer = 0;


    protected TemplarEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
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

    @Override
    public float getMovementSpeed() {
        return (this.getTarget() != null && this.getTarget().distanceTo(this) > 4 ? 2F : 1F )*super.getMovementSpeed();
    }

    protected TemplarEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser) {
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

    @Override
    public ItemStack getMainWeapon() {
        return Items.DIAMOND_SWORD.getDefaultStack();
    }

    protected TemplarEntity(EntityType<? extends PathAwareEntity> entityType, World world, boolean lesser, float spawnCoeff) {
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
        return false;
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

        this.goalSelector.add(2, new MeleeAttackGoalTemplar(this,1.0F,true));
        super.initCustomGoals();
    }
    @Override
    public boolean isTwoHand() {
        return true;
    }


    @Override
    public double squaredAttackRange(LivingEntity target) {
        return Math.max(super.squaredAttackRange(target),3.5);
    }
    public static void spawnParticlesSlash(Entity entity, ServerWorld world, float yaw, float pitch, float range){

        int iii = -200;
        for (int i = 0; i < 5; i++) {

            for (int ii = 0; ii < 80; ii++) {

                iii++;

                int finalIii = iii;
                int finalI = i;
                int finalIi = ii;
                ((WorldScheduler)world).schedule(i+1,() ->{
                    if(world != null) {
                        double x = 0;
                        double x2 = 0;

                        double z = 0;
                        x =  ((range*entity.getWidth() + range*entity.getWidth() * sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));
                        x2 =  -((1.2*range*entity.getWidth() + range*entity.getWidth()*sin(20 *  ((double) finalIii /(double)(4*31.74)))) * cos(((double) finalIii /(double)(4*31.74))));

                        z =  pitch*((1.2*range*entity.getWidth() + 0*entity.getWidth() * sin(20 * ((double) finalIii /(double)(4*31.74)))) * sin(((double) finalIii /(double)(4*31.74))));
                        float f7 = entity.getYaw()+-90;
                        float f = (float) (yaw - 90);
                        //Vec3d vec3d = rotate(x,0,z,Math.toRadians(-f7),Math.toRadians(f),0);
                        Vec3d vec3d2 = rotate(x2,0,z,Math.toRadians(-f7),Math.toRadians(f),0/*( > 0 ? 1 : -1) *  Math.clamp(180/entity.getYaw(),1,180)*Math.toRadians((entity.getPitch())/2)*/);
                        // vec3d2 = rotate(vec3d2.x,vec3d2.y,vec3d2.z,0,0,Math.toRadians(entity.getPitch()*((Math.atan( Math.toRadians(entity.getYaw()))))));
                        vec3d2 =  VectorHelper.rotateTowards(vec3d2,new Vec3d(0,-1,0),entity.getPitch());
                        //Vec3d vec3d3 = vec3d.add(entity.getEyePos().getX(),entity.getEyeY(),entity.getEyePos().getZ());
                        Vec3d vec3d4 = vec3d2.add(entity.getEyePos().getX(),entity.getEyeY(),entity.getEyePos().getZ());

                        double y = entity.getY()+entity.getHeight()/2;




                        for(ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
                            if ((2*range - player.getRandom().nextInt(2*Math.max(0,(int)(range - (-x2))) + 1))  == 2*range){


                                if (finalIi % 4 == 1) {
                                    //serverWorld.spawnParticles(player, Particles.snowflake.particleType,true, vec3d3.getX(), vec3d3.getY(), vec3d3.getZ(), 1, 0, 0, 0, 0);
                                    world.spawnParticles(player, Particles.holy_spell.particleType, true, vec3d4.getX(), vec3d4.getY(), vec3d4.getZ(), 1, 0, 0, 0, 0);
                                    world.spawnParticles(player, ParticleTypes.ELECTRIC_SPARK, true, vec3d4.getX(), vec3d4.getY(), vec3d4.getZ(), 1, 0, 0, 0, 0);

                                }
                                //serverWorld.spawnParticles(player,Particles.frost_shard.particleType, true, vec3d3.getX(), vec3d3.getY(), vec3d3.getZ(), 1, 0, 0, 0, 0);
                                world.spawnParticles(player, Particles.holy_spell.particleType, true, vec3d4.getX(), vec3d4.getY(), vec3d4.getZ(), 1, 0, 0, 0, 0);
                            }
                        }
                        if(entity instanceof ServerPlayerEntity player) {
                            if ((2*range - player.getRandom().nextInt(2*Math.max(0,(int)(range - (-x2))) + 1))  == 2*range){

                                if (finalIi % 4 == 1) {
                                    //serverWorld.spawnParticles(player, Particles.snowflake.particleType,true, vec3d3.getX(), vec3d3.getY(), vec3d3.getZ(), 1, 0, 0, 0, 0);
                                    world.spawnParticles(player, Particles.holy_spell.particleType, true, vec3d4.getX(), vec3d4.getY(), vec3d4.getZ(), 1, 0, 0, 0, 0);
                                    world.spawnParticles(player, ParticleTypes.ELECTRIC_SPARK, true, vec3d4.getX(), vec3d4.getY(), vec3d4.getZ(), 1, 0, 0, 0, 0);

                                }
                                //serverWorld.spawnParticles(player,Particles.frost_shard.particleType, true, vec3d3.getX(), vec3d3.getY(), vec3d3.getZ(), 1, 0, 0, 0, 0);
                                world.spawnParticles(player, Particles.holy_spell.particleType, true, vec3d4.getX(), vec3d4.getY(), vec3d4.getZ(), 1, 0, 0, 0, 0);

                            }
                        }
                    }
                });

            }


        }
    }
    public static Vec3d rotate(double x, double y, double z, double pitch, double roll, double yaw) {
        double cosa = Math.cos(yaw);
        double sina = Math.sin(yaw);

        double cosb = Math.cos(pitch);
        double sinb = Math.sin(pitch);
        double cosc = Math.cos(roll);
        double sinc = Math.sin(roll);

        double Axx = cosa * cosb;
        double Axy = cosa * sinb * sinc - sina * cosc;
        double Axz = cosa * sinb * cosc + sina * sinc;

        double Ayx = sina * cosb;
        double Ayy = sina * sinb * sinc + cosa * cosc;
        double Ayz = sina * sinb * cosc - cosa * sinc;

        double Azx = -sinb;
        double Azy = cosb * sinc;
        double Azz = cosb * cosc;

        Vec3d vec3 = new Vec3d(Axx * x + Axy * y + Axz * z,Ayx * x + Ayy * y + Ayz * z,Azx * x + Azy * y + Azz * z);
        return vec3;
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        super.registerControllers(animationData);




        animationData.add(
                new AnimationController<MinibossEntity>(this, "actions",
                        0, this::predicateTemplar)
                        .triggerableAnim("swing1", SWING1).triggerableAnim("swing2",SWING2)  .triggerableAnim("staff", STAFF));



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





        if (this.getTarget() != null) {
            this.getLookControl().lookAt(this.getTarget(),360,360);
        }
        if(!this.getWorld().isClient()) {
            stafftimer++;
            twirltimer++;
            dashtimer++;
            dash_attack_timer++;
            cooldown++;
            this.parryTimer--;
        }


        if(!this.getWorld().isClient() && stafftimer > 300 && !this.performing && this.getTarget() != null ) {
            this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,1,1);

            ((TemplarEntity)this).triggerAnim("actions","staff");
            ((WorldScheduler) this.getWorld()).schedule(160, () -> {
                this.performing = false;
                this.is_staff = false;

                    }
            );
            var spell = SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID, "divine_fall"));
            var id = Identifier.of(RPGMinibosses.MOD_ID, "divine_fall");
            this.getNavigation().stop();
            for(int i = 0 ; i < 5; i++) {
                ((WorldScheduler) this.getWorld()).schedule(20*(i+1), () -> {
                            if (this.getTarget() != null && this.canSee(this.getTarget())) {

                                SpellHelper.ImpactContext context = new SpellHelper.ImpactContext(1.0F, 1.0F, this.getTarget().getPos(), SpellPower.getSpellPower(SpellSchools.HEALING, this), TargetHelper.TargetingMode.DIRECT);
                                SoundHelper.playSound(this.getWorld(), this, new Sound("spell_engine:generic_healing_release"));
                                Vec3d pos = this.getTarget().getBoundingBox().getCenter();
                                ((WorldScheduler) this.getWorld()).schedule(25, () -> {

                                            if (this.getTarget() != null) {


                                                for (Entity entity : this.getWorld().getOtherEntities(this, Box.of(pos, 6, 6, 6))) {
                                                    if (entity instanceof LivingEntity living && raycastObstacleFree(living, pos, living.getBoundingBox().getCenter())) {
                                                        SpellHelper.performImpacts(this.getWorld(), this, this.getTarget(), this.getTarget(), new SpellInfo(spell, id),
                                                                context, false);
                                                    }
                                                }
                                                sendBatches(this.getTarget(), SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID, "holy_burst")).area_impact.particles, pos,1, PlayerLookup.tracking(this.getTarget()) ,true);
                                                if(this.getTarget() instanceof PlayerEntity player) {
                                                 sendBatches(this.getTarget(), SpellRegistry.getSpell(Identifier.of(RPGMinibosses.MOD_ID, "holy_burst")).area_impact.particles, pos,1, List.of((ServerPlayerEntity) player),true);

                                                }
                                            }
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
        if(this.is_staff){
            this.getNavigation().stop();

        }

        super.mobTick();

    }
    public static void sendBatches(Entity trackedEntity, ParticleBatch[] batches, Vec3d pos, float countMultiplier, Collection<ServerPlayerEntity> trackers, boolean includeSourceEntity) {
        if (batches != null && batches.length != 0) {
            int sourceEntityId = trackedEntity.getId();
            Packets.ParticleBatches.SourceType sourceType = Packets.ParticleBatches.SourceType.COORDINATE;
            ArrayList<Packets.ParticleBatches.Spawn> spawns = new ArrayList();
            ParticleBatch[] var8 = batches;
            int var9 = batches.length;

            for(int var10 = 0; var10 < var9; ++var10) {
                ParticleBatch batch = var8[var10];
                Vec3d sourceLocation = pos;


                spawns.add(new Packets.ParticleBatches.Spawn(includeSourceEntity ? sourceEntityId : 0, trackedEntity.getYaw(), trackedEntity.getPitch(), sourceLocation, batch));
            }

            PacketByteBuf packet = (new Packets.ParticleBatches(sourceType, spawns)).write(countMultiplier);
            if (trackedEntity instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity)trackedEntity;
                sendWrittenBatchesToPlayer(serverPlayer, packet);
            }

            trackers.forEach((serverPlayerx) -> {
                sendWrittenBatchesToPlayer(serverPlayerx, packet);
            });
        }
    }
    private static Vec3d origin(Entity entity, ParticleBatch.Origin origin) {
        switch (origin) {
            case FEET:
                return entity.getPos().add(0.0, (double)(entity.getHeight() * 0.1F), 0.0);
            case CENTER:
                return entity.getPos().add(0.0, (double)(entity.getHeight() * 0.5F), 0.0);
            case LAUNCH_POINT:
                if (entity instanceof LivingEntity livingEntity) {
                    return SpellHelper.launchPoint(livingEntity);
                }

                return entity.getPos().add(0.0, (double)(entity.getHeight() * 0.5F), 0.0);
            default:
                return entity.getPos();
        }
    }


    private PlayState predicateTemplar(AnimationState<MinibossEntity> state) {
        if(this.isAttacking()){
            if(this.getVelocity().length() > 0.2F){
                state.setAnimation(SPRINT_AGGRO);
            }
            else{
                state.setAnimation(AGGRO_TEMPLAR);

            }
            return PlayState.CONTINUE;
        }
        return PlayState.CONTINUE;

    }

    public static final RawAnimation IDLE_AGGRO = RawAnimation.begin().thenPlay("animation.unknown.idle");
    public static final RawAnimation SPRINT_AGGRO = RawAnimation.begin().thenPlay("animation.templar.heavy_run");

    public static final RawAnimation AGGRO_TEMPLAR = RawAnimation.begin().thenPlay("animation.mob.walk_templar");

    private static void sendWrittenBatchesToPlayer(ServerPlayerEntity serverPlayer, PacketByteBuf packet) {
        try {
            if (ServerPlayNetworking.canSend(serverPlayer, Packets.ParticleBatches.ID)) {
                ServerPlayNetworking.send(serverPlayer, Packets.ParticleBatches.ID, packet);
            }
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }
    private static BlockHitResult raycastObstacle(Entity entity, Vec3d start, Vec3d end) {
        return entity.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
    }

    static boolean raycastObstacleFree(Entity entity, Vec3d start, Vec3d end) {
        BlockHitResult hit = raycastObstacle(entity, start, end);
        return hit.getType() != HitResult.Type.BLOCK;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if(!performing && this.parryTimer <= 0 ) {

            if(source.getAttacker() != null && source.getAttacker() instanceof LivingEntity && !source.isIndirect() && source.getAttacker().distanceTo(this) < 2+ sqrt( this.squaredAttackRange((LivingEntity) source.getAttacker()))){
                this.tryAttack(source.getAttacker());
                this.parryTimer =  (int)(160*this.getCooldownCoeff());
                amount *= 0.5F;
                SoundHelper.playSoundEvent(this.getWorld(),this, SoundEvents.BLOCK_ANVIL_PLACE);
            }
        }

        return super.damage(source, amount);
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
        if(!performing && target instanceof LivingEntity living) {
            if(this.getWorld() instanceof ServerWorld serverWorld) {
                spawnParticlesSlash(this, serverWorld, 180 + (swingBool ? 60F : -60F)+this.getRandom().nextBetween(0,60), 1, 2F+(float) +Math.sqrt((float) this.squaredAttackRange(living)));

            }
            if (swingBool) {
                (this).triggerAnim("actions", "swing1");
                swingBool = false;

            } else {
                (this).triggerAnim("actions", "swing2");
                swingBool = true;

            }
        }
        return super.tryAttack(target);

    }

}
