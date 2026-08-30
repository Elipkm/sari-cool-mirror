package at.sari.trader.strategy;

import at.sari.trader.market.DailyCandle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TrendPullbackStrategy {
    private static final int FAST = 20;
    private static final int TREND = 50;

    public StrategyDecision decide(List<DailyCandle> candles, boolean hasPosition) {
        if (candles.size() < TREND + 1) {
            BigDecimal price = candles.isEmpty() ? BigDecimal.ZERO : candles.get(candles.size() - 1).close();
            return StrategyDecision.hold(price, "need at least 51 daily candles");
        }

        int last = candles.size() - 1;
        BigDecimal close = candles.get(last).close();
        BigDecimal sma20 = average(candles, last - FAST + 1, last);
        BigDecimal sma50 = average(candles, last - TREND + 1, last);
        BigDecimal previousClose = candles.get(last - 1).close();
        BigDecimal previousSma20 = average(candles, last - FAST, last - 1);

        if (hasPosition) {
            if (close.compareTo(sma20) < 0) {
                return new StrategyDecision(StrategyDecision.Action.EXIT, close, null,
                        "daily close fell below 20-day average");
            }
            return StrategyDecision.hold(close, "position remains above 20-day average");
        }

        boolean uptrend = close.compareTo(sma50) > 0;
        boolean recoveredFromPullback = previousClose.compareTo(previousSma20) <= 0 && close.compareTo(sma20) > 0;
        if (uptrend && recoveredFromPullback) {
            BigDecimal recentLow = candles.subList(last - 4, last + 1).stream()
                    .map(DailyCandle::close)
                    .min(BigDecimal::compareTo)
                    .orElse(close);
            BigDecimal invalidation = recentLow.multiply(new BigDecimal("0.98"))
                    .setScale(8, RoundingMode.HALF_UP);
            return new StrategyDecision(StrategyDecision.Action.BUY, close, invalidation,
                    "uptrend above 50-day average and pullback recovered above 20-day average");
        }

        return StrategyDecision.hold(close, "no trend-pullback entry");
    }

    private BigDecimal average(List<DailyCandle> candles, int from, int to) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = from; i <= to; i++) sum = sum.add(candles.get(i).close());
        return sum.divide(BigDecimal.valueOf(to - from + 1L), 10, RoundingMode.HALF_UP);
    }
}
