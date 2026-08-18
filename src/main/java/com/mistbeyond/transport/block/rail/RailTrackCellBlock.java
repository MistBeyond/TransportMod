package com.mistbeyond.transport.block.rail;

import com.mistbeyond.registry.RegisterBlock;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.BlockRegistration;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.SignalPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackCellData;
import com.mistbeyond.transport.api.rail.graph.TrackCellDataRecord;
import com.mistbeyond.transport.api.rail.graph.TrackGraphSource;
import com.mistbeyond.transport.api.rail.graph.TrackAxis;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import com.mistbeyond.transport.core.rail.RailNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RegisterBlock
public class RailTrackCellBlock extends Block {
    /**
     * Stores one of the four track axes (N–S, E–W, NE–SW, NW–SE), not eight one-way directions: a simple cell is a
     * bidirectional segment, so each pair of opposite directions is equivalent (see ADR 0005 and
     * docs/roadmap/rail/tracks.md). Signal direction still uses the full 8-direction {@link GridDirection}.
     */
    public static final EnumProperty<TrackAxis> DIRECTION = EnumProperty.create("direction", TrackAxis.class);

    public RailTrackCellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DIRECTION, TrackAxis.E_W));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The aiming/terrain shape is a single cell-centered box (not the 1 px staircase), so vanilla break particles
        // spawn in normal quantity, centered on the cell; the physical collision stays collisionShape below.
        return AIMING_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShape(state, level, pos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !(level.getBlockState(pos.below()).getBlock() instanceof RailTrackCellBlock);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        TrackAxis axis = player == null
                ? TrackAxis.from(fromMinecraft(context.getHorizontalDirection()))
                : axisFromYaw(player.getYRot());
        return defaultBlockState().setValue(DIRECTION, axis);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        markManagerDirty(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
                                               BlockPos pos, boolean movedByPiston) {
        markManagerDirty(level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIRECTION);
    }

    public static boolean isTrackAt(Level level, GridPos pos) {
        return level.getBlockState(new BlockPos(pos.x(), pos.y(), pos.z())).getBlock() instanceof RailTrackCellBlock;
    }

    public static TrackGraphSource source(Level level) {
        return new TrackGraphSource() {
            @Override
            public TrackCellData cellDataAt(GridPos cell) {
                BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof TrackCellData complex && complex.cell().equals(cell)) {
                    return complex;
                }

                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof RailTrackCellBlock)) {
                    return new TrackCellDataRecord(cell, Set.of(), Optional.empty());
                }
                return new TrackCellDataRecord(cell, placementsFor(cell, state.getValue(DIRECTION)), Optional.empty());
            }

            @Override
            public Set<TrackPlacement> placementsAt(GridPos cell) {
                return cellDataAt(cell).placements();
            }

            @Override
            public Optional<SignalPlacement> signalAt(GridPos cell) {
                return cellDataAt(cell).signal();
            }
        };
    }

    /**
     * Collision shape for a track cell: the block entity's union of placement strips when the cell is complex,
     * otherwise the strip of the BlockState axis. The graph source reads cells the same way (block entity first,
     * BlockState fallback), so collision and graph always agree on what a cell contains.
     */
    public static VoxelShape collisionShape(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TrackCellData complex
                && complex.cell().equals(new GridPos(pos.getX(), pos.getY(), pos.getZ()))) {
            return TrackCellShapes.cellShape(complex.placements());
        }
        return TrackCellShapes.axisShape(state.getValue(DIRECTION));
    }

    /**
     * Aiming/terrain shape for a track cell (used by {@code getShape}): a single 16x16x2 box centered on the cell.
     * An axis-aligned full-cell box keeps vanilla break/terrain particles at normal quantity (one box, not one burst
     * per 1 px collision box) and centered on the cell, so they never burst toward a neighbor.
     */
    public static final VoxelShape AIMING_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    /**
     * Placements emitted by a simple track cell: both directional placements of the stored axis, so a cell connects
     * bidirectionally with its neighbors along that axis. This matches the graph rule that an edge only exists when
     * both cells point at each other, and keeps simple straight lines fully connected (see ADR 0005 and
     * docs/roadmap/rail/tracks.md). Crossings and corners still require a complex cell with multiple placements.
     */
    public static Set<TrackPlacement> placementsFor(GridPos cell, TrackAxis axis) {
        TrackType trackType = axis.diagonal() ? TrackType.DIAGONAL_45 : TrackType.STRAIGHT;
        Set<TrackPlacement> placements = new HashSet<>();
        for (GridDirection direction : axis.directions()) {
            placements.add(new TrackPlacement(cell, direction, trackType));
        }
        return Set.copyOf(placements);
    }

    private static GridDirection fromMinecraft(Direction direction) {
        return switch (direction) {
            case NORTH -> GridDirection.NORTH;
            case SOUTH -> GridDirection.SOUTH;
            case WEST -> GridDirection.WEST;
            case EAST, UP, DOWN -> GridDirection.EAST;
        };
    }

    /**
     * Maps the player's horizontal rotation to a track axis in 45-degree steps, so looking along a diagonal direction
     * places a diagonal 45 track. Yaw 0 faces south; positive yaw turns clockwise (west, north, east).
     */
    static TrackAxis axisFromYaw(float yaw) {
        int index = Math.floorMod(Math.round(yaw / 45.0F), 8);
        GridDirection direction = switch (index) {
            case 0 -> GridDirection.SOUTH;
            case 1 -> GridDirection.SOUTH_WEST;
            case 2 -> GridDirection.WEST;
            case 3 -> GridDirection.NORTH_WEST;
            case 4 -> GridDirection.NORTH;
            case 5 -> GridDirection.NORTH_EAST;
            case 6 -> GridDirection.EAST;
            case 7 -> GridDirection.SOUTH_EAST;
            default -> throw new IllegalArgumentException("unreachable yaw index: " + index);
        };
        return TrackAxis.from(direction);
    }

    private static void markManagerDirty(Level level, BlockPos pos) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            RailNetworkManager.of(serverLevel).markCellChanged(new GridPos(pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(BlockRegistration registration) {
        registration.register(
                "rail_track_cell",
                RailTrackCellBlock::new,
                properties -> properties.strength(0.5F).noOcclusion().sound(SoundType.METAL)
        );
    }
}
