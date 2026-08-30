package at.sari.trader.simulation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SimulationResult(
        LocalDate startDate, LocalDate currentDate, int iteration,
        BigDecimal startingCapitalEur, BigDecimal equityEur, BigDecimal cashEur,
        BigDecimal returnPct, BigDecimal buyAndHoldReturnPct, BigDecimal maxDrawdownPct,
        int completedTrades, BigDecimal winRatePct, BigDecimal averageTradePnlEur,
        BigDecimal totalFeesEur, Map<String, BigDecimal> positionsEur,
        List<SimulationDecision> decisions, List<EquityPoint> equityCurve,
        List<SimulationTrade> trades, boolean hasNextDay
) {}
