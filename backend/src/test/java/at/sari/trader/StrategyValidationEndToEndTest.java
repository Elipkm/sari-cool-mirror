package at.sari.trader;

import at.sari.trader.market.DailyCandle;
import at.sari.trader.market.MarketHistoryProvider;
import at.sari.trader.market.MarketPriceProvider;
import at.sari.trader.paper.PaperTradeRepository;
import at.sari.trader.validation.StrategyValidationReport;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(StrategyValidationEndToEndTest.MarketConfig.class)
class StrategyValidationEndToEndTest {
    @Autowired TestRestTemplate http;
    @Autowired PaperTradeRepository paperTradeRepository;

    @Test
    void frozenNonOverlappingPeriodsProduceAnExplicitVerdictWithoutPaperTrades() {
        paperTradeRepository.deleteAll();
        StrategyValidationReport report = http.postForObject(
                "/api/validation/evaluate", null, StrategyValidationReport.class);

        assertThat(report).isNotNull();
        assertThat(report.frozenStrategy()).isEqualTo("trend-pullback-v1 @ v0.6.0");
        assertThat(report.periods()).extracting("label")
                .containsExactly("Development", "Validation", "Out of sample");
        assertThat(report.periods()).allMatch(period -> period.days() == 180);
        assertThat(report.periods().get(0).endDate().plusDays(1))
                .isEqualTo(report.periods().get(1).startDate());
        assertThat(report.periods().get(1).endDate().plusDays(1))
                .isEqualTo(report.periods().get(2).startDate());
        assertThat(report.criteria()).hasSize(5);
        assertThat(report.verdict()).isEqualTo("REJECT");
        assertThat(report.criteria()).anyMatch(criterion -> !criterion.passed());
        assertThat(paperTradeRepository.count()).isZero();
    }

    @TestConfiguration
    static class MarketConfig {
        @Bean
        @Primary
        MarketHistoryProvider marketHistoryProvider() {
            return (asset, limit) -> history();
        }

        @Bean
        @Primary
        MarketPriceProvider marketPriceProvider() {
            return asset -> new BigDecimal("100.00");
        }

        private static List<DailyCandle> history() {
            List<DailyCandle> candles = new ArrayList<>();
            LocalDate firstDay = LocalDate.now(ZoneOffset.UTC).minusDays(620);
            Instant first = firstDay.atStartOfDay().toInstant(ZoneOffset.UTC);
            for (int day = 0; day < 620; day++) {
                BigDecimal trend = new BigDecimal("100.00").add(new BigDecimal("0.05").multiply(BigDecimal.valueOf(day)));
                int cycle = day % 30;
                BigDecimal close = cycle == 20 ? trend.multiply(new BigDecimal("0.97"))
                        : cycle == 21 ? trend.multiply(new BigDecimal("1.03"))
                        : cycle == 22 ? trend.multiply(new BigDecimal("0.94"))
                        : trend;
                candles.add(new DailyCandle(first.plusSeconds(86400L * day), close.setScale(2, RoundingMode.HALF_UP)));
            }
            return List.copyOf(candles);
        }
    }
}
