package at.sari.trader.risk;

import java.math.BigDecimal;
import java.util.List;

public record RiskDecision(
        boolean allowed,
        BigDecimal approvedNotionalEur,
        CircuitBreakerState circuitBreakerState,
        List<String> reasons
) {
    public static RiskDecision allow(BigDecimal approvedNotionalEur, CircuitBreakerState state) {
        return new RiskDecision(true, approvedNotionalEur, state,
                List.of("proposal satisfies hard risk checks"));
    }

    public static RiskDecision reject(CircuitBreakerState state, List<String> reasons) {
        return new RiskDecision(false, BigDecimal.ZERO, state, List.copyOf(reasons));
    }
}
