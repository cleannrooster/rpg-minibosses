package com.cleannrooster.rpg_minibosses.item;


import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SummonHorn<T extends LivingEntity> extends GoatHornItem {
    private  EntityType<T> entityType;
    public SummonHorn(Item.Settings settings, TagKey<Instrument> instrumentTag) {
        super(settings, instrumentTag);
    }


    public SummonHorn(Settings settings, EntityType<T> type, TagKey<Instrument> instrumentTag) {
        super(settings,instrumentTag);
        this.entityType = type;
    }


    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {

        super.onStoppedUsing(stack, world, user, remainingUseTicks);
        if(world instanceof ServerWorld level1  && user instanceof PlayerEntity player) {
            if (RPGMinibossesEntities.config.magus) {
                if (level1.getEntitiesByType(TypeFilter.instanceOf(MagusPrimeEntity.class), archmagus -> archmagus.distanceTo(player) < 200).isEmpty()) {
                    for (int i = 0; i < 10; i++) {
                        BlockPos vec3 = getSafePositionAroundPlayer2(world, player.getSteppingPos(), 10);
                        if (vec3 != null && world.isSkyVisible(vec3.up()) && !world.isClient()) {
                            T magus = this.entityType.create(world);
                            magus.setPosition(vec3.getX(), vec3.getY(), vec3.getZ());


                            magus.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, player.getPos());
                            world.spawnEntity(magus);

                            stack.damage(1, player, (c) -> {
                            });
                            player.sendMessage(Text.translatable("Magus has been unleashed!"));

                            return;
                        }
                    }
                    player.sendMessage(Text.translatable("Magus has no room at your location"));
                } else {
                    player.sendMessage(Text.translatable("Magus is already present within 200 blocks."));
                }
            }
            else{
                stack.damage(1, player, (c) -> {
                });
                player.sendMessage(Text.translatable("text.rpg-minibosses.lavos_horn"),true);
                player.addExperience(400);
            }
        }
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
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        if(RPGMinibossesEntities.config.magus) {
            tooltip.add(Text.translatable("Use to summon Forsaken Magus, if available."));
        }
        else {
            tooltip.add(Text.translatable("text.rpg-minibosses.lavos_horn_2"));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }
}
