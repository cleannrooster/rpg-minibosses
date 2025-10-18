package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;



public class OrbEntity extends Entity implements GeoEntity, Ownable {

    public static final RawAnimation SPINNING = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation INTRO = RawAnimation.begin().thenLoop("intro");

    private Entity owner;
    @Nullable
    private int ownerUuid;
    public Entity target;
    public SpellHelper.ImpactContext context;
    private boolean performing;

    public OrbEntity(EntityType<? extends OrbEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.noClip = true;
    }

    public static final TrackedData<Integer> COLOR;
    public static final TrackedData<Integer> OWNER;

    static {
        COLOR = DataTracker.registerData(OrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
        OWNER = DataTracker.registerData(OrbEntity.class, TrackedDataHandlerRegistry.INTEGER);

    }



    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(COLOR, 1);
        builder.add(OWNER, -1);

    }



    public int getColor(){
        return this.dataTracker.get(COLOR);
    }
    public void setColor(int color){
         this.dataTracker.set(COLOR,color);
    }
    @Override
    public void tick() {
        if(this.age % 40 == 0) {
            this.playSound(SoundEvents.ENTITY_BLAZE_AMBIENT,1,0.5F);
        }
            if(this.firstUpdate) {
                this.playSound(SoundEvents.ENTITY_BLAZE_AMBIENT,1,0.5F);

                if (!this.getWorld().isClient()) {
                ((WorldScheduler) this.getWorld()).schedule(40, () -> {
                            this.performing = false;

                        }
                );
                this.performing = true;
            }
        }
        if(!this.getWorld().isClient() &&!this.performing && this.age % 10 == 0 && this.getOwner() instanceof LivingEntity livingEntity){
            Identifier id = Identifier.of(RPGMinibosses.MOD_ID,"dark_matter");

            Spell spell = SpellRegistry.from(this.getWorld()).get(id);
            Optional<RegistryEntry.Reference<Spell>> spellReference = SpellRegistry.from(this.getWorld()).getEntry(id);

            List<LivingEntity> list = this.getWorld().getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class),this.getBoundingBox(), target -> target != this.getOwner());
            for(LivingEntity living : list){
                boolean bool = SpellHelper.performImpacts(this.getWorld(),livingEntity,living,living,spellReference.get(),spellReference.get().value().impacts,new SpellHelper.ImpactContext().power(SpellPower.getSpellPower( SpellSchools.SOUL,livingEntity)).position(this.getPos()));
            }
        }
        if(this.age == 240 && !this.getWorld().isClient()){
            this.discard();
        }
        super.tick();
    }

    @Override
    public boolean canHit() {
        return false;
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar animationData) {
        animationData.add(
                new AnimationController<>(this, "intro", event -> PlayState.CONTINUE)
                        .triggerableAnim("intro", INTRO));
        animationData.add(new AnimationController<>(this, "fly",
                0, this::predicate2)
        );
    }
    private <E extends GeoAnimatable> PlayState predicate2(AnimationState<E> event) {

            return event.setAndContinue(SPINNING);
    }
    private AnimatableInstanceCache factory = AzureLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }
    public void setOwner(@Nullable Entity entity) {
        if (entity != null) {
            this.ownerUuid = entity.getId();
            this.owner = entity;
            this.dataTracker.set(OWNER,entity.getId());
        }
    }
    @Nullable
    public Entity getOwner() {
        if(this.dataTracker.get(OWNER) != -1) {
            return this.getWorld().getEntityById(this.dataTracker.get(OWNER));
        }else{
            return null;
        }
    }
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Color", (Integer) this.dataTracker.get(COLOR));
            nbt.putInt("Owner", this.ownerUuid);


    }
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("Color")) {
            this.dataTracker.set(COLOR, nbt.getInt("Color"));
        }

        if (nbt.containsUuid("Owner")) {
            this.ownerUuid = nbt.getInt("Owner");
            this.owner = null;
        }

    }
}
