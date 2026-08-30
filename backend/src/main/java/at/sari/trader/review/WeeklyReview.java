package at.sari.trader.review;

import at.sari.trader.paper.PaperTrade;
import at.sari.trader.risk.CircuitBreakerState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** The small, stable read model used by the weekly dashboard. */
public record WeeklyReview(
        BigDecimal equityEur,
        BigDecimal cashEur,
        BigDecimal returnPct,
        BigDecimal weeklyReturnPct,
        BigDecimal maxDrawdownPct,
        CircuitBreakerState systemState,
        Map<String, BigDecimal> positionsEur,
        List<PaperTrade> recentTrades
) {}
