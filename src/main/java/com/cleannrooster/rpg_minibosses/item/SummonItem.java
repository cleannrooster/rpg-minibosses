package com.cleannrooster.rpg_minibosses.item;


import com.cleannrooster.rpg_minibosses.entity.GeminiEntity;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.GoatHornItem;
import net.minecraft.item.Instrument;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SummonItem<T extends LivingEntity> extends Item {
    private  List<EntityType<T>> entityTypelist;
    public SummonItem(Settings settings, TagKey<Instrument> instrumentTag) {
        super(settings);
    }


    public SummonItem(Settings settings, List<EntityType<T>> typelist, TagKey<Instrument> instrumentTag) {
        super(settings);
        this.entityTypelist = typelist;
    }

    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
            user.setCurrentHand(hand);
            user.getItemCooldownManager().set(this, 20);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            return TypedActionResult.consume(itemStack);

    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {

        super.onStoppedUsing(stack, world, user, remainingUseTicks);
            if (world instanceof ServerWorld level1 && user instanceof PlayerEntity player) {

                if (level1.getEntitiesByType(TypeFilter.instanceOf(GeminiEntity.class), archmagus -> archmagus.distanceTo(player) < 200).isEmpty()) {
                    for (int i = 0; i < 10; i++) {
                        BlockPos vec3 = getSafePositionAroundPlayer2(world, player.getSteppingPos(), 10);
                        if (vec3 != null && world.isSkyVisible(vec3.up()) && !world.isClient()) {
                            for (EntityType<T> type : entityTypelist) {

                                T magus = type.create(world);
                                magus.setPosition(vec3.getX(), vec3.getY(), vec3.getZ());


                                magus.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, player.getPos());
                                world.spawnEntity(magus);
                            }
                                stack.decrement(1);
                            user.playSound(this.getBreakSound());

                            player.sendMessage(Text.translatable("A dark force has been unleashed!"));


                            return;
                        }
                    }
                    player.sendMessage(Text.translatable("There is no room at your location"));
                } else {
                    player.sendMessage(Text.translatable("A dark force is already present within 200 blocks."));
                }
            }
        }

    @Override
    public SoundEvent getBreakSound() {
        return super.getBreakSound();
    }

    static @Nullable BlockPos getSafePositionAroundPlayer2(World level, BlockPos pos, int range) {
        if (range == 0) {
            return null;
        } else {
            BlockPos safestPos = null;

            for(int attempts = 0; attempts < 1; ++attempts) {
                int a = -1;
                boolean b = true;
                int c = -1;
                if (level.getRandom().nextBoolean()) {
                    a = 1;
                }

                if (level.getRandom().nextBoolean()) {
                    b = true;
                }

                if (level.getRandom().nextBoolean()) {
                    c = 1;
                }

                int posX = pos.getX() + a * level.getRandom().nextInt(10);
                int posY = pos.getY() + level.getRandom().nextInt(10) - 5;
                int posZ = pos.getZ() + c * level.getRandom().nextInt(10);
                BlockPos testPos = findGround(level, new BlockPos(posX, posY, posZ));
                if (testPos != null) {
                    safestPos = testPos;
                    break;
                }
            }

            return safestPos;
        }
    }
    private static @Nullable BlockPos findGround(World level, BlockPos pos) {
        BlockPos downPos;
        if (level.getBlockState(pos).isAir()) {
            for(downPos = pos; World.isValid(downPos.down()) && level.getBlockState(downPos.down()).isAir() && downPos.down().isWithinDistance(pos, 20.0); downPos = downPos.down()) {
            }

            if (!level.getBlockState(downPos.down()).isAir()) {
                return downPos;
            }
        } else {
            for(downPos = pos; World.isValid(downPos.up()) && !level.getBlockState(downPos.up()).isAir() && downPos.up().isWithinDistance(pos, 20.0); downPos = downPos.up()) {
            }

            if (!level.getBlockState(downPos.up()).isAir()) {
                return downPos;
            }
        }

        return null;
    }
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("Use to summon Gemini, if available."));

        super.appendTooltip(stack, context, tooltip, type);
    }
}
