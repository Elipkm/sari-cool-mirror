package at.sari.trader.market;

import java.util.List;

public interface MarketHistoryProvider {
    List<DailyCandle> dailyCandles(String asset, int limit);
}
