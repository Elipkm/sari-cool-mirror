package at.sari.trader.strategy;

import java.math.BigDecimal;

public record BacktestResult(
        String asset,
        int candles,
        int completedTrades,
        BigDecimal startingCapitalEur,
        BigDecimal endingCapitalEur,
        BigDecimal strategyReturnPct,
        BigDecimal buyAndHoldReturnPct,
        BigDecimal maxDrawdownPct
) {}
