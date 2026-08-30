package at.sari.trader.automation;

import at.sari.trader.strategy.StrategyRunResult;

import java.time.Instant;
import java.util.List;

public record AutomationRunResult(
        Instant startedAt,
        Instant completedAt,
        String status,
        List<StrategyRunResult> assets,
        String summary
) {}
