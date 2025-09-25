package com.cleannrooster.rpg_minibosses.entity;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.google.gson.Gson;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.client.util.RenderUtils;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.spell_engine.internals.target.EntityRelations.actionAllowed;

public class Explosive extends PersistentProjectileEntity implements GeoEntity {

    private boolean shotprojectile;

    public Explosive(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }
    public Explosive(EntityType<? extends PersistentProjectileEntity> entityType, Entity owner, World world, Identifier spellId, SpellHelper.ImpactContext context) {
        super(entityType, world);
        this.setOwner(owner);
        this.spellId = spellId;
        this.context = context;
    }
    public Optional<RegistryEntry.Reference<Spell>> getSpellEntry() {

        return SpellRegistry.from(this.getWorld()).getEntry(this.spellId);
    }


    public SpellHelper.ImpactContext getImpactContext() {
        return this.context;
    }
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        Gson gson = new Gson();
        nbt.putString(NBT_SPELL_ID, this.spellId.toString());
        nbt.putString(NBT_IMPACT_CONTEXT, gson.toJson(this.context));
    }
    private Identifier spellId;
    private SpellHelper.ImpactContext context;
    private static String NBT_SPELL_ID = "Spell.ID";
    private static String NBT_PERKS = "Perks";
    private static String NBT_IMPACT_CONTEXT = "Impact.Context";
    private static String NBT_ITEM_MODEL_ID = "Item.Model.ID";
    private boolean channeling = false;
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(NBT_SPELL_ID, 8)) {
            try {
                Gson gson = new Gson();
                this.spellId = Identifier.tryParse(nbt.getString(NBT_SPELL_ID));
                this.context = (SpellHelper.ImpactContext)gson.fromJson(nbt.getString(NBT_IMPACT_CONTEXT), SpellHelper.ImpactContext.class);

            } catch (Exception var3) {
                System.err.println("SpellProjectile - Failed to read spell data from NBT " + var3.getMessage());
            }
        }

    }
    @Override
    protected ItemStack asItemStack() {
        return Items.ARROW.getDefaultStack();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return Items.ARROW.getDefaultStack();
    }

    @Nullable
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return ProjectileUtil.getEntityCollision(this.getWorld(), this, currentPosition, nextPosition, this.getBoundingBox().expand(1.0), this::canHit);
    }
    @Override
    protected boolean canHit(Entity entity) {

        return this.getOwner() instanceof LivingEntity player && actionAllowed(SpellTarget.FocusMode.AREA, SpellTarget.Intent.HARMFUL,player,entity)&&this.age > 40;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {

    }
    public static final RawAnimation DROP =  RawAnimation.begin().thenPlayAndHold("animation.model.new");


    public static Vec3d launchPoint(Entity caster, float forward) {
        Vec3d look = caster.getRotationVector().multiply((double)(forward));
        return caster.getPos().add(0.0, 0.0, 0.0).add(look);
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    public void shootProjectile(World world, Entity caster, Entity target, Spell spellInfo, SpellHelper.ImpactContext context, int sequenceIndex) {
        if (!world.isClient) {
            if(target != null) {
                this.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, target.getBoundingBox().getCenter());
            }
            else{
                if(this.getOwner() != null) {
                    this.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, this.getOwner().getBoundingBox().getCenter());
                }

            }
            if(this.getSpellEntry().isPresent()) {
                Spell spell = this.getSpellEntry().get().value();
                Vec3d launchPoint = launchPoint(caster, 0.5F);
                Spell.Delivery.ShootProjectile data = spell.deliver.projectile;
                Spell.ProjectileData projectileData = data.projectile;
                Spell.ProjectileData.Perks mutablePerks = projectileData.perks.copy();
                SpellProjectile projectile = new SpellProjectile(world, (LivingEntity) this.getOwner(), launchPoint.getX(), launchPoint.getY(), launchPoint.getZ(), SpellProjectile.Behaviour.FLY, this.getSpellEntry().get(), context, mutablePerks);
                Spell.LaunchProperties mutableLaunchProperties = data.launch_properties.copy();


                float velocity = mutableLaunchProperties.velocity;
                float divergence = projectileData.divergence;
                if (data.inherit_shooter_velocity) {
                    projectile.setVelocity(caster, caster.getPitch(), caster.getYaw(), 0.0F, velocity, divergence);
                } else {
                    Vec3d look = caster.getRotationVector().normalize();
                    projectile.setVelocity(look.x, look.y, look.z, velocity, divergence);
                }

                projectile.range = spell.range;

                projectile.setPitch(caster.getPitch());
                projectile.setYaw(caster.getYaw());
                world.spawnEntity(projectile);
                SoundHelper.playSound(world, projectile, mutableLaunchProperties.sound);
                if (sequenceIndex == 0 && mutableLaunchProperties.extra_launch_count > 0) {
                    for (int i = 0; i < mutableLaunchProperties.extra_launch_count; ++i) {
                        int ticks = (i + 1) * mutableLaunchProperties.extra_launch_delay;
                        int nextSequenceIndex = i + 1;
                        ((WorldScheduler) world).schedule(ticks, () -> {
                            if (caster != null && caster.isAlive()) {
                                shootProjectile(world, caster, target, spellInfo, context, nextSequenceIndex);
                            }
                        });
                    }


                    ((WorldScheduler) world).schedule(mutableLaunchProperties.extra_launch_count * mutableLaunchProperties.extra_launch_delay + 1, this::discard);

                }
            }
        }
    }
    @Override
    public void tick() {

        if(this.getSpellEntry().isEmpty() || this.getOwner() == null){
            if(!this.getWorld().isClient()) {
                this.discard();
            }
        }
        super.tick();
    }
    public boolean  perhapsExplode(List<Entity> targets,LivingEntity player){

                    if(!this.getWorld().isClient() && this.spellId.getNamespace().equals(RPGMinibosses.MOD_ID)  && this.getSpellEntry().isPresent() && this.getSpellEntry().get().value().active != null) {

                        if (this.getSpellEntry().get().value().active.cast.channel_ticks > 0) {
                            this.channeling = true;
                        } else if(this.getSpellEntry().get().value().deliver.type.equals(Spell.Delivery.Type.PROJECTILE) && !this.shotprojectile){
                            List<Entity> list = TargetHelper.targetsFromArea(this,this.getPos(),6,new Spell.Target.Area(),
                                    (target) ->  actionAllowed(SpellTarget.FocusMode.AREA, SpellTarget.Intent.HARMFUL,player, target) && target instanceof LivingEntity);
                            ArrayList<LivingEntity> list1 = new ArrayList<>();
                            list.forEach(target -> {if(target instanceof LivingEntity living){
                            list1.add(living);}
                            });
                            Entity entity = null;
                            if(!list.isEmpty()) {
                                 entity = this.getWorld().getClosestEntity(list1, TargetPredicate.DEFAULT,null,this.getX(),this.getY(),this.getZ());

                            }
                            shootProjectile(this.getWorld(),this,entity,this.getSpellEntry().get().value(),this.getImpactContext().position(this.getPos()),0);
                            this.shotprojectile = true;
                        } else if (!shotprojectile && this.getSpellEntry().get().value().deliver.type.equals(Spell.Delivery.Type.METEOR)){
                            int i = 0;
                            Entity entity = null;
                            if(!targets.isEmpty()){
                                i = this.random.nextInt(targets.size());
                                entity = targets.get(i);
                            }
                            SpellHelper.fallProjectile(this.getWorld(),player,entity,this.getPos(),getSpellEntry().get(),this.getImpactContext().position(this.getPos()));
                            return true;
                        }else if (!this.shotprojectile && this.getSpellEntry().get().value().area_impact != null) {
                            boolean bool = SpellHelper.lookupAndPerformAreaImpact(this.getSpellEntry().get().value().area_impact, getSpellEntry().get(), player, this, this,this.getSpellEntry().get().value().impacts, this.getImpactContext().position(this.getPos()), false);
                            ParticleHelper.sendBatches(this, this.getSpellEntry().get().value().release.particles);
                        return true;
                        }



        }
                    return false;
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "drop", event -> PlayState.CONTINUE)
                        .triggerableAnim("drop", DROP));

    }
    private AnimatableInstanceCache factory = AzureLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    public double getTick(Object object) {
        return RenderUtils.getCurrentTick();
    }
}
