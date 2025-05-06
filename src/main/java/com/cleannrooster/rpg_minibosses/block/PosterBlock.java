package com.cleannrooster.rpg_minibosses.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class PosterBlock extends WallMountedBlock {
    public static final MapCodec<PosterBlock> CODEC = createCodec(PosterBlock::new);
    public static final int MAX_LAYERS = 8;

    public static final int field_31248 = 5;

    public MapCodec<PosterBlock> getCodec() {
        return CODEC;
    }

    public PosterBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING,  FACE});
    }

    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = (Direction)state.get(FACING);
        switch ((BlockFace)state.get(FACE)) {
            case FLOOR:
                if (direction.getAxis() == Direction.Axis.X) {
                    return FLOOR_X_SHAPE;
                }

                return  FLOOR_Z_SHAPE;
            case WALL:
                VoxelShape var10000;
                switch (direction) {
                    case EAST:
                        var10000 = EAST_SHAPE;
                        break;
                    case WEST:
                        var10000 = WEST_SHAPE;
                        break;
                    case SOUTH:
                        var10000 =  SOUTH_SHAPE;
                        break;
                    case NORTH:
                    case UP:
                    case DOWN:
                        var10000 = NORTH_SHAPE;
                        break;
                    default:
                        throw new MatchException((String)null, (Throwable)null);
                }

                return var10000;
            case CEILING:
            default:
                if (direction.getAxis() == Direction.Axis.X) {
                    return CEILING_X_SHAPE;
                } else {
                    return CEILING_Z_SHAPE;
                }
        }
    }
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = (Direction)state.get(FACING);
        switch ((BlockFace)state.get(FACE)) {
            case FLOOR:
                if (direction.getAxis() == Direction.Axis.X) {
                    return FLOOR_X_SHAPE;
                }

                return  FLOOR_Z_SHAPE;
            case WALL:
                VoxelShape var10000;
                switch (direction) {
                    case EAST:
                        var10000 = EAST_SHAPE;
                        break;
                    case WEST:
                        var10000 = WEST_SHAPE;
                        break;
                    case SOUTH:
                        var10000 =  SOUTH_SHAPE;
                        break;
                    case NORTH:
                    case UP:
                    case DOWN:
                        var10000 = NORTH_SHAPE;
                        break;
                    default:
                        throw new MatchException((String)null, (Throwable)null);
                }

                return var10000;
            case CEILING:
            default:
                if (direction.getAxis() == Direction.Axis.X) {
                    return CEILING_X_SHAPE;
                } else {
                    return CEILING_Z_SHAPE;
                }
        }    }

    protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        Direction direction = (Direction)state.get(FACING);
        switch ((BlockFace)state.get(FACE)) {
            case FLOOR:
                if (direction.getAxis() == Direction.Axis.X) {
                    return FLOOR_X_SHAPE;
                }

                return  FLOOR_Z_SHAPE;
            case WALL:
                VoxelShape var10000;
                switch (direction) {
                    case EAST:
                        var10000 = EAST_SHAPE;
                        break;
                    case WEST:
                        var10000 = WEST_SHAPE;
                        break;
                    case SOUTH:
                        var10000 =  SOUTH_SHAPE;
                        break;
                    case NORTH:
                    case UP:
                    case DOWN:
                        var10000 = NORTH_SHAPE;
                        break;
                    default:
                        throw new MatchException((String)null, (Throwable)null);
                }

                return var10000;
            case CEILING:
            default:
                if (direction.getAxis() == Direction.Axis.X) {
                    return CEILING_X_SHAPE;
                } else {
                    return CEILING_Z_SHAPE;
                }
        }    }


    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = (Direction)state.get(FACING);
        switch ((BlockFace)state.get(FACE)) {
            case FLOOR:
                if (direction.getAxis() == Direction.Axis.X) {
                    return FLOOR_X_SHAPE;
                }

                return  FLOOR_Z_SHAPE;
            case WALL:
                VoxelShape var10000;
                switch (direction) {
                    case EAST:
                        var10000 = EAST_SHAPE;
                        break;
                    case WEST:
                        var10000 = WEST_SHAPE;
                        break;
                    case SOUTH:
                        var10000 =  SOUTH_SHAPE;
                        break;
                    case NORTH:
                    case UP:
                    case DOWN:
                        var10000 = NORTH_SHAPE;
                        break;
                    default:
                        throw new MatchException((String)null, (Throwable)null);
                }

                return var10000;
            case CEILING:
            default:
                if (direction.getAxis() == Direction.Axis.X) {
                    return CEILING_X_SHAPE;
                } else {
                    return CEILING_Z_SHAPE;
                }
        }    }

    protected boolean hasSidedTransparency(BlockState state) {
        return true;
    }

    protected static final VoxelShape CEILING_X_SHAPE;
    protected static final VoxelShape CEILING_Z_SHAPE;
    protected static final VoxelShape FLOOR_X_SHAPE;
    protected static final VoxelShape FLOOR_Z_SHAPE;
    protected static final VoxelShape NORTH_SHAPE;
    protected static final VoxelShape SOUTH_SHAPE;
    protected static final VoxelShape WEST_SHAPE;
    protected static final VoxelShape EAST_SHAPE;

    static {
        CEILING_X_SHAPE = Block.createCuboidShape(0, 14.0, 3, 16, 16.0, 13);
        CEILING_Z_SHAPE = Block.createCuboidShape(3, 14.0, 0, 13, 16.0, 16);
        FLOOR_X_SHAPE = Block.createCuboidShape(0, 0.0, 3, 16, 2.0, 13);
        FLOOR_Z_SHAPE = Block.createCuboidShape(3, 0.0, 0, 13, 2.0, 15);
        NORTH_SHAPE = Block.createCuboidShape(3.0, 0, 14, 13, 16, 16);
        SOUTH_SHAPE = Block.createCuboidShape(3.0, 0, 0, 13, 16, 2);
        WEST_SHAPE = Block.createCuboidShape(14, 0, 3, 16, 16, 13);
        EAST_SHAPE = Block.createCuboidShape(0, 0, 3, 2.0, 16, 13);
    }



}
