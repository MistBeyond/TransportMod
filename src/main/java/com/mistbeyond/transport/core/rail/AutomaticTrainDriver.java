package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.dispatch.DispatchService;
import com.mistbeyond.transport.api.rail.dispatch.RailControlMode;
import com.mistbeyond.transport.api.rail.dispatch.RailRoute;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainSchedule;
import com.mistbeyond.transport.api.rail.dispatch.RailTrainScheduleType;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeView;
import com.mistbeyond.transport.api.rail.graph.TrackPosition;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentId;
import com.mistbeyond.transport.api.rail.graph.TrackSegmentView;
import com.mistbeyond.transport.api.rail.graph.TraversalDirection;
import com.mistbeyond.transport.api.rail.section.RailSectionView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Advances automatic trains along their locked {@link RailRoute}s on the server main thread. The driver is the
 * automatic counterpart of manual player input: it moves a train step by step, reserves sections through
 * {@link DispatchService#advance} when crossing boundaries, waits at blocked section boundaries, dwells at timetable
 * stops, reroutes on graph changes, and finishes {@code ONE_WAY} schedules or restarts {@code LOOP} schedules. The
 * train entity itself stays presentation-only and merely reflects the positions computed here.
 */
public class AutomaticTrainDriver {
    /**
     * Automatic cruise speed in blocks (meters) per tick.
     */
    public static final double AUTO_SPEED = 0.4;
    private static final double EPSILON = 1.0E-6;
    /**
     * Default dwell at a stop whose schedule entry has no explicit dwell.
     */
    private static final int DEFAULT_DWELL_TICKS = 40;
    /**
     * Retry interval after a schedule restart was rejected (e.g. no free route).
     */
    private static final int RESTART_RETRY_TICKS = 20;
    /**
     * Guard against pathological zero-length edges looping forever in one tick.
     */
    private static final int MAX_ADVANCES_PER_TICK = 8;

    private final Map<RailTrainId, AutoMotionState> states = new HashMap<>();
    private final Map<RailTrainId, List<RailNodeId>> stopNodes = new HashMap<>();

    /**
     * The server-side host the driver drives through: the rail network manager of one level.
     */
    public interface Host {
        RailGraphView graph();

        DispatchService dispatch();

        List<RailTrainAggregate> trains();

        void update(RailTrainAggregate train);

        /**
         * Releases the train's current route and re-runs its schedule from the nearest reachable node.
         */
        void restartSchedule(RailTrainId trainId);
    }

    /**
     * Called after a schedule was accepted; resets the motion state and records the ordered stop nodes.
     */
    public void onScheduleStarted(RailTrainId trainId, List<RailNodeId> stopNodes) {
        this.stopNodes.put(trainId, List.copyOf(stopNodes));
        this.states.put(trainId, AutoMotionState.initial());
    }

    /**
     * Drops all driver state for a train (removed, derailed into manual, or switched to manual).
     */
    public void discard(RailTrainId trainId) {
        this.states.remove(trainId);
        this.stopNodes.remove(trainId);
    }

    public void tick(Host host) {
        for (RailTrainAggregate train : host.trains()) {
            if (train.controlMode() != RailControlMode.AUTOMATIC || train.derailed()) {
                continue;
            }
            RailTrainId id = train.id();
            AutoMotionState state = states.getOrDefault(id, AutoMotionState.initial());
            states.put(id, step(host, train, state));
        }
    }

    /**
     * World-space position at which the given automatic train currently renders; the entity only follows.
     */
    public Optional<TrainPresentation> presentation(Host host, RailTrainId trainId) {
        Optional<RailTrainAggregate> train = host.trains().stream()
                .filter(candidate -> candidate.id().equals(trainId))
                .findFirst();
        if (train.isEmpty() || train.get().route().isEmpty()) {
            return Optional.empty();
        }
        AutoMotionState state = states.getOrDefault(trainId, AutoMotionState.initial());
        if (state.done()) {
            return Optional.of(TrainPresentation.atCell(train.get().position()));
        }
        RailRouteView route = routeView(train.get(), host.graph());
        Optional<StepView> step = currentStep(route, state);
        if (step.isEmpty()) {
            return Optional.of(TrainPresentation.atCell(train.get().position()));
        }
        RailEdgeView edge = host.graph().edgeById(step.get().edgeId()).orElse(null);
        if (edge == null) {
            return Optional.of(TrainPresentation.atCell(train.get().position()));
        }
        double fraction = edge.lengthMeters() > 0.0
                ? Math.min(1.0, state.progress() / edge.lengthMeters())
                : 0.0;
        return Optional.of(presentOn(step.get(), fraction));
    }

    private AutoMotionState step(Host host, RailTrainAggregate train, AutoMotionState state) {
        RailTrainId id = train.id();
        if (train.route().isEmpty() || train.schedule().isEmpty()) {
            return waitForRestart(host, id, state);
        }
        if (state.dwellRemaining() > 0) {
            return state.withDwellRemaining(state.dwellRemaining() - 1);
        }

        RailRouteView route = routeView(train, host.graph());
        if (route.steps().isEmpty()) {
            // The start equals the destination (no edges to travel): nothing left to do.
            return finish(host, train, state);
        }
        AutoMotionState rolling = state;
        double remaining = AUTO_SPEED;
        for (int guard = 0; guard < MAX_ADVANCES_PER_TICK; guard++) {
            Optional<StepView> step = currentStep(route, rolling);
            if (step.isEmpty()) {
                return finish(host, train, rolling);
            }
            StepView current = step.get();
            RailEdgeView edge = host.graph().edgeById(current.edgeId()).orElse(null);
            if (edge == null) {
                // The graph changed underneath this route (edge removed): reroute from the nearest node.
                return waitForRestart(host, id, rolling);
            }
            double length = edge.lengthMeters();
            double currentMeters = edgeProgress(edge, current, rolling.progress());

            // Section containing the current position; needed for every boundary check below.
            Optional<RailSectionView> hereSection =
                    host.graph().sectionAt(trackPosition(edge, current, rolling.progress()));

            // At the end of this edge: reserve the next section if it differs, then switch to the next route step.
            if (currentMeters >= length - EPSILON) {
                int nextIndex = rolling.stepIndex() + 1;
                if (nextIndex >= route.steps().size()) {
                    return finish(host, train, rolling);
                }
                if (hereSection.isPresent() && needsCrossEdgeAdvance(host, route, nextIndex, hereSection.get())) {
                    DispatchService.AdvanceResult result = host.dispatch().advance(id, hereSection.get().id());
                    if (result instanceof DispatchService.AdvanceResult.Blocked) {
                        // Stop at the edge end; retry the advance next tick.
                        return rolling.withProgress(rolling.progress());
                    }
                    if (result instanceof DispatchService.AdvanceResult.Arrived) {
                        return finish(host, train, rolling);
                    }
                }
                // Dwell at the exit node if it is the next timetable stop.
                List<RailNodeId> stops = stopNodes.getOrDefault(id, List.of());
                int stopIndex = rolling.stopIndex();
                if (stopIndex < stops.size() && stops.get(stopIndex).equals(current.exit())) {
                    int dwell = dwellTicks(train, stopIndex);
                    moveToCell(host, train, current);
                    return rolling
                            .withStepIndex(nextIndex)
                            .withProgress(0.0)
                            .withStopIndex(stopIndex + 1)
                            .withDwellRemaining(dwell);
                }
                moveToCell(host, train, current);
                rolling = rolling.withStepIndex(nextIndex).withProgress(0.0);
                continue;
            }

            if (hereSection.isEmpty()) {
                // Not on any section (e.g. mid-edit): park and try again next tick.
                return rolling;
            }
            // Does the planned travel actually cross into a different section?
            TrackPosition there = trackPosition(
                    edge, current, Math.min(length, rolling.progress() + remaining));
            Optional<RailSectionView> thereSection = host.graph().sectionAt(there);
            boolean crossSection = thereSection.isPresent()
                    && !thereSection.get().id().equals(hereSection.get().id());
            if (crossSection) {
                DispatchService.AdvanceResult result = host.dispatch().advance(id, hereSection.get().id());
                if (result instanceof DispatchService.AdvanceResult.Blocked) {
                    // Stop exactly at the boundary; retry next tick, the dispatcher keeps the reservation.
                    Optional<Double> boundary = boundaryProgress(hereSection.get(), edge, host.graph());
                    double boundaryMeters = boundary.orElse(length);
                    return rolling.withProgress(edgeProgressToTravel(edge, current, boundaryMeters));
                }
                if (result instanceof DispatchService.AdvanceResult.Arrived) {
                    // The last section of the locked route has been traversed.
                    return finish(host, train, rolling);
                }
                // Move granted: enter the next section and keep spending the remaining distance.
                double boundaryMeters = boundaryProgress(hereSection.get(), edge, host.graph()).orElse(length);
                remaining -= Math.max(0.0, boundaryMeters - currentMeters);
                rolling = rolling.withProgress(edgeProgressToTravel(edge, current, boundaryMeters));
                continue;
            }

            // Normal advance; never past the edge end within this iteration.
            double travel = Math.min(remaining, length - currentMeters);
            double nextMeters = currentMeters + travel;
            rolling = rolling.withProgress(edgeProgressToTravel(edge, current, nextMeters));
            moveTo(host, train, edge, current, rolling.progress());
            remaining -= travel;
            if (remaining <= EPSILON) {
                break;
            }
        }
        return rolling;
    }

    /**
     * Whether the next edge of the route starts in a different section than the one we are leaving.
     */
    private static boolean needsCrossEdgeAdvance(
            Host host,
            RailRouteView route,
            int nextIndex,
            RailSectionView hereSection
    ) {
        StepView next = route.steps().get(nextIndex);
        RailEdgeView nextEdge = host.graph().edgeById(next.edgeId()).orElse(null);
        if (nextEdge == null) {
            return false;
        }
        return host.graph()
                .sectionAt(trackPosition(nextEdge, next, 0.0))
                .map(section -> !section.id().equals(hereSection.id()))
                .orElse(false);
    }

    private AutoMotionState finish(Host host, RailTrainAggregate train, AutoMotionState state) {
        Optional<RailTrainSchedule> schedule = train.schedule();
        if (schedule.isPresent() && schedule.get().type() == RailTrainScheduleType.LOOP) {
            // A LOOP continues from the first stop; restarting projects the current position to the nearest node.
            host.restartSchedule(train.id());
            return AutoMotionState.initial();
        }
        // ONE_WAY terminates after the last stop: the driver releases the route and parks the train.
        host.dispatch().release(train.id());
        return state.withDone();
    }

    private AutoMotionState waitForRestart(Host host, RailTrainId trainId, AutoMotionState state) {
        if (state.retryTicks() > 0) {
            return state.withRetryTicks(state.retryTicks() - 1);
        }
        host.restartSchedule(trainId);
        return state.withRetryTicks(RESTART_RETRY_TICKS);
    }

    private static int dwellTicks(RailTrainAggregate train, int stopIndex) {
        Optional<RailTrainSchedule> schedule = train.schedule();
        if (schedule.isEmpty() || stopIndex >= schedule.get().stops().size()) {
            return DEFAULT_DWELL_TICKS;
        }
        int dwell = schedule.get().stops().get(stopIndex).dwellTicks();
        return dwell > 0 ? dwell : DEFAULT_DWELL_TICKS;
    }

    private static void moveTo(Host host, RailTrainAggregate train, RailEdgeView edge, StepView step,
                               double progress) {
        host.update(train.withPosition(cellAt(edge, step, progress)));
    }

    private static void moveToCell(Host host, RailTrainAggregate train, StepView step) {
        host.update(train.withPosition(step.exitCell()));
    }

    private static GridPos cellAt(RailEdgeView edge, StepView step, double progress) {
        GridPos from = step.entryCell();
        GridPos to = step.exitCell();
        double length = Math.max(1.0E-6, edge.lengthMeters());
        double fraction = Math.min(1.0, progress / length);
        return new GridPos(
                (int) Math.round(from.x() + (to.x() - from.x()) * fraction),
                from.y(),
                (int) Math.round(from.z() + (to.z() - from.z()) * fraction)
        );
    }

    private static TrackPosition trackPosition(RailEdgeView edge, StepView step, double progress) {
        double metersFromStart = step.direction() == TraversalDirection.REVERSE
                ? edge.lengthMeters() - progress
                : progress;
        return new TrackPosition(edge.id(), metersFromStart, step.direction());
    }

    /**
     * Progress along the step's travel direction converted to meters from the edge start.
     */
    private static double edgeProgress(RailEdgeView edge, StepView step, double progress) {
        return step.direction() == TraversalDirection.REVERSE
                ? edge.lengthMeters() - progress
                : progress;
    }

    /**
     * Meters from the edge start converted back to progress along the step's travel direction.
     */
    private static double edgeProgressToTravel(RailEdgeView edge, StepView step, double metersFromStart) {
        return step.direction() == TraversalDirection.REVERSE
                ? edge.lengthMeters() - metersFromStart
                : metersFromStart;
    }

    /**
     * Position (in meters from the edge start) of the next section boundary on this edge, if the current section
     * does not extend to the edge end. Only segments of the current edge are considered. The returned boundary is
     * the far side of the current section's span on this edge (its {@code toMeters}); the driver only crosses when
     * the section actually changes, so this is used to stop exactly at the transition.
     */
    private static Optional<Double> boundaryProgress(RailSectionView section, RailEdgeView edge,
                                                     RailGraphView graph) {
        double boundary = edge.lengthMeters();
        boolean found = false;
        for (TrackSegmentId segmentId : section.segments()) {
            Optional<TrackSegmentView> segment = graph.segmentById(segmentId);
            if (segment.isEmpty() || !segment.get().edgeId().equals(edge.id())) {
                continue;
            }
            boundary = Math.min(boundary, segment.get().toMeters());
            found = true;
        }
        return found ? Optional.of(boundary) : Optional.empty();
    }

    private static Optional<StepView> currentStep(RailRouteView route, AutoMotionState state) {
        if (state.stepIndex() >= route.steps().size()) {
            return Optional.empty();
        }
        return Optional.of(route.steps().get(state.stepIndex()));
    }

    private static RailRouteView routeView(RailTrainAggregate train, RailGraphView graph) {
        return new RailRouteView(train.route().orElseThrow(), graph);
    }

    private static TrainPresentation presentOn(StepView step, double fraction) {
        GridPos from = step.entryCell();
        GridPos to = step.exitCell();
        double x = from.x() + (to.x() - from.x()) * fraction + 0.5;
        double y = from.y() + 0.5;
        double z = from.z() + (to.z() - from.z()) * fraction + 0.5;
        float yaw = (float) Math.toDegrees(Math.atan2(to.z() - from.z(), to.x() - from.x()));
        return new TrainPresentation(x, y, z, yaw);
    }

    /**
     * World-space presentation of an automatic train for the entity renderer; pure data.
     */
    public record TrainPresentation(double x, double y, double z, float yawDegrees) {
        static TrainPresentation atCell(GridPos cell) {
            return new TrainPresentation(cell.x() + 0.5, cell.y() + 0.5, cell.z() + 0.5, 0.0F);
        }
    }

    /**
     * Immutable progress of one automatic train along its locked route.
     */
    record AutoMotionState(
            int stepIndex,
            double progress,
            int stopIndex,
            int dwellRemaining,
            int retryTicks,
            boolean done
    ) {
        static AutoMotionState initial() {
            return new AutoMotionState(0, 0.0, 0, 0, 0, false);
        }

        AutoMotionState withStepIndex(int stepIndex) {
            return new AutoMotionState(stepIndex, progress, stopIndex, dwellRemaining, retryTicks, done);
        }

        AutoMotionState withProgress(double progress) {
            return new AutoMotionState(stepIndex, progress, stopIndex, dwellRemaining, retryTicks, done);
        }

        AutoMotionState withStopIndex(int stopIndex) {
            return new AutoMotionState(stepIndex, progress, stopIndex, dwellRemaining, retryTicks, done);
        }

        AutoMotionState withDwellRemaining(int dwellRemaining) {
            return new AutoMotionState(stepIndex, progress, stopIndex, dwellRemaining, retryTicks, done);
        }

        AutoMotionState withRetryTicks(int retryTicks) {
            return new AutoMotionState(stepIndex, progress, stopIndex, dwellRemaining, retryTicks, done);
        }

        AutoMotionState withDone() {
            return new AutoMotionState(stepIndex, progress, stopIndex, dwellRemaining, retryTicks, true);
        }
    }

    /**
     * A locked route with ready access to its steps and the graph needed to resolve node ids to cells.
     */
    static final class RailRouteView {
        private final List<StepView> steps;

        RailRouteView(RailRoute route, RailGraphView graph) {
            this.steps = route.steps().stream().map(step -> new StepView(step, graph)).toList();
        }

        List<StepView> steps() {
            return steps;
        }
    }

    /**
     * One route step bound to the graph so entry/exit node ids resolve to cells without further plumbing.
     */
    static final class StepView {
        private final RailRoute.RailRouteStep delegate;
        private final RailGraphView graph;

        StepView(RailRoute.RailRouteStep delegate, RailGraphView graph) {
            this.delegate = delegate;
            this.graph = graph;
        }

        RailEdgeId edgeId() {
            return delegate.edgeId();
        }

        RailNodeId exit() {
            return delegate.exit();
        }

        TraversalDirection direction() {
            return delegate.direction();
        }

        GridPos entryCell() {
            return cellOf(delegate.entry());
        }

        GridPos exitCell() {
            return cellOf(delegate.exit());
        }

        private GridPos cellOf(RailNodeId nodeId) {
            return graph.nodes().stream()
                    .filter((RailNodeView node) -> node.id().equals(nodeId))
                    .findFirst()
                    .map(RailNodeView::pos)
                    .orElseThrow(() -> new IllegalStateException("Route step references unknown node: " + nodeId));
        }
    }
}