package at.sari.trader.strategy;

import at.sari.trader.market.DailyCandle;
import at.sari.trader.market.MarketHistoryProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BacktestService {
    private static final BigDecimal START = new BigDecimal("5000.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final MarketHistoryProvider historyProvider;
    private final TrendPullbackStrategy strategy;
    private final BigDecimal feePct;
    private final BigDecimal slippagePct;

    public BacktestService(
            MarketHistoryProvider historyProvider,
            TrendPullbackStrategy strategy,
            @Value("${trading.paper.fee-pct:0.25}") BigDecimal feePct,
            @Value("${trading.paper.slippage-pct:0.10}") BigDecimal slippagePct
    ) {
        this.historyProvider = historyProvider;
        this.strategy = strategy;
        this.feePct = feePct;
        this.slippagePct = slippagePct;
    }

    public BacktestResult run(String asset) {
        String symbol = asset.toUpperCase();
        List<DailyCandle> candles = historyProvider.dailyCandles(symbol, 365);
        if (candles.size() < 60) throw new IllegalStateException("not enough daily history");

        BigDecimal cash = START;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal peak = START;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        int trades = 0;

        for (int i = 51; i < candles.size(); i++) {
            List<DailyCandle> window = candles.subList(0, i + 1);
            boolean hasPosition = quantity.signum() > 0;
            StrategyDecision decision = strategy.decide(window, hasPosition);
            BigDecimal close = candles.get(i).close();

            if (!hasPosition && decision.action() == StrategyDecision.Action.BUY) {
                BigDecimal notional = cash.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.DOWN);
                BigDecimal fill = close.multiply(BigDecimal.ONE.add(slippagePct.divide(HUNDRED, 10, RoundingMode.HALF_UP)));
                BigDecimal fee = notional.multiply(feePct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
                quantity = notional.divide(fill, 12, RoundingMode.DOWN);
                cash = cash.subtract(notional).subtract(fee);
            } else if (hasPosition && decision.action() == StrategyDecision.Action.EXIT) {
                BigDecimal fill = close.multiply(BigDecimal.ONE.subtract(slippagePct.divide(HUNDRED, 10, RoundingMode.HALF_UP)));
                BigDecimal notional = quantity.multiply(fill);
                BigDecimal fee = notional.multiply(feePct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
                cash = cash.add(notional).subtract(fee);
                quantity = BigDecimal.ZERO;
                trades++;
            }

            BigDecimal equity = cash.add(quantity.multiply(close));
            if (equity.compareTo(peak) > 0) peak = equity;
            if (peak.signum() > 0 && equity.compareTo(peak) < 0) {
                BigDecimal dd = peak.subtract(equity).divide(peak, 10, RoundingMode.HALF_UP).multiply(HUNDRED);
                if (dd.compareTo(maxDrawdown) > 0) maxDrawdown = dd;
            }
        }

        BigDecimal last = candles.get(candles.size() - 1).close();
        BigDecimal ending = cash.add(quantity.multiply(last)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal strategyReturn = ending.subtract(START).divide(START, 10, RoundingMode.HALF_UP).multiply(HUNDRED);
        BigDecimal first = candles.get(0).close();
        BigDecimal holdReturn = last.subtract(first).divide(first, 10, RoundingMode.HALF_UP).multiply(HUNDRED);

        return new BacktestResult(symbol, candles.size(), trades, START, ending,
                strategyReturn.setScale(2, RoundingMode.HALF_UP),
                holdReturn.setScale(2, RoundingMode.HALF_UP),
                maxDrawdown.setScale(2, RoundingMode.HALF_UP));
    }
}
