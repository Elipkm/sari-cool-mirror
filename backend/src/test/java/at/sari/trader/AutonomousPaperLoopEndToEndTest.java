package at.sari.trader;

import at.sari.trader.automation.AutomationRunRepository;
import at.sari.trader.automation.AutomationRunResult;
import at.sari.trader.market.DailyCandle;
import at.sari.trader.market.MarketHistoryProvider;
import at.sari.trader.market.MarketPriceProvider;
import at.sari.trader.paper.PaperAccountRepository;
import at.sari.trader.paper.PaperPositionRepository;
import at.sari.trader.paper.PaperTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AutonomousPaperLoopEndToEndTest.MarketConfig.class)
class AutonomousPaperLoopEndToEndTest {
    @Autowired TestRestTemplate http;
    @Autowired AutomationRunRepository automationRunRepository;
    @Autowired PaperTradeRepository tradeRepository;
    @Autowired PaperPositionRepository positionRepository;
    @Autowired PaperAccountRepository accountRepository;

    @BeforeEach
    void clean() {
        automationRunRepository.deleteAll();
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void oneAutomationRunScansAllAssetsAndPersistsItsProof() {
        AutomationRunResult result = http.postForObject("/api/automation/run", null, AutomationRunResult.class);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.assets()).hasSize(3);
        assertThat(result.assets()).extracting(r -> r.asset()).containsExactly("BTC", "ETH", "SOL");
        assertThat(result.assets()).allMatch(r -> r.action().name().equals("BUY"));
        assertThat(tradeRepository.count()).isEqualTo(3);
        assertThat(positionRepository.count()).isEqualTo(3);
        assertThat(automationRunRepository.count()).isEqualTo(1);
        assertThat(http.getForObject("/api/automation/last", String.class)).contains("SUCCESS", "Scanned 3 assets");
    }

    @TestConfiguration
    static class MarketConfig {
        @Bean @Primary MarketPriceProvider priceProvider() { return asset -> new BigDecimal("105.00"); }
        @Bean @Primary MarketHistoryProvider historyProvider() { return (asset, limit) -> entryHistory(); }

        private static List<DailyCandle> entryHistory() {
            List<DailyCandle> candles = new ArrayList<>();
            Instant start = Instant.parse("2026-01-01T00:00:00Z");
            for (int i = 0; i < 58; i++) candles.add(new DailyCandle(start.plusSeconds(86400L * i), new BigDecimal("100.00")));
            candles.add(new DailyCandle(start.plusSeconds(86400L * 58), new BigDecimal("98.00")));
            candles.add(new DailyCandle(start.plusSeconds(86400L * 59), new BigDecimal("105.00")));
            return List.copyOf(candles);
        }
    }
}
