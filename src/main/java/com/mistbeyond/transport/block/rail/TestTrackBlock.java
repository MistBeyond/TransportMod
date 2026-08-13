package com.mistbeyond.transport.block.rail;

import com.mistbeyond.registry.RegisterBlock;
import com.mistbeyond.registry.SubscribeRegistration;
import com.mistbeyond.registry.impl.BlockRegistration;
import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.TrackGraphSource;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

@RegisterBlock
public class TestTrackBlock extends Block {
    public static final EnumProperty<TrackAxis> AXIS = EnumProperty.create("axis", TrackAxis.class);

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public TestTrackBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, TrackAxis.NORTH_SOUTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !(level.getBlockState(pos.below()).getBlock() instanceof TestTrackBlock);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        if (!axis.isHorizontal()) {
            axis = context.getHorizontalDirection().getAxis();
        }
        return defaultBlockState().setValue(AXIS, axis == Direction.Axis.X ? TrackAxis.EAST_WEST : TrackAxis.NORTH_SOUTH);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    public static boolean isTrackAt(Level level, GridPos pos) {
        return level.getBlockState(new BlockPos(pos.x(), pos.y(), pos.z())).getBlock() instanceof TestTrackBlock;
    }

    public static Set<GridDirection> placementDirections(BlockState state) {
        if (!(state.getBlock() instanceof TestTrackBlock)) {
            return Set.of();
        }
        return placementDirectionsForAxis(state.getValue(AXIS));
    }

    static Set<GridDirection> placementDirectionsForAxis(TrackAxis axis) {
        return switch (axis) {
            case NORTH_SOUTH -> Set.of(GridDirection.NORTH, GridDirection.SOUTH);
            case EAST_WEST -> Set.of(GridDirection.EAST, GridDirection.WEST);
        };
    }

    public static TrackGraphSource source(Level level) {
        return cell -> {
            BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TestTrackBlock)) {
                return Set.of();
            }
            Set<TrackPlacement> placements = new HashSet<>();
            for (GridDirection direction : placementDirections(state)) {
                placements.add(new TrackPlacement(cell, direction, TrackType.STRAIGHT));
            }
            return placements;
        };
    }

    @SubscribeRegistration
    @SuppressWarnings("unused")
    private static void register(BlockRegistration registration) {
        registration.register(
                "test_track",
                TestTrackBlock::new,
                properties -> properties.strength(0.5F).noOcclusion()
        );
    }

    public enum TrackAxis implements StringRepresentable {
        NORTH_SOUTH("north_south"),
        EAST_WEST("east_west");

        private final String serializedName;

        TrackAxis(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
