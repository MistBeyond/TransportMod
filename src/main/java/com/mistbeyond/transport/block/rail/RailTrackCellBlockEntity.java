package com.mistbeyond.transport.block.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.SignalPlacement;
import com.mistbeyond.transport.api.rail.graph.TrackCellData;
import com.mistbeyond.transport.api.rail.graph.TrackCellDataRecord;
import com.mistbeyond.transport.api.rail.graph.TrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RailTrackCellBlockEntity extends BlockEntity implements TrackCellData {
    /**
     * Model-data key of the per-block-entity track geometry for the baked-model rendering route (ADR 0006). The
     * client block model reads this property and generates quads from the placement set; the value is an immutable
     * {@link TrackCellDataRecord} snapshot because model data is read from meshing worker threads.
     */
    public static final ModelProperty<TrackCellData> TRACK_CELL_MODEL_DATA = new ModelProperty<>();

    private Set<TrackPlacement> placements = Set.of();
    private Set<SignalPlacement> signals = Set.of();

    public RailTrackCellBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(RailTrackCellBlockEntities.TRACK_CELL.get(), worldPosition, blockState);
    }

    /**
     * Backwards-compatible overload for single-signal callers.
     */
    public void setData(Set<TrackPlacement> placements, Optional<SignalPlacement> signal) {
        setData(placements, signal.map(Set::of).orElse(Set.of()));
    }

    public void setData(Set<TrackPlacement> placements, Set<SignalPlacement> signals) {
        this.placements = Set.copyOf(placements);
        this.signals = Set.copyOf(signals);
        setChanged();
        requestModelDataUpdate();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.of(TRACK_CELL_MODEL_DATA, new TrackCellDataRecord(cell(), placements, signals));
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
    public Set<SignalPlacement> signals() {
        return signals;
    }

    @Override
    public Optional<SignalPlacement> signal() {
        return signals.stream().findFirst();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.placements = Set.copyOf(input.read("Placements", TrackPlacement.CODEC.listOf()).orElse(List.of()));
        // Backwards compatible: old saves used single "Signal", new saves use "Signals" list.
        List<SignalPlacement> migrated = input.read("Signal", SignalPlacement.CODEC).map(List::of).orElse(List.of());
        List<SignalPlacement> list = input.read("Signals", SignalPlacement.CODEC.listOf()).orElse(migrated);
        this.signals = Set.copyOf(list);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Placements", TrackPlacement.CODEC.listOf(), placements.stream().toList());
        if (!signals.isEmpty()) {
            output.store("Signals", SignalPlacement.CODEC.listOf(), signals.stream().toList());
            // Keep single "Signal" for backwards compatibility when exactly one signal exists (optional, not required for load).
            if (signals.size() == 1) {
                output.store("Signal", SignalPlacement.CODEC, signals.iterator().next());
            }
        }
    }
}
