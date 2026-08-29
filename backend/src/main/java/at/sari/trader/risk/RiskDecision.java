package at.sari.trader.risk;

import java.util.List;

public record RiskDecision(boolean allowed, List<String> reasons) {
    public static RiskDecision allow() { return new RiskDecision(true, List.of("proposal satisfies hard risk checks")); }
    public static RiskDecision reject(List<String> reasons) { return new RiskDecision(false, List.copyOf(reasons)); }
}
