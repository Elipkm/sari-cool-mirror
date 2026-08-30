package at.sari.trader.market;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
public class KrakenMarketHistoryProvider implements MarketHistoryProvider {
    private final RestClient client = RestClient.create("https://api.kraken.com");

    @Override
    @SuppressWarnings("unchecked")
    public List<DailyCandle> dailyCandles(String asset, int limit) {
        String pair = switch (asset.toUpperCase()) {
            case "BTC" -> "XBTEUR";
            case "ETH" -> "ETHEUR";
            case "SOL" -> "SOLEUR";
            default -> throw new IllegalArgumentException("unsupported asset: " + asset);
        };

        Map<String, Object> response = client.get()
                .uri("/0/public/OHLC?pair={pair}&interval=1440", pair)
                .retrieve()
                .body(Map.class);
        if (response == null || !((List<?>) response.get("error")).isEmpty()) {
            throw new IllegalStateException("Kraken OHLC request failed");
        }

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        List<List<Object>> rows = null;
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (!"last".equals(entry.getKey())) {
                rows = (List<List<Object>>) entry.getValue();
                break;
            }
        }
        if (rows == null) throw new IllegalStateException("Kraken OHLC data missing");

        int from = Math.max(0, rows.size() - Math.max(1, limit));
        List<DailyCandle> candles = new ArrayList<>();
        for (List<Object> row : rows.subList(from, rows.size())) {
            long epochSeconds = ((Number) row.get(0)).longValue();
            BigDecimal close = new BigDecimal(row.get(4).toString());
            candles.add(new DailyCandle(Instant.ofEpochSecond(epochSeconds), close));
        }
        return List.copyOf(candles);
    }
}
