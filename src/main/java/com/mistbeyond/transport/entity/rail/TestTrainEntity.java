package com.mistbeyond.transport.entity.rail;

import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.RailEdgeId;
import com.mistbeyond.transport.api.rail.graph.RailEdgeView;
import com.mistbeyond.transport.api.rail.graph.RailGraphView;
import com.mistbeyond.transport.api.rail.graph.RailNodeId;
import com.mistbeyond.transport.api.rail.graph.RailNodeView;
import com.mistbeyond.transport.block.rail.TestTrackBlock;
import com.mistbeyond.transport.core.rail.RailNetworkService;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("resource")
public class TestTrainEntity extends Entity {
    private static final double MAX_SPEED = 0.12;
    private static final double ACCELERATION = 0.012;

    @Nullable
    private RailGraphView graph;
    @Nullable
    private EdgeState currentEdge;
    @Nullable
    private GridPos startPos;
    private Map<RailNodeId, List<EdgeEnd>> adjacency = Map.of();
    private double speed;
    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public TestTrainEntity(EntityType<? extends TestTrainEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public void begin(RailGraphView graph, GridPos startPos) {
        this.graph = graph;
        this.startPos = startPos;
        this.adjacency = buildAdjacency(graph);
        this.currentEdge = firstEdge(graph, startPos);
        this.speed = 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.interpolation.interpolate();
            return;
        }
        if (this.graph == null || this.currentEdge == null) {
            return;
        }
        if (!(this.getPassengers().stream().findFirst().orElse(null) instanceof ServerPlayer player)) {
            this.speed = 0.0;
            this.moveToCurrentPosition();
            return;
        }

        Input playerInput = player.getLastClientInput();
        float inputValue = playerInput.forward() == playerInput.backward()
                ? 0.0F
                : playerInput.forward() ? 1.0F : -1.0F;
        double targetSpeed = inputValue * MAX_SPEED;
        this.speed = Mth.approach((float) this.speed, (float) targetSpeed, (float) ACCELERATION);
        if (Math.abs(this.speed) < 1.0E-5) {
            this.moveToCurrentPosition();
            return;
        }

        EdgeState edge = this.currentEdge;
        double nextProgress = edge.progress + this.speed;
        if (nextProgress >= edge.length) {
            if (advanceAtNode(edge, false)) {
                this.currentEdge.progress = 0.0;
            } else {
                edge.progress = edge.length;
                this.speed = 0.0;
            }
        } else if (nextProgress <= 0.0) {
            if (advanceAtNode(edge, true)) {
                this.currentEdge.progress = this.currentEdge.length;
            } else {
                edge.progress = 0.0;
                this.speed = 0.0;
            }
        } else {
            edge.progress = nextProgress;
        }
        this.moveToCurrentPosition();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        this.kill(level);
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.getPassengers().isEmpty() && player.startRiding(this)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (this.startPos == null || this.currentEdge == null) {
            return;
        }
        output.putInt("StartX", this.startPos.x());
        output.putInt("StartY", this.startPos.y());
        output.putInt("StartZ", this.startPos.z());
        output.putString("EdgeId", this.currentEdge.edgeId.value());
        output.putDouble("Progress", this.currentEdge.progress);
        output.putDouble("Speed", this.speed);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        GridPos savedStart = new GridPos(
                input.getIntOr("StartX", this.getBlockX()),
                input.getIntOr("StartY", this.getBlockY()),
                input.getIntOr("StartZ", this.getBlockZ())
        );
        String edgeId = input.getStringOr("EdgeId", "");
        double progress = input.getDoubleOr("Progress", 0.0);
        double savedSpeed = input.getDoubleOr("Speed", 0.0);
        if (this.level() instanceof ServerLevel serverLevel) {
            RailGraphView graph = RailNetworkService.collectGraph(TestTrackBlock.source(serverLevel), savedStart);
            this.begin(graph, savedStart);
            if (!edgeId.isEmpty()) {
                this.currentEdge = graph.edgeById(new RailEdgeId(edgeId))
                        .map(edge -> edgeStateFor(edge, savedStart))
                        .orElseGet(() -> firstEdge(graph, savedStart));
            }
            if (this.currentEdge != null) {
                this.currentEdge.progress = progress;
            }
            this.speed = savedSpeed;
        }
    }

    private boolean advanceAtNode(EdgeState edge, boolean backward) {
        EdgeState next = nextEdge(this.adjacency, edge, backward);
        if (next == null) {
            return false;
        }
        this.currentEdge = next;
        return true;
    }

    @Nullable
    static EdgeState nextEdge(Map<RailNodeId, List<EdgeEnd>> adjacency, EdgeState edge, boolean backward) {
        RailNodeId nodeId = backward ? edge.from : edge.to;
        GridPos nodePos = backward ? edge.fromPos : edge.toPos;
        GridPos heading = backward
                ? new GridPos(edge.fromPos.x() - edge.toPos.x(), 0, edge.fromPos.z() - edge.toPos.z())
                : new GridPos(edge.toPos.x() - edge.fromPos.x(), 0, edge.toPos.z() - edge.fromPos.z());
        List<EdgeEnd> candidates = adjacency.getOrDefault(nodeId, List.of()).stream()
                .filter(candidate -> !candidate.edgeId().equals(edge.edgeId))
                .toList();
        EdgeEnd next = chooseNextEdge(candidates, nodePos, heading);
        if (next == null) {
            return null;
        }
        if (backward) {
            EdgeState result = new EdgeState(
                    next.edgeId(),
                    next.to(),
                    next.from(),
                    next.toPos(),
                    next.fromPos(),
                    next.length()
            );
            result.progress = result.length;
            return result;
        }
        return new EdgeState(
                next.edgeId(),
                next.from(),
                next.to(),
                next.fromPos(),
                next.toPos(),
                next.length()
        );
    }

    @Nullable
    static EdgeEnd chooseNextEdge(List<EdgeEnd> candidates, GridPos nodePos, GridPos heading) {
        double headingLength = Math.max(1.0E-6, headingDistance(heading));
        EdgeEnd best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (EdgeEnd candidate : candidates) {
            double dx = candidate.toPos().x() - nodePos.x();
            double dz = candidate.toPos().z() - nodePos.z();
            double candidateLength = Math.max(1.0E-6, Math.sqrt(dx * dx + dz * dz));
            double dot = (heading.x() * dx + heading.z() * dz) / (headingLength * candidateLength);
            double score = 1.0 - dot;
            if (score < bestScore || score == bestScore && best != null
                    && candidate.edgeId().value().compareTo(best.edgeId().value()) < 0) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static double headingDistance(GridPos heading) {
        return Math.sqrt(heading.x() * (double) heading.x() + heading.z() * (double) heading.z());
    }

    static Map<RailNodeId, List<EdgeEnd>> buildAdjacency(RailGraphView graph) {
        Map<RailNodeId, List<EdgeEnd>> result = new HashMap<>();
        for (RailEdgeView edge : graph.edges()) {
            GridPos start = edge.placement().start();
            GridPos end = edge.placement().end();
            addEdgeEnd(result, new EdgeEnd(edge.id(), edge.start(), edge.end(), start, end, edge.lengthMeters()));
            addEdgeEnd(result, new EdgeEnd(edge.id(), edge.end(), edge.start(), end, start, edge.lengthMeters()));
        }
        return result;
    }

    private static void addEdgeEnd(Map<RailNodeId, List<EdgeEnd>> adjacency, EdgeEnd edgeEnd) {
        adjacency.computeIfAbsent(edgeEnd.from(), ignored -> new ArrayList<>()).add(edgeEnd);
    }

    @Nullable
    static EdgeState firstEdge(RailGraphView graph, GridPos startPos) {
        RailNodeId startNode = graph.nodeAt(startPos).map(RailNodeView::id).orElse(null);
        if (startNode == null) {
            return null;
        }
        for (RailEdgeView edge : graph.edges()) {
            GridPos start = edge.placement().start();
            GridPos end = edge.placement().end();
            if (edge.start().equals(startNode)) {
                return new EdgeState(edge.id(), edge.start(), edge.end(), start, end, edge.lengthMeters());
            }
            if (edge.end().equals(startNode)) {
                return new EdgeState(edge.id(), edge.end(), edge.start(), end, start, edge.lengthMeters());
            }
        }
        return null;
    }

    private static EdgeState edgeStateFor(RailEdgeView edge, GridPos fromPos) {
        GridPos start = edge.placement().start();
        GridPos end = edge.placement().end();
        if (fromPos.equals(start)) {
            return new EdgeState(edge.id(), edge.start(), edge.end(), start, end, edge.lengthMeters());
        }
        return new EdgeState(edge.id(), edge.end(), edge.start(), end, start, edge.lengthMeters());
    }

    private void moveToCurrentPosition() {
        EdgeState edge = this.currentEdge;
        if (edge == null) {
            return;
        }
        this.setOldPosAndRot();
        double dx = edge.toPos.x() - edge.fromPos.x();
        double dz = edge.toPos.z() - edge.fromPos.z();
        double fraction = edge.length > 0.0 ? Math.min(1.0, edge.progress / edge.length) : 0.0;
        double x = edge.fromPos.x() + dx * fraction + 0.5;
        double y = edge.fromPos.y() + 0.5;
        double z = edge.fromPos.z() + dz * fraction + 0.5;
        double length = Math.max(1.0E-6, edge.length);
        this.setDeltaMovement(dx / length * this.speed, 0.0, dz / length * this.speed);
        this.setPos(x, y, z);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx));
        this.setYRot(yaw);
    }

    record EdgeEnd(
            RailEdgeId edgeId,
            RailNodeId from,
            RailNodeId to,
            GridPos fromPos,
            GridPos toPos,
            double length
    ) {
    }

    static final class EdgeState {
        final RailEdgeId edgeId;
        final RailNodeId from;
        final RailNodeId to;
        final GridPos fromPos;
        final GridPos toPos;
        final double length;
        double progress;

        EdgeState(
                RailEdgeId edgeId,
                RailNodeId from,
                RailNodeId to,
                GridPos fromPos,
                GridPos toPos,
                double length
        ) {
            this.edgeId = edgeId;
            this.from = from;
            this.to = to;
            this.fromPos = fromPos;
            this.toPos = toPos;
            this.length = length;
        }
    }
}
