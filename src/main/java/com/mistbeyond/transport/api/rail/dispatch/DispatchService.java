package com.mistbeyond.transport.api.rail.dispatch;

import com.mistbeyond.transport.api.rail.section.RailSectionId;

public interface DispatchService {
    DispatchResult start(RouteRequest request);

    AdvanceResult advance(RailTrainId trainId, RailSectionId currentSection);

    void claimManual(RailTrainId trainId, RailSectionId sectionId);

    void releaseManual(RailTrainId trainId, RailSectionId sectionId);

    void release(RailTrainId trainId);

    DispatchSnapshot snapshot();

    sealed interface DispatchResult permits DispatchResult.Accepted, DispatchResult.Rejected {
        record Accepted(RouteLock lock) implements DispatchResult {
        }

        record Rejected(String reason) implements DispatchResult {
        }
    }

    sealed interface AdvanceResult permits AdvanceResult.Move, AdvanceResult.Blocked, AdvanceResult.Arrived {
        record Move(RailSectionId nextSection) implements AdvanceResult {
        }

        record Blocked(RailSectionId currentSection) implements AdvanceResult {
        }

        record Arrived() implements AdvanceResult {
        }
    }
}
