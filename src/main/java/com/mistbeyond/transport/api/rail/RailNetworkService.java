package com.mistbeyond.transport.api.rail;

import com.mistbeyond.transport.api.rail.dispatch.RailTrainId;

import java.util.Optional;

/**
 * Read-only entry point into a per-world rail network. Mutations are performed through the concrete
 * core manager so that external consumers cannot bypass the server main thread.
 */
public interface RailNetworkService {
    RailNetworkSnapshot snapshot();

    Optional<RailTrainSnapshot> train(RailTrainId trainId);
}
