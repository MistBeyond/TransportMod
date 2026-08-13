package com.mistbeyond.transport.api.rail.dispatch;

import com.mistbeyond.transport.api.rail.section.RailSectionId;

public record Reservation(
        RailSectionId sectionId,
        RailTrainId trainId,
        ReservationState state
) {
}
