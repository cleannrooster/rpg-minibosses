package com.cleannrooster.anarchy_rpg.client.fx;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ground a cloud lies on, column by column: a flood outward from where it landed over the surface a
 * player would walk on, so the overlay follows steps and stops at walls and drops instead of painting a
 * disc through them. A cell's flood distance is when the overlay reaches it, so it washes outward from
 * the middle. Client only, worked out once per cloud.
 */
public final class GroundPatch {
    /** One column the cloud covers: where its overlay is drawn, and how many steps from the middle. */
    public record Cell(int x, int z, float y, int distance) {
    }

    /** Clearance above the floor, enough that the overlay does not z-fight it when seen from far off. */
    private static final double LIFT = 0.05;
    /** How far up or down the flood may step between neighbouring columns. */
    private static final int STEP_UP = 1;
    private static final int STEP_DOWN = 3;
    /** Thin cover - snow, carpet - is drawn on top of; anything taller is a different floor. */
    private static final double MAX_COVER = 0.5;
    private static final int MAX_CELLS = 512;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private final List<Cell> cells;
    private final Map<Long, Cell> byColumn;

    private GroundPatch(List<Cell> cells, Map<Long, Cell> byColumn) {
        this.cells = cells;
        this.byColumn = byColumn;
    }

    public List<Cell> cells() {
        return this.cells;
    }

    public boolean covers(int x, int z) {
        return this.byColumn.containsKey(key(x, z));
    }

    /** Every walkable column within {@code radius} of {@code center} that the flood reaches. */
    public static GroundPatch flood(BlockView world, Vec3d center, double radius) {
        List<Cell> cells = new ArrayList<>();
        Map<Long, Cell> byColumn = new HashMap<>();
        int ox = MathHelper.floor(center.x);
        int oz = MathHelper.floor(center.z);
        Float top = floor(world, ox, MathHelper.floor(center.y + 0.5), oz);
        if (top == null) {
            return new GroundPatch(cells, byColumn);
        }
        var start = new Cell(ox, oz, top, 0);
        var frontier = new ArrayDeque<Cell>();
        frontier.add(start);
        byColumn.put(key(ox, oz), start);
        double reach = radius * radius;
        while (!frontier.isEmpty() && cells.size() < MAX_CELLS) {
            var cell = frontier.poll();
            cells.add(cell);
            for (Direction side : SIDES) {
                int x = cell.x() + side.getOffsetX();
                int z = cell.z() + side.getOffsetZ();
                double dx = x + 0.5 - center.x;
                double dz = z + 0.5 - center.z;
                if (dx * dx + dz * dz > reach || byColumn.containsKey(key(x, z))) {
                    continue;
                }
                // The block the neighbour's floor belongs to, stepping from this one's.
                Float next = floor(world, x, MathHelper.ceil(cell.y() - LIFT - 1.0e-3) - 1, z);
                if (next == null) {
                    continue;
                }
                var reached = new Cell(x, z, next, cell.distance() + 1);
                byColumn.put(key(x, z), reached);
                frontier.add(reached);
            }
        }
        return new GroundPatch(List.copyOf(cells), byColumn);
    }

    /**
     * The height the overlay is drawn at in this column, near block {@code y}: the top of the highest
     * solid block within the step range that has room above it, plus any thin cover lying on it.
     */
    private static @Nullable Float floor(BlockView world, int x, int y, int z) {
        var pos = new BlockPos.Mutable();
        for (int at = y + STEP_UP; at >= y - STEP_DOWN; at--) {
            pos.set(x, at, z);
            VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
            if (shape.isEmpty()) {
                continue;
            }
            double top = at + shape.getMax(Direction.Axis.Y);
            pos.set(x, at + 1, z);
            if (top > at + 1.0 || !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                // A fence or wall, or something with no headroom: not a floor.
                return null;
            }
            return (float) (top + cover(world, pos, top) + LIFT);
        }
        return null;
    }

    /** How far above {@code top} the thin cover in {@code above} reaches - snow and carpet have little or no collision. */
    private static double cover(BlockView world, BlockPos above, double top) {
        VoxelShape outline = world.getBlockState(above).getOutlineShape(world, above);
        if (outline.isEmpty()) {
            return 0.0;
        }
        double rise = above.getY() + outline.getMax(Direction.Axis.Y) - top;
        return rise > 0.0 && rise <= MAX_COVER ? rise : 0.0;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
