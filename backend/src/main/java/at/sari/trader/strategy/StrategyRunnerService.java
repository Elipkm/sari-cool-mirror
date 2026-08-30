package at.sari.trader.strategy;

import at.sari.trader.market.MarketHistoryProvider;
import at.sari.trader.paper.PaperPositionRepository;
import at.sari.trader.paper.PaperTradeResult;
import at.sari.trader.paper.PaperTradingService;
import at.sari.trader.risk.TradeProposal;
import org.springframework.stereotype.Service;

@Service
public class StrategyRunnerService {
    private final MarketHistoryProvider historyProvider;
    private final TrendPullbackStrategy strategy;
    private final PaperPositionRepository positionRepository;
    private final PaperTradingService paperTradingService;

    public StrategyRunnerService(
            MarketHistoryProvider historyProvider,
            TrendPullbackStrategy strategy,
            PaperPositionRepository positionRepository,
            PaperTradingService paperTradingService
    ) {
        this.historyProvider = historyProvider;
        this.strategy = strategy;
        this.positionRepository = positionRepository;
        this.paperTradingService = paperTradingService;
    }

    public StrategyRunResult run(String asset) {
        String symbol = asset.toUpperCase();
        boolean hasPosition = positionRepository.findById(symbol).map(p -> p.isOpen()).orElse(false);
        StrategyDecision decision = strategy.decide(historyProvider.dailyCandles(symbol, 120), hasPosition);

        PaperTradeResult trade = null;
        if (decision.action() == StrategyDecision.Action.BUY) {
            TradeProposal proposal = new TradeProposal(
                    symbol,
                    TradeProposal.Side.BUY,
                    "trend_pullback",
                    decision.referencePrice(),
                    decision.invalidationPrice(),
                    1.0,
                    decision.reason()
            );
            trade = paperTradingService.execute(proposal);
        } else if (decision.action() == StrategyDecision.Action.EXIT) {
            trade = paperTradingService.exitPosition(symbol, "trend_pullback", decision.reason());
        }

        return new StrategyRunResult(symbol, decision.action(), decision.reason(), trade);
    }
}
