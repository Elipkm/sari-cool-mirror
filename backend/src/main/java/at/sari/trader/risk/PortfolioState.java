package at.sari.trader.risk;

import java.math.BigDecimal;
import java.util.Map;

/** Trusted state reconstructed from broker/exchange balances, positions and open orders. */
public record PortfolioState(
        BigDecimal equityEur,
        BigDecimal startOfDayEquityEur,
        BigDecimal highWaterMarkEquityEur,
        BigDecimal cashEur,
        Map<String, BigDecimal> positionNotionalEur,
        Map<String, BigDecimal> openOrderNotionalEur,
        CircuitBreakerState circuitBreakerState
) {
    public BigDecimal exposureFor(String asset) {
        String symbol = asset.toUpperCase();
        return positionNotionalEur.getOrDefault(symbol, BigDecimal.ZERO)
                .add(openOrderNotionalEur.getOrDefault(symbol, BigDecimal.ZERO));
    }

    public BigDecimal totalCryptoExposureEur() {
        return positionNotionalEur.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(openOrderNotionalEur.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
