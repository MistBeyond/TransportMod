package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchService;
import com.mistbeyond.transport.api.rail.dispatch.DispatchSnapshot;
import com.mistbeyond.transport.api.rail.dispatch.RailPathfinder;
import com.mistbeyond.transport.api.rail.dispatch.RailRoute;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.dispatch.Reservation;
import com.mistbeyond.transport.api.rail.dispatch.ReservationState;
import com.mistbeyond.transport.api.rail.dispatch.RouteLock;
import com.mistbeyond.transport.api.rail.dispatch.RouteRequest;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentView;
import com.mistbeyond.transport.api.rail.graph.TraversalDirection;
import com.mistbeyond.transport.api.rail.section.RailSectionId;
import com.mistbeyond.transport.api.rail.section.RailSectionView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DispatchServiceImpl implements DispatchService {
    private final RailGraphView graph;
    private final RailPathfinder pathfinder;
    private final Map<RailTrainId, RouteLock> locks = new HashMap<>();
    private final Map<RailSectionId, Reservation> reservations = new HashMap<>();
    private final Map<RailSectionId, RailTrainId> manualClaims = new HashMap<>();

    public DispatchServiceImpl(RailGraphView graph, RailPathfinder pathfinder) {
        this.graph = graph;
        this.pathfinder = pathfinder;
    }

    @Override
    public DispatchResult start(RouteRequest request) {
        if (locks.containsKey(request.trainId())) {
            return new DispatchResult.Rejected("Train already has an active route: " + request.trainId());
        }
        Optional<RailRoute> route = pathfinder.findRoute(graph, request);
        if (route.isEmpty()) {
            return new DispatchResult.Rejected("No route found for train: " + request.trainId());
        }

        List<RailSectionId> sectionIds = routeSections(route.get());
        for (RailSectionId sectionId : sectionIds) {
            if (isBlocked(sectionId)) {
                return new DispatchResult.Rejected("Section is not free: " + sectionId);
            }
        }

        RouteLock lock = new RouteLock(request.trainId(), route.get(), sectionIds, true);
        locks.put(request.trainId(), lock);
        if (!sectionIds.isEmpty()) {
            RailSectionId first = sectionIds.getFirst();
            reservations.put(first, new Reservation(first, request.trainId(), ReservationState.RESERVED));
        }
        return new DispatchResult.Accepted(lock);
    }

    @Override
    public AdvanceResult advance(RailTrainId trainId, RailSectionId currentSection) {
        RouteLock lock = locks.get(trainId);
        if (lock == null || !lock.active()) {
            return new AdvanceResult.Blocked(currentSection);
        }
        int index = lock.sections().indexOf(currentSection);
        if (index < 0) {
            return new AdvanceResult.Blocked(currentSection);
        }
        if (index == lock.sections().size() - 1) {
            reservations.remove(currentSection);
            locks.remove(trainId);
            return new AdvanceResult.Arrived();
        }

        RailSectionId nextSection = lock.sections().get(index + 1);
        if (isBlocked(nextSection)) {
            return new AdvanceResult.Blocked(currentSection);
        }

        reservations.remove(currentSection);
        reservations.put(nextSection, new Reservation(nextSection, trainId, ReservationState.RESERVED));
        return new AdvanceResult.Move(nextSection);
    }

    @Override
    public void claimManual(RailTrainId trainId, RailSectionId sectionId) {
        if (reservations.containsKey(sectionId)) {
            throw new IllegalStateException("Section is already reserved: " + sectionId);
        }
        if (manualClaims.containsKey(sectionId)) {
            throw new IllegalStateException("Section is already manually claimed: " + sectionId);
        }
        manualClaims.put(sectionId, trainId);
    }

    @Override
    public void releaseManual(RailTrainId trainId, RailSectionId sectionId) {
        if (!trainId.equals(manualClaims.get(sectionId))) {
            throw new IllegalStateException("Section is not manually claimed by this train: " + sectionId);
        }
        manualClaims.remove(sectionId);
    }

    @Override
    public void release(RailTrainId trainId) {
        reservations.values().removeIf(reservation -> reservation.trainId().equals(trainId));
        locks.remove(trainId);
    }

    @Override
    public DispatchSnapshot snapshot() {
        return new DispatchSnapshot(
                new HashSet<>(reservations.values()),
                new HashSet<>(locks.values()),
                new HashMap<>(manualClaims)
        );
    }

    private boolean isBlocked(RailSectionId sectionId) {
        return reservations.containsKey(sectionId) || manualClaims.containsKey(sectionId);
    }

    private List<RailSectionId> routeSections(RailRoute route) {
        LinkedHashSet<RailSectionId> result = new LinkedHashSet<>();
        for (RailRoute.RailRouteStep step : route.steps()) {
            List<RailSectionView> sectionsOnEdge = graph.sections().stream()
                    .filter(section -> section.segments().stream()
                            .anyMatch(segmentId -> segmentId.edgeId().equals(step.edgeId())))
                    .toList();
            List<RailSectionView> ordered = sectionsOnEdge.stream()
                    .sorted(java.util.Comparator.comparingDouble(
                            section -> section.segments().stream()
                                    .map(graph::segmentById)
                                    .flatMap(Optional::stream)
                                    .filter(segment -> segment.edgeId().equals(step.edgeId()))
                                    .mapToDouble(TrackSegmentView::fromMeters)
                                    .min()
                                    .orElse(0.0)
                    ))
                    .toList();
            if (step.direction() == TraversalDirection.REVERSE) {
                ordered = ordered.reversed();
            }
            ordered.forEach(section -> result.add(section.id()));
        }
        return new ArrayList<>(result);
    }
}
