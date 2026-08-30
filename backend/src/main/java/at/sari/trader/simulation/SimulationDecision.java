package at.sari.trader.simulation;

import java.math.BigDecimal;

public record SimulationDecision(
        String asset,
        String action,
        String execution,
        BigDecimal closeEur,
        BigDecimal approvedNotionalEur,
        String reason
) {}
