package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.Ids;
import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-bound payload carrying the resolved RED/GREEN aspect plus ERROR health indicator of every rail signal,
 * broadcast by the server so the client F3 debug overlay can color signal markers. The state itself is resolved
 * server-side from dispatch reservations and manual claims (see {@link SignalStateResolver}); this payload is only
 * the transport view.
 */
public record RailSignalsPayload(List<SignalState> states) implements CustomPacketPayload {
    public static final Type<RailSignalsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rail_signals"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RailSignalsPayload> STREAM_CODEC =
            StreamCodec.ofMember(RailSignalsPayload::encode, RailSignalsPayload::decode);

    public RailSignalsPayload {
        states = List.copyOf(states);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void encode(RailSignalsPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.states().size());
        for (SignalState state : payload.states()) {
            buf.writeUtf(state.id().value());
            buf.writeByte(state.aspect().ordinal());
            buf.writeBoolean(state.error());
        }
    }

    static RailSignalsPayload decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<SignalState> states = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            SignalId id = new SignalId(buf.readUtf());
            SignalAspect aspect = SignalAspect.values()[buf.readByte()];
            boolean error = buf.readBoolean();
            states.add(new SignalState(id, aspect, error));
        }
        return new RailSignalsPayload(states);
    }

}