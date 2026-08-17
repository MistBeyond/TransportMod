package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchSnapshot;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalState;
import com.mistbeyond.transport.api.rail.section.SignalView;

import java.util.HashSet;
import java.util.Set;

public final class SignalStateResolver {
    private SignalStateResolver() {
    }

    public static Set<SignalState> resolve(RailGraphView graph, DispatchSnapshot dispatch) {
        Set<RailSectionId> blocked = new HashSet<>();
        dispatch.reservations().forEach(reservation -> blocked.add(reservation.sectionId()));
        blocked.addAll(dispatch.manualClaims().keySet());

        Set<SignalState> result = new HashSet<>();
        for (SignalView signal : graph.signals()) {
            boolean red = graph.sections().stream()
                    .filter(section -> section.boundaries().stream().anyMatch(boundary -> boundary.id().equals(signal.id())))
                    .anyMatch(section -> blocked.contains(section.id()));
            result.add(new SignalState(signal.id(), red ? SignalAspect.RED : SignalAspect.GREEN));
        }
        return result;
    }
}
