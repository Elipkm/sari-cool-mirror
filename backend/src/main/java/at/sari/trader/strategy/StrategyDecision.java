package at.sari.trader.strategy;

import java.math.BigDecimal;

public record StrategyDecision(
        Action action,
        BigDecimal referencePrice,
        BigDecimal invalidationPrice,
        String reason
) {
    public enum Action { BUY, EXIT, HOLD }

    public static StrategyDecision hold(BigDecimal price, String reason) {
        return new StrategyDecision(Action.HOLD, price, null, reason);
    }
}
