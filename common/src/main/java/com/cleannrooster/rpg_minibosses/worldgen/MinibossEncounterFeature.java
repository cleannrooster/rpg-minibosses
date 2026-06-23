package com.cleannrooster.rpg_minibosses.worldgen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Builds one of five compact 7x7 environmental vignettes (see {@link EncounterType}) and spawns the
 * matching miniboss behind/inside it. One shared feature drives every encounter; the layout branches
 * on the configured {@link EncounterType}.
 *
 * <p>All block coordinates are authored in local space (origin = centre of the footprint, {@code ly=0}
 * is the surface block) and transformed by a random {@link BlockRotation} via {@link #placeBlock}, so
 * layouts do not always face the same way. Placement is conservative: terrain flatness, fluid coverage
 * and head-room are validated first, and every block honours {@link #canReplace} so containers, block
 * entities, portals and unbreakable blocks are never overwritten.</p>
 */
public class MinibossEncounterFeature extends Feature<MinibossEncounterConfig> {
    // ── Footprint / validation thresholds ───────────────────────────────────
    private static final int RADIUS = 3;                 // 7x7 footprint
    private static final int MAX_HEIGHT_DIFF = 3;        // ~2-3 block average tolerance
    private static final double MAX_FLUID_FRACTION = 0.25;
    private static final int HEAD_ROOM = 4;              // clear blocks above the central lane

    // ── Shared palette ──────────────────────────────────────────────────────
    private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState CRACKED = Blocks.CRACKED_STONE_BRICKS.getDefaultState();
    private static final BlockState MOSSY = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
    private static final BlockState COBBLE = Blocks.COBBLESTONE.getDefaultState();
    private static final BlockState IRON_BARS = Blocks.IRON_BARS.getDefaultState();
    private static final BlockState CHAIN = Blocks.CHAIN.getDefaultState();
    private static final BlockState FOUNDATION = Blocks.DIRT.getDefaultState();

    public MinibossEncounterFeature() {
        super(MinibossEncounterConfig.CODEC);
    }

    @Override
    public boolean generate(FeatureContext<MinibossEncounterConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        // Heightmap placement gives the first air block above the surface; the surface itself is below.
        BlockPos ground = context.getOrigin().down();
        EncounterType type = context.getConfig().encounter();

        if (!validate(world, ground)) {
            return false;
        }

        BlockRotation rotation = BlockRotation.random(random);
        levelFoundation(world, ground, rotation);

        BlockPos spawnMarker = switch (type) {
            case BROKEN_TOLLGATE -> buildBrokenTollgate(world, ground, rotation, random);
            case CONTRACT_CAMP -> buildContractCamp(world, ground, rotation, random);
            case WAYSIDE_SHRINE -> buildWaysideShrine(world, ground, rotation, random);
            case SCORCHED_COURT -> buildScorchedCourt(world, ground, rotation, random);
            case DESECRATED_CHAPEL -> buildDesecratedChapel(world, ground, rotation, random);
        };

        // Never spawn the boss from the generation thread (it can deadlock). Record the marker and
        // let MinibossEncounterSpawnManager spawn it on the main thread at chunk load. The config
        // gate is applied there, so the vignette always generates regardless.
        MinibossEncounterSpawnManager.markPending(world.toServerWorld(), spawnMarker, type, facingYaw(rotation));
        return true;
    }

    // ── Placement validation ────────────────────────────────────────────────
    private boolean validate(StructureWorldAccess world, BlockPos ground) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int fluidCells = 0;
        int cells = 0;
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                int surface = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, ground.getX() + x, ground.getZ() + z);
                min = Math.min(min, surface);
                max = Math.max(max, surface);
                cells++;
                FluidState fluid = world.getBlockState(new BlockPos(ground.getX() + x, surface - 1, ground.getZ() + z)).getFluidState();
                if (!fluid.isEmpty()) {
                    fluidCells++;
                }
            }
        }
        if (max - min > MAX_HEIGHT_DIFF) {
            return false; // cliff / not flat enough
        }
        if ((double) fluidCells / cells > MAX_FLUID_FRACTION) {
            return false; // mostly fluid / underwater
        }
        if (!world.getBlockState(ground.up()).getFluidState().isEmpty()) {
            return false; // origin submerged
        }
        // Head-room over the central lane (rejects caves, dense canopy, cliff overhangs).
        for (int y = 1; y <= HEAD_ROOM; y++) {
            if (world.getBlockState(ground.up(y)).isSolidBlock(world, ground.up(y))) {
                return false;
            }
        }
        return true;
    }

    /** Fill only shallow gaps directly under the footprint so nothing floats; never carves terrain. */
    private void levelFoundation(StructureWorldAccess world, BlockPos ground, BlockRotation rotation) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                BlockPos cell = local(ground, x, 0, z, rotation);
                BlockState state = world.getBlockState(cell);
                if (state.isAir() || !state.getFluidState().isEmpty() || state.isReplaceable()) {
                    if (canReplace(world, cell)) {
                        world.setBlockState(cell, FOUNDATION, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    // ── Local-coordinate placement ──────────────────────────────────────────
    /** Transform a local offset (origin-centred, ly=0 == surface) by {@code rotation} into world space. */
    private BlockPos local(BlockPos ground, int lx, int ly, int lz, BlockRotation rotation) {
        int rx;
        int rz;
        switch (rotation) {
            case CLOCKWISE_90 -> { rx = -lz; rz = lx; }
            case CLOCKWISE_180 -> { rx = -lx; rz = -lz; }
            case COUNTERCLOCKWISE_90 -> { rx = lz; rz = -lx; }
            default -> { rx = lx; rz = lz; }
        }
        return ground.add(rx, ly, rz);
    }

    private void placeBlock(StructureWorldAccess world, BlockPos ground, int lx, int ly, int lz, BlockState state, BlockRotation rotation) {
        BlockPos pos = local(ground, lx, ly, lz, rotation);
        if (canReplace(world, pos)) {
            world.setBlockState(pos, state.rotate(rotation), Block.NOTIFY_LISTENERS);
        }
    }

    /** Never overwrite block entities (containers/spawners), portals, or unbreakable blocks. */
    private boolean canReplace(StructureWorldAccess world, BlockPos pos) {
        if (world.isOutOfHeightLimit(pos)) {
            return false;
        }
        if (world.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.BEDROCK) || state.getHardness(world, pos) < 0) {
            return false;
        }
        return !state.isOf(Blocks.NETHER_PORTAL)
                && !state.isOf(Blocks.END_PORTAL)
                && !state.isOf(Blocks.END_PORTAL_FRAME)
                && !state.isOf(Blocks.END_GATEWAY);
    }

    // ── Encounter layouts (return the local spawn marker, already rotated) ────
    // 1. Juggernaut — Broken Tollgate: two wall stubs, a smashed central gate, overturned debris;
    //    open lane along +Z, the Juggernaut claims the passage behind the gate facing outward.
    private BlockPos buildBrokenTollgate(StructureWorldAccess world, BlockPos g, BlockRotation r, Random rand) {
        // Left wall stub (-X side)
        for (int y = 1; y <= 2; y++) {
            placeBlock(world, g, -3, y, -1, STONE_BRICKS, r);
            placeBlock(world, g, -3, y, 0, CRACKED, r);
        }
        placeBlock(world, g, -3, 3, 0, MOSSY, r);
        placeBlock(world, g, -2, 1, 0, COBBLE, r);
        // Right wall stub (+X side)
        for (int y = 1; y <= 2; y++) {
            placeBlock(world, g, 3, y, 0, STONE_BRICKS, r);
            placeBlock(world, g, 3, y, 1, CRACKED, r);
        }
        placeBlock(world, g, 2, 1, 0, COBBLE, r);
        // Broken central gate / portcullis fragment
        placeBlock(world, g, -1, 1, 0, IRON_BARS, r);
        placeBlock(world, g, -1, 2, 0, IRON_BARS, r);
        placeBlock(world, g, 1, 1, 0, IRON_BARS, r);
        // Overturned cart-like debris and dark stains on the approach
        placeBlock(world, g, 1, 1, 2, Blocks.OAK_FENCE.getDefaultState(), r);
        placeBlock(world, g, 2, 1, 2, Blocks.HAY_BLOCK.getDefaultState(), r);
        placeBlock(world, g, -1, 0, 2, Blocks.COBBLESTONE_SLAB.getDefaultState(), r);
        // Spawn just behind the gate facing the open +Z approach
        return local(g, 0, 1, -1, r);
    }

    // 2. Mercenary — Contract Camp: lean-to awning, campfire, supply crates, low cover, target board.
    private BlockPos buildContractCamp(StructureWorldAccess world, BlockPos g, BlockRotation r, Random rand) {
        BlockState planks = Blocks.OAK_PLANKS.getDefaultState();
        // Lean-to / awning on the -Z back edge
        placeBlock(world, g, -2, 1, -3, Blocks.OAK_FENCE.getDefaultState(), r);
        placeBlock(world, g, 2, 1, -3, Blocks.OAK_FENCE.getDefaultState(), r);
        for (int x = -2; x <= 2; x++) {
            placeBlock(world, g, x, 2, -3, Blocks.OAK_SLAB.getDefaultState(), r);
        }
        // Supply crates / barrels (decorative, no loot table)
        placeBlock(world, g, -2, 1, -2, Blocks.BARREL.getDefaultState(), r);
        placeBlock(world, g, -2, 2, -2, planks, r);
        placeBlock(world, g, -1, 1, -2, Blocks.BARREL.getDefaultState(), r);
        // Campfire
        placeBlock(world, g, 1, 1, -1, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true), r);
        // Low firing cover / sandbags facing the open approach
        placeBlock(world, g, -1, 1, 2, COBBLE, r);
        placeBlock(world, g, 0, 1, 2, Blocks.COBBLESTONE_SLAB.getDefaultState(), r);
        placeBlock(world, g, 1, 1, 2, COBBLE, r);
        // Target board + rope post
        placeBlock(world, g, 3, 1, -1, Blocks.TARGET.getDefaultState(), r);
        placeBlock(world, g, 3, 1, 1, Blocks.OAK_FENCE.getDefaultState(), r);
        // One small low-value container beside the crates
        placeBlock(world, g, -2, 1, -1, Blocks.CHEST.getDefaultState(), r);
        // Spawn beside the crates/firing position, facing the open +Z lane
        return local(g, -1, 1, -1, r);
    }

    // 3. Rogue — Abandoned Wayside Shrine: broken statue/altar, two asymmetric wall fragments,
    //    vegetation & cobwebs, a concealed side alcove; Rogue tucked behind, off the approach line.
    private BlockPos buildWaysideShrine(StructureWorldAccess world, BlockPos g, BlockRotation r, Random rand) {
        // Central memorial / broken statue on a small dais
        placeBlock(world, g, 0, 1, 0, Blocks.CHISELED_STONE_BRICKS.getDefaultState(), r);
        placeBlock(world, g, 0, 2, 0, Blocks.STONE_BRICK_WALL.getDefaultState(), r);
        placeBlock(world, g, 0, 0, 1, Blocks.MOSSY_STONE_BRICK_SLAB.getDefaultState(), r);
        // Asymmetric wall fragments (intentionally different lengths/sides)
        for (int z = -2; z <= 0; z++) {
            placeBlock(world, g, -2, 1, z, MOSSY, r);
        }
        placeBlock(world, g, -2, 2, -2, CRACKED, r);
        placeBlock(world, g, 2, 1, -2, MOSSY, r);
        placeBlock(world, g, 2, 1, -1, CRACKED, r);
        placeBlock(world, g, 2, 2, -2, Blocks.STONE_BRICK_WALL.getDefaultState(), r);
        // Concealment: cobwebs, vines, leaves around the rear alcove
        placeBlock(world, g, -2, 1, -1, Blocks.OAK_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true), r);
        placeBlock(world, g, 2, 2, -1, Blocks.VINE.getDefaultState(), r);
        // Spawn behind-right of the monument, screened by the +X wall fragment and off the +Z sightline
        return local(g, 1, 1, -1, r);
    }

    // 4. Fire Mage — Scorched Ritual Court: blackened floor, central brazier/dais, four broken
    //    corner pillars; deliberately open so the player cannot fully negate ranged zoning.
    private BlockPos buildScorchedCourt(StructureWorldAccess world, BlockPos g, BlockRotation r, Random rand) {
        // Scorched floor
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockState floor = (Math.abs(x) + Math.abs(z)) % 2 == 0
                        ? Blocks.BLACK_CONCRETE_POWDER.getDefaultState()
                        : Blocks.BASALT.getDefaultState();
                placeBlock(world, g, x, 0, z, floor, r);
            }
        }
        // Magma cracks + central one-block dais with a brazier
        placeBlock(world, g, 2, 0, -1, Blocks.MAGMA_BLOCK.getDefaultState(), r);
        placeBlock(world, g, -2, 0, 1, Blocks.MAGMA_BLOCK.getDefaultState(), r);
        placeBlock(world, g, 0, 1, 0, Blocks.POLISHED_BLACKSTONE.getDefaultState(), r);
        placeBlock(world, g, 0, 2, 0, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true), r);
        // Four short corner pillars / broken columns (kept low so they do not block projectiles)
        int[][] corners = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
        for (int[] c : corners) {
            placeBlock(world, g, c[0], 1, c[1], Blocks.POLISHED_BASALT.getDefaultState(), r);
            placeBlock(world, g, c[0], 2, c[1], Blocks.BASALT.getDefaultState(), r);
        }
        // Spawn just off the brazier on the open scorched floor, facing the +Z approach
        return local(g, 0, 1, 1, r);
    }

    // 5. Templar — Desecrated Roadside Chapel: two walls forming a shallow nave, central judgment
    //    altar, bell/chains, a narrow central approach lane; institutional and severe.
    private BlockPos buildDesecratedChapel(StructureWorldAccess world, BlockPos g, BlockRotation r, Random rand) {
        // Shallow nave walls along +/-X, opening toward +Z
        for (int z = -3; z <= 0; z++) {
            for (int y = 1; y <= 2; y++) {
                placeBlock(world, g, -2, y, z, z == -3 ? MOSSY : STONE_BRICKS, r);
                placeBlock(world, g, 2, y, z, z == -3 ? MOSSY : CRACKED, r);
            }
        }
        // Back wall behind the altar
        for (int x = -2; x <= 2; x++) {
            placeBlock(world, g, x, 1, -3, STONE_BRICKS, r);
            placeBlock(world, g, x, 2, -3, CRACKED, r);
        }
        // Central judgment altar / execution stone
        placeBlock(world, g, 0, 1, -2, Blocks.CHISELED_STONE_BRICKS.getDefaultState(), r);
        placeBlock(world, g, 0, 2, -2, Blocks.STONE_BRICK_SLAB.getDefaultState(), r);
        // Bell on a crossbeam, chains, iron bars
        placeBlock(world, g, 0, 3, -3, Blocks.BELL.getDefaultState(), r);
        placeBlock(world, g, -1, 2, -3, IRON_BARS, r);
        placeBlock(world, g, 1, 2, -3, IRON_BARS, r);
        placeBlock(world, g, -2, 3, -2, CHAIN, r);
        placeBlock(world, g, 2, 3, -2, CHAIN, r);
        // Narrow path leading to the altar
        placeBlock(world, g, 0, 0, 1, Blocks.STONE_BRICK_SLAB.getDefaultState(), r);
        placeBlock(world, g, 0, 0, 2, Blocks.STONE_BRICK_SLAB.getDefaultState(), r);
        // Spawn in front of the altar facing the open approach (+Z)
        return local(g, 0, 1, -1, r);
    }

    /** Yaw facing the open +Z approach lane, rotated with the structure. */
    private float facingYaw(BlockRotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> 270.0F;
            case CLOCKWISE_180 -> 180.0F;
            case COUNTERCLOCKWISE_90 -> 90.0F;
            default -> 0.0F;
        };
    }
}
