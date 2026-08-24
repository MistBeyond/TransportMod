package com.mistbeyond.transport.core.rail;

import com.mistbeyond.transport.api.rail.section.SignalAspect;
import com.mistbeyond.transport.api.rail.section.SignalId;
import com.mistbeyond.transport.api.rail.section.SignalState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailSignalsPayloadTest {
    @Test
    void roundTripsSignalStates() {
        RailSignalsPayload payload = new RailSignalsPayload(List.of(
                new SignalState(new SignalId("sig-a"), SignalAspect.RED, false),
                new SignalState(new SignalId("sig-b"), SignalAspect.GREEN, true)
        ));

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RailSignalsPayload.encode(payload, buf);
        buf.readerIndex(0);

        assertEquals(payload, RailSignalsPayload.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void roundTripsEmptyPayload() {
        RailSignalsPayload payload = new RailSignalsPayload(List.of());

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RailSignalsPayload.encode(payload, buf);
        buf.readerIndex(0);

        assertEquals(payload, RailSignalsPayload.decode(buf));
        assertEquals(0, buf.readableBytes());
    }
}