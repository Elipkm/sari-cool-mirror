package at.sari.trader.validation;

import java.time.Instant;
import java.util.List;

public record StrategyValidationReport(
        Instant evaluatedAt,
        String frozenStrategy,
        List<ValidationPeriodResult> periods,
        List<ValidationCriterion> criteria,
        String verdict,
        String summary
) {}
