package com.mistbeyond.transport.item.rail;

import com.mistbeyond.transport.api.rail.graph.GridDirection;
import com.mistbeyond.transport.api.rail.graph.GridPos;
import com.mistbeyond.transport.api.rail.graph.SignalPlacement;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalType;
import com.mistbeyond.transport.block.rail.RailTrackCellBlock;
import com.mistbeyond.transport.block.rail.RailTrackCellBlockEntity;
import com.mistbeyond.transport.core.rail.RailNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Base item for placing rail signals onto {@link RailTrackCellBlock} cells. The item does not place a separate
 * world block; it adds a {@link SignalPlacement} to the track cell's {@link RailTrackCellBlockEntity}. The signal's
 * facing direction is the player's look direction at placement (sections.md), and re-facing is done by break-and-replace
 * (sneak + use to remove). The model is a placeholder flat item that will be replaced when the real signal models are ready.
 */
public abstract class RailSignalItem extends Item {
    private final SignalType signalType;

    protected RailSignalItem(Properties properties, SignalType signalType) {
        super(properties);
        this.signalType = signalType;
    }

    public SignalType signalType() {
        return signalType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RailTrackCellBlock)) {
            if (!level.isClientSide() && context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.literal("Signal must be placed on a rail track cell."));
            }
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return tryRemove(level, pos, player);
        }
        return tryPlace(context, level, pos, state, player);
    }

    private InteractionResult tryPlace(UseOnContext context, Level level, BlockPos pos, BlockState state, Player player) {
        GridDirection direction = directionFromContext(context, player);
        GridPos cell = new GridPos(pos.getX(), pos.getY(), pos.getZ());
        BlockEntity be = level.getBlockEntity(pos);
        Set<SignalPlacement> existing;
        RailTrackCellBlockEntity trackBe;
        // Resolve existing signals and track placements. Per runtime-contract simple cells have no BE; a signal turns it into a complex cell.
        if (be instanceof RailTrackCellBlockEntity complex && complex.cell().equals(cell)) {
            existing = complex.signals();
            trackBe = complex;
        } else {
            // Simple cell without BE -> upgrade to complex on server (runtime-contract: simple→complex must create BE and preserve placements).
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.FAIL;
            }
            Set<com.mistbeyond.transport.api.rail.graph.TrackPlacement> placements = RailTrackCellBlock.placementsFor(cell, state.getValue(RailTrackCellBlock.DIRECTION));
            RailTrackCellBlockEntity created = new RailTrackCellBlockEntity(pos, state);
            created.setData(placements, Set.of());
            // Install the new BE into the chunk (LevelChunk#setBlockEntity is the 1.21.11 path for manual BE creation).
            LevelChunk chunk = (LevelChunk) level.getChunk(pos);
            chunk.setBlockEntity(created);
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
            trackBe = created;
            existing = Set.of();
        }

        // Enforce placement constraint: at most one signal per cell per facing direction (ADR 0008, sections.md).
        for (SignalPlacement s : existing) {
            if (s.direction() == direction) {
                if (!level.isClientSide() && player != null) {
                    player.sendSystemMessage(Component.literal("A signal already faces " + direction + " in this cell. Sneak+use to remove it first."));
                }
                return InteractionResult.FAIL;
            }
        }
        // Opposite-direction sharing is allowed, but we cap at 8 distinct directions for sanity.
        if (existing.size() >= 8) {
            if (!level.isClientSide() && player != null) {
                player.sendSystemMessage(Component.literal("Too many signals in this cell."));
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        String id = "signal-" + cell.x() + "-" + cell.y() + "-" + cell.z() + "-" + direction.name().toLowerCase() + "-" + signalType.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6);
        SignalPlacement placement = new SignalPlacement(new SignalId(id), direction, signalType);
        Set<SignalPlacement> next = new HashSet<>(existing);
        next.add(placement);
        // Track placements stay as they are.
        trackBe.setData(trackBe.placements(), Set.copyOf(next));
        level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        RailNetworkManager.of(serverLevel).markCellChanged(cell);
        if (player != null && !player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        if (player != null) {
            player.sendSystemMessage(Component.literal((signalType == SignalType.BLOCK ? "Block" : "Path") + " signal placed facing " + direction + "."));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult tryRemove(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        GridPos cell = new GridPos(pos.getX(), pos.getY(), pos.getZ());
        if (!(be instanceof RailTrackCellBlockEntity complex) || !complex.cell().equals(cell)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("No signal data at this cell."));
            }
            return InteractionResult.FAIL;
        }
        if (complex.signals().isEmpty()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("No signals to remove in this cell."));
            }
            return InteractionResult.FAIL;
        }
        GridDirection look = directionFromPlayer(player);
        // Prefer removing a signal that matches the player's look direction; otherwise remove an arbitrary one.
        SignalPlacement toRemove = null;
        for (SignalPlacement s : complex.signals()) {
            if (s.direction() == look && s.type() == signalType) {
                toRemove = s;
                break;
            }
        }
        if (toRemove == null) {
            for (SignalPlacement s : complex.signals()) {
                if (s.direction() == look) {
                    toRemove = s;
                    break;
                }
            }
        }
        if (toRemove == null) {
            // Fallback: remove any signal of our type, else any signal.
            for (SignalPlacement s : complex.signals()) {
                if (s.type() == signalType) {
                    toRemove = s;
                    break;
                }
            }
            if (toRemove == null) {
                toRemove = complex.signals().iterator().next();
            }
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        Set<SignalPlacement> next = new HashSet<>(complex.signals());
        next.remove(toRemove);
        BlockState state = level.getBlockState(pos);
        if (next.isEmpty() && isSimplePlacements(complex.placements())) {
            // Downgrade complex→simple per runtime-contract: remove BE and write remaining placement back to BlockState.
            level.removeBlockEntity(pos);
            // Derive the axis from the remaining placements (they are the two opposite directions of one axis).
            // The BlockState already stores that axis, so we just trigger a block update to refresh the chunk mesh.
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        } else {
            complex.setData(complex.placements(), Set.copyOf(next));
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        RailNetworkManager.of(serverLevel).markCellChanged(cell);
        player.sendSystemMessage(Component.literal("Removed " + toRemove.type() + " signal facing " + toRemove.direction() + "."));
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean isSimplePlacements(Set<com.mistbeyond.transport.api.rail.graph.TrackPlacement> placements) {
        if (placements.size() != 2) {
            return false;
        }
        // Simple cell: exactly the two opposite directions of one axis, STRAIGHT or DIAGONAL_45, no curve.
        var it = placements.iterator();
        var a = it.next();
        var b = it.next();
        if (a.trackType() != b.trackType()) {
            return false;
        }
        if (a.trackType() != com.mistbeyond.transport.api.rail.graph.TrackType.STRAIGHT
                && a.trackType() != com.mistbeyond.transport.api.rail.graph.TrackType.DIAGONAL_45) {
            return false;
        }
        return a.direction().opposite() == b.direction() && a.originCell().equals(b.originCell());
    }

    private static GridDirection directionFromContext(UseOnContext context, Player player) {
        if (player != null) {
            return directionFromPlayer(player);
        }
        // Fallback to horizontal direction of the click face.
        return switch (context.getHorizontalDirection()) {
            case NORTH -> GridDirection.NORTH;
            case SOUTH -> GridDirection.SOUTH;
            case WEST -> GridDirection.WEST;
            case EAST -> GridDirection.EAST;
            default -> GridDirection.EAST;
        };
    }

    private static GridDirection directionFromPlayer(Player player) {
        return RailTrackCellBlock.directionFromYaw(player.getYRot());
    }
}
