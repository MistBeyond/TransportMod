package com.mistbeyond.transport.block.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.SignalPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackCellData;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RailTrackCellBlockEntity extends BlockEntity implements TrackCellData {
    private Set<TrackPlacement> placements = Set.of();
    private Optional<SignalPlacement> signal = Optional.empty();

    public RailTrackCellBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(RailTrackCellBlockEntities.TRACK_CELL.get(), worldPosition, blockState);
    }

    public void setData(Set<TrackPlacement> placements, Optional<SignalPlacement> signal) {
        this.placements = Set.copyOf(placements);
        this.signal = signal;
        setChanged();
    }

    @Override
    public GridPos cell() {
        BlockPos pos = getBlockPos();
        return new GridPos(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Set<TrackPlacement> placements() {
        return placements;
    }

    @Override
    public Optional<SignalPlacement> signal() {
        return signal;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.placements = Set.copyOf(input.read("Placements", TrackPlacement.CODEC.listOf()).orElse(List.of()));
        this.signal = input.read("Signal", SignalPlacement.CODEC);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Placements", TrackPlacement.CODEC.listOf(), placements.stream().toList());
        signal.ifPresent(value -> output.store("Signal", SignalPlacement.CODEC, value));
    }
}
