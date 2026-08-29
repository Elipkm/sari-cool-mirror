package at.sari.trader.market;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
public class KrakenMarketPriceProvider implements MarketPriceProvider {
    private final RestClient client = RestClient.create("https://api.kraken.com");

    @Override
    @SuppressWarnings("unchecked")
    public BigDecimal priceEur(String asset) {
        String pair = switch (asset.toUpperCase()) {
            case "BTC" -> "XBTEUR";
            case "ETH" -> "ETHEUR";
            case "SOL" -> "SOLEUR";
            default -> throw new IllegalArgumentException("unsupported asset: " + asset);
        };

        Map<String, Object> response = client.get()
                .uri("/0/public/Ticker?pair={pair}", pair)
                .retrieve()
                .body(Map.class);
        if (response == null || !((List<?>) response.get("error")).isEmpty()) {
            throw new IllegalStateException("Kraken ticker request failed");
        }
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        Map<String, Object> ticker = (Map<String, Object>) result.values().iterator().next();
        List<String> lastTrade = (List<String>) ticker.get("c");
        return new BigDecimal(lastTrade.get(0));
    }
}
