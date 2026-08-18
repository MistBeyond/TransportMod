package com.mistbeyond.transport.internal.rail;

import com.mistbeyond.transport.Ids;
import com.mistbeyond.transport.core.rail.RailNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class RailNetworkSavedData extends SavedData {
    public static final SavedDataType<RailNetworkSavedData> TYPE = new SavedDataType<>(
            Ids.thisMod("rail_network"),
            RailNetworkSavedData::new,
            RailNetworkCodecs.STATE.xmap(RailNetworkSavedData::new, RailNetworkSavedData::state)
    );

    private RailNetworkState state = RailNetworkState.EMPTY;

    @Nullable
    private transient RailNetworkManager manager;

    public RailNetworkSavedData() {
    }

    public RailNetworkSavedData(RailNetworkState state) {
        this.state = state;
    }

    public static RailNetworkManager managerOf(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE).manager(level);
    }

    public RailNetworkManager manager(ServerLevel level) {
        if (manager == null) {
            manager = new RailNetworkManager(level);
            manager.attachSavedData(this);
            manager.restore(state);
        }
        return manager;
    }

    public RailNetworkState state() {
        return state;
    }

    public void updateState(RailNetworkState state) {
        this.state = state;
        setDirty();
    }
}
