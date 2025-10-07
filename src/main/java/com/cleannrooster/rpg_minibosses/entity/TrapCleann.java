package com.cleannrooster.rpg_minibosses.entity;


import com.google.gson.Gson;
import mod.azure.azurelib.animatable.GeoEntity;

import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.azurelib.util.AzureLibUtil;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.TargetHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static net.spell_engine.utils.TargetHelper.actionAllowed;

public class TrapCleann extends Explosive implements GeoEntity {

    private boolean shotprojectile;

    public TrapCleann(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }
    public TrapCleann(EntityType<? extends PersistentProjectileEntity> entityType, Entity owner, World world, Identifier spellId, SpellHelper.ImpactContext context) {
        super(entityType, owner,world,spellId,context);
        this.setOwner(owner);
        this.spellId = spellId;
        this.context = context;
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
        return caster.getPos().add(0.0, 0.1, 0.0).add(look);
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


    @Override
    public void tick() {
        if(firstUpdate) {
            this.triggerAnim("drop", "drop");

        }
        if(this.age > 40 && !this.getWorld().isClient()){
            if(this.age > 40 && !this.getWorld().isClient()){
                if(this.getOwner() instanceof LivingEntity player) {

                    if (this.getSpellEntry() != null && this.context != null) {
                        ArrayList<Entity> targets = new ArrayList<>(TargetHelper.targetsFromArea(this, this.getBoundingBox().getCenter(), 2, new Spell.Release.Target.Area(), (entity) ->actionAllowed(TargetHelper.TargetingMode.AREA, TargetHelper.Intent.HARMFUL,(LivingEntity) this.getOwner(),entity)));
                        if (!targets.isEmpty()) {
                            if(perhapsExplode(targets,player)){
                                discard();
                            };
                        }
                    }
                }
            }
        }
        if(this.age > 160 && !this.getWorld().isClient()){
            this.discard();
        }
            super.tick();
    }
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "drop", event -> PlayState.CONTINUE)
                        .triggerableAnim("drop", DROP));

    }
    private AnimatableInstanceCache factory = mod.azure.azurelib.util.AzureLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    public double getTick(Object object) {
        return mod.azure.azurelib.util.RenderUtils.getCurrentTick();
    }
}
