package at.sari.trader.validation;

import at.sari.trader.simulation.HistoricalSimulationService;
import at.sari.trader.simulation.SimulationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class StrategyValidationService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TEN = new BigDecimal("10.00");
    private static final BigDecimal NEGATIVE_TEN = new BigDecimal("-10.00");
    private static final BigDecimal FIFTEEN = new BigDecimal("15.00");

    private final HistoricalSimulationService simulationService;

    public StrategyValidationService(HistoricalSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public synchronized StrategyValidationReport evaluate() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        ValidationPeriodResult development = period(
                "Development", today.minusDays(540), today.minusDays(361));
        ValidationPeriodResult validation = period(
                "Validation", today.minusDays(360), today.minusDays(181));
        ValidationPeriodResult outOfSample = period(
                "Out of sample", today.minusDays(180), today.minusDays(1));
        List<ValidationPeriodResult> periods = List.of(development, validation, outOfSample);

        int positivePeriods = (int) periods.stream()
                .filter(period -> period.strategyReturnPct().compareTo(ZERO) > 0).count();
        List<ValidationCriterion> criteria = List.of(
                criterion("Positive out-of-sample return",
                        outOfSample.strategyReturnPct().compareTo(ZERO) > 0,
                        percent(outOfSample.strategyReturnPct()), "> 0%"),
                criterion("Out-of-sample beats benchmark",
                        outOfSample.excessReturnPct().compareTo(ZERO) >= 0,
                        percent(outOfSample.excessReturnPct()) + " excess", ">= 0% excess"),
                criterion("Out-of-sample drawdown limit",
                        outOfSample.maxDrawdownPct().compareTo(TEN) <= 0,
                        percent(outOfSample.maxDrawdownPct()), "<= 10%"),
                criterion("Sufficient out-of-sample trades",
                        outOfSample.completedTrades() >= 5,
                        outOfSample.completedTrades() + " completed", ">= 5 completed"),
                criterion("Positive across periods",
                        positivePeriods >= 2,
                        positivePeriods + " of 3 positive", ">= 2 of 3 positive")
        );

        boolean allPass = criteria.stream().allMatch(ValidationCriterion::passed);
        boolean reject = periods.stream().anyMatch(period -> period.maxDrawdownPct().compareTo(FIFTEEN) > 0)
                || outOfSample.strategyReturnPct().compareTo(NEGATIVE_TEN) <= 0
                || positivePeriods == 0;
        String verdict = allPass ? "ACCEPT" : reject ? "REJECT" : "REVISE";
        String summary = switch (verdict) {
            case "ACCEPT" -> "The frozen strategy passed every historical acceptance criterion.";
            case "REJECT" -> "The frozen strategy hit a rejection boundary and should not progress toward live trading.";
            default -> "The frozen strategy is operationally controlled but lacks enough evidence of an edge; revise only from measured weaknesses.";
        };
        return new StrategyValidationReport(
                Instant.now(), "trend-pullback-v1 @ v0.6.0", periods, criteria, verdict, summary);
    }

    private ValidationPeriodResult period(String label, LocalDate start, LocalDate end) {
        SimulationResult result = simulationService.runRange(start, end);
        BigDecimal excess = result.returnPct().subtract(result.buyAndHoldReturnPct());
        return new ValidationPeriodResult(
                label, result.startDate(), result.currentDate(), regime(result.buyAndHoldReturnPct()),
                result.iteration(), result.returnPct(), result.buyAndHoldReturnPct(), excess,
                result.maxDrawdownPct(), result.completedTrades(), result.winRatePct(), result.totalFeesEur());
    }

    private String regime(BigDecimal benchmarkReturn) {
        if (benchmarkReturn.compareTo(TEN) > 0) return "BULL";
        if (benchmarkReturn.compareTo(NEGATIVE_TEN) < 0) return "BEAR";
        return "SIDEWAYS";
    }

    private ValidationCriterion criterion(String name, boolean passed, String actual, String requirement) {
        return new ValidationCriterion(name, passed, actual, requirement);
    }

    private String percent(BigDecimal value) {
        return value.setScale(2) + "%";
    }
}
