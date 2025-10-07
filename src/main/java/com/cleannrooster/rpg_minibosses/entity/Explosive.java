package com.cleannrooster.rpg_minibosses.entity;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.google.gson.Gson;
import mod.azure.azurelib.animatable.GeoEntity;

import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.util.AzureLibUtil;
import mod.azure.azurelib.util.RenderUtils;
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
import net.spell_engine.api.spell.SpellInfo;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.internals.WorldScheduler;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.TargetHelper;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.spell_engine.utils.TargetHelper.actionAllowed;


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
    public Spell getSpellEntry() {

        return net.spell_engine.internals.SpellRegistry.getSpell(this.spellId);
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



    @Nullable
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return ProjectileUtil.getEntityCollision(this.getWorld(), this, currentPosition, nextPosition, this.getBoundingBox().expand(1.0), this::canHit);
    }
    @Override
    protected boolean canHit(Entity entity) {

        return this.getOwner() instanceof LivingEntity player && actionAllowed(TargetHelper.TargetingMode.AREA, TargetHelper.Intent.HARMFUL,player,entity)&&this.age > 40;
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
            if(this.getSpellEntry() != null) {
                Spell spell = this.getSpellEntry();
                Vec3d launchPoint = launchPoint(caster, 0.5F);
                Spell.Release.Target.ShootProjectile data = spell.release.target.projectile;
                Spell.ProjectileData projectileData = data.projectile;
                Spell.ProjectileData.Perks mutablePerks = projectileData.perks.copy();
                SpellProjectile projectile = null;
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
                SoundHelper.playSound(world, projectile, spell.release.sound);
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

        if(this.getSpellEntry() == null || this.getOwner() == null){
            if(!this.getWorld().isClient()) {
                this.discard();
            }
        }
        super.tick();
    }
    public boolean  perhapsExplode(List<Entity> targets,LivingEntity player){

                    if(!this.getWorld().isClient() && this.spellId.getNamespace().equals(RPGMinibosses.MOD_ID)  && this.getSpellEntry() != null) {

                        SpellHelper.lookupAndPerformAreaImpact(this.getSpellEntry().area_impact,new SpellInfo(this.getSpellEntry(),new Identifier(RPGMinibosses.MOD_ID,"explosion")), (LivingEntity) this.getOwner(),this,this,this.getImpactContext().position(this.getPos()),false);
                        net.spell_engine.particle.ParticleHelper.sendBatches(this, this.getSpellEntry().release.particles);
                        return true;
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
