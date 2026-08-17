package com.mistbeyond.transport.internal.rail;

import com.mistbeyond.transport.api.rail.RailTrainSnapshot;
import com.mistbeyond.transport.api.rail.dispatch.RailControlMode;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainScheduleType;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainStop;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.station.RailStationId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class RailNetworkCodecs {
    public static final Codec<GridPos> GRID_POS = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(GridPos::x),
            Codec.INT.fieldOf("y").forGetter(GridPos::y),
            Codec.INT.fieldOf("z").forGetter(GridPos::z)
    ).apply(instance, GridPos::new));

    public static final Codec<RailTrainId> TRAIN_ID = Codec.STRING.xmap(RailTrainId::new, RailTrainId::value);

    public static final Codec<RailControlMode> CONTROL_MODE =
            Codec.STRING.xmap(RailControlMode::valueOf, RailControlMode::name);

    public static final Codec<RailStationId> STATION_ID = Codec.STRING.xmap(RailStationId::new, RailStationId::value);

    public static final Codec<RailTrainScheduleType> SCHEDULE_TYPE =
            Codec.STRING.xmap(RailTrainScheduleType::valueOf, RailTrainScheduleType::name);

    public static final Codec<RailTrainStop> STOP = RecordCodecBuilder.create(instance -> instance.group(
            STATION_ID.fieldOf("station").forGetter(RailTrainStop::station),
            Codec.INT.fieldOf("dwell_ticks").forGetter(RailTrainStop::dwellTicks)
    ).apply(instance, RailTrainStop::new));

    public static final Codec<RailTrainSchedule> SCHEDULE = RecordCodecBuilder.create(instance -> instance.group(
            SCHEDULE_TYPE.fieldOf("type").forGetter(RailTrainSchedule::type),
            STOP.listOf().fieldOf("stops").forGetter(RailTrainSchedule::stops)
    ).apply(instance, RailTrainSchedule::new));

    public static final Codec<RailTrainSnapshot> TRAIN = RecordCodecBuilder.create(instance -> instance.group(
            TRAIN_ID.fieldOf("id").forGetter(RailTrainSnapshot::id),
            CONTROL_MODE.fieldOf("control_mode").forGetter(RailTrainSnapshot::controlMode),
            GRID_POS.fieldOf("position").forGetter(RailTrainSnapshot::position),
            Codec.BOOL.fieldOf("derailed").forGetter(RailTrainSnapshot::derailed),
            SCHEDULE.optionalFieldOf("schedule").forGetter(RailTrainSnapshot::schedule)
    ).apply(instance, RailTrainSnapshot::new));

    public static final Codec<RailNetworkState> STATE = RecordCodecBuilder.create(instance -> instance.group(
            TRAIN.listOf().fieldOf("trains").forGetter(RailNetworkState::trains)
    ).apply(instance, RailNetworkState::new));

    private RailNetworkCodecs() {
    }
}
