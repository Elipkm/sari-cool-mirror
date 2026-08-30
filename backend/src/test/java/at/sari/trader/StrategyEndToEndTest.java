package at.sari.trader;

import at.sari.trader.market.DailyCandle;
import at.sari.trader.market.MarketHistoryProvider;
import at.sari.trader.market.MarketPriceProvider;
import at.sari.trader.paper.PaperAccountRepository;
import at.sari.trader.paper.PaperPositionRepository;
import at.sari.trader.paper.PaperTradeRepository;
import at.sari.trader.strategy.BacktestResult;
import at.sari.trader.strategy.StrategyRunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(StrategyEndToEndTest.MarketConfig.class)
class StrategyEndToEndTest {
    @Autowired TestRestTemplate http;
    @Autowired PaperTradeRepository tradeRepository;
    @Autowired PaperAccountRepository accountRepository;
    @Autowired PaperPositionRepository positionRepository;

    @BeforeEach
    void clean() {
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void marketHistoryCreatesSignalThatExecutesThroughPaperTrading() {
        ResponseEntity<StrategyRunResult> response = http.postForEntity(
                "/api/strategy/run/BTC", null, StrategyRunResult.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        StrategyRunResult result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.action().name()).isEqualTo("BUY");
        assertThat(result.trade()).isNotNull();
        assertThat(result.trade().status()).isEqualTo("EXECUTED");
        assertThat(result.trade().asset()).isEqualTo("BTC");
        assertThat(positionRepository.findById("BTC")).isPresent();
        assertThat(positionRepository.findById("BTC").orElseThrow().isOpen()).isTrue();
        assertThat(tradeRepository.count()).isEqualTo(1);
    }

    @Test
    void backtestUsesSameStrategyAndReturnsBenchmarkAndCostsAwareResult() {
        BacktestResult result = http.getForObject("/api/strategy/backtest/BTC", BacktestResult.class);
        assertThat(result).isNotNull();
        assertThat(result.asset()).isEqualTo("BTC");
        assertThat(result.candles()).isEqualTo(60);
        assertThat(result.startingCapitalEur()).isEqualByComparingTo("5000.00");
        assertThat(result.strategyReturnPct()).isNotNull();
        assertThat(result.buyAndHoldReturnPct()).isNotNull();
        assertThat(result.maxDrawdownPct()).isNotNull();
    }

    @TestConfiguration
    static class MarketConfig {
        @Bean
        @Primary
        MarketPriceProvider marketPriceProvider() {
            return asset -> new BigDecimal("105.00");
        }

        @Bean
        @Primary
        MarketHistoryProvider marketHistoryProvider() {
            return (asset, limit) -> entryHistory();
        }

        private static List<DailyCandle> entryHistory() {
            List<DailyCandle> candles = new ArrayList<>();
            Instant start = Instant.parse("2026-01-01T00:00:00Z");
            for (int i = 0; i < 58; i++) {
                candles.add(new DailyCandle(start.plusSeconds(86400L * i), new BigDecimal("100.00")));
            }
            candles.add(new DailyCandle(start.plusSeconds(86400L * 58), new BigDecimal("98.00")));
            candles.add(new DailyCandle(start.plusSeconds(86400L * 59), new BigDecimal("105.00")));
            return List.copyOf(candles);
        }
    }
}
