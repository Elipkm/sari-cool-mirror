package at.sari.trader.risk;

public enum CircuitBreakerState {
    NORMAL,
    CAUTION,
    REDUCE_ONLY,
    HALTED
}
