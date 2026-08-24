package com.mistbeyond.transport.api.rail.section;

public record SignalState(
        SignalId id,
        SignalAspect aspect,
        boolean error
) {
    /**
     * @param error configuration/health indicator for a misconfigured signal (no track on its facing side, or a path
     *              signal whose facing side never reaches a routing node). The ERROR indicator is not a third aspect:
     *              a misconfigured signal still carries a RED/GREEN {@code aspect} but reports {@code error} and behaves
     *              as RED (ADR 0008, docs/roadmap/rail/sections.md).
     */
    public SignalState {
    }
}
