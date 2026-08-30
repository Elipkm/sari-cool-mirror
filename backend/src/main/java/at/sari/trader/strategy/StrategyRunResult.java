package at.sari.trader.strategy;

import at.sari.trader.paper.PaperTradeResult;

public record StrategyRunResult(
        String asset,
        StrategyDecision.Action action,
        String reason,
        PaperTradeResult trade
) {}
