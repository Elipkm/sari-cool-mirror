package at.sari.trader;

import at.sari.trader.market.DailyCandle;
import at.sari.trader.market.MarketHistoryProvider;
import at.sari.trader.market.MarketPriceProvider;
import at.sari.trader.paper.PaperTradeRepository;
import at.sari.trader.simulation.SimulationResult;
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
@Import(HistoricalSimulationEndToEndTest.HistoryConfig.class)
class HistoricalSimulationEndToEndTest {
    @Autowired TestRestTemplate http;
    @Autowired PaperTradeRepository paperTradeRepository;

    @Test
    void eachRequestAdvancesExactlyOneDay() {
        paperTradeRepository.deleteAll();
        SimulationResult first = step("2026-02-28");
        assertThat(first.iteration()).isEqualTo(1);
        assertThat(first.currentDate()).isEqualTo(java.time.LocalDate.parse("2026-02-28"));
        assertThat(first.decisions()).hasSize(3).allMatch(d -> d.action().equals("HOLD"));

        SimulationResult second = step("2026-02-28");
        assertThat(second.iteration()).isEqualTo(2);
        assertThat(second.currentDate()).isEqualTo(java.time.LocalDate.parse("2026-03-01"));
        assertThat(second.decisions()).hasSize(3).allMatch(d -> d.action().equals("BUY"));
        assertThat(second.positionsEur()).containsOnlyKeys("BTC", "ETH", "SOL");
        assertThat(second.equityEur()).isLessThan(second.startingCapitalEur());
        assertThat(paperTradeRepository.count()).isZero();

        SimulationResult complete = http.postForObject(
                "/api/simulation/run?startDate=2026-02-28", null, SimulationResult.class);
        assertThat(complete).isNotNull();
        assertThat(complete.iteration()).isEqualTo(7);
        assertThat(complete.hasNextDay()).isFalse();
        assertThat(complete.equityCurve()).hasSize(7);
        assertThat(complete.trades()).hasSize(6);
        assertThat(complete.completedTrades()).isEqualTo(3);
        assertThat(complete.totalFeesEur()).isPositive();
        assertThat(paperTradeRepository.count()).isZero();
    }

    private SimulationResult step(String startDate) {
        return http.postForObject("/api/simulation/step?startDate=" + startDate, null, SimulationResult.class);
    }

    @TestConfiguration
    static class HistoryConfig {
        @Bean
        @Primary
        MarketHistoryProvider historyProvider() {
            return (asset, limit) -> history();
        }

        @Bean
        @Primary
        MarketPriceProvider marketPriceProvider() {
            return asset -> new BigDecimal("100.00");
        }

        private static List<DailyCandle> history() {
            List<DailyCandle> candles = new ArrayList<>();
            Instant start = Instant.parse("2026-01-01T00:00:00Z");
            for (int i = 0; i < 58; i++) candles.add(candle(start, i, "100.00"));
            candles.add(candle(start, 58, "98.00"));
            candles.add(candle(start, 59, "105.00"));
            candles.add(candle(start, 60, "106.00"));
            candles.add(candle(start, 61, "90.00"));
            for (int i = 62; i < 65; i++) candles.add(candle(start, i, "91.00"));
            return List.copyOf(candles);
        }

        private static DailyCandle candle(Instant start, int day, String close) {
            return new DailyCandle(start.plusSeconds(86400L * day), new BigDecimal(close));
        }
    }
}
