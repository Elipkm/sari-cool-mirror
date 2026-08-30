package at.sari.trader.validation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ValidationPeriodResult(
        String label,
        LocalDate startDate,
        LocalDate endDate,
        String regime,
        int days,
        BigDecimal strategyReturnPct,
        BigDecimal benchmarkReturnPct,
        BigDecimal excessReturnPct,
        BigDecimal maxDrawdownPct,
        int completedTrades,
        BigDecimal winRatePct,
        BigDecimal totalFeesEur
) {}
