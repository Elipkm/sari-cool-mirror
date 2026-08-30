package at.sari.trader;

import at.sari.trader.market.MarketHistoryProvider;
import at.sari.trader.market.MarketPriceProvider;
import at.sari.trader.review.WeeklyReview;
import at.sari.trader.review.WeeklyReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:weekly-review;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true"
})
class WeeklyReviewEndToEndTest {
    @Autowired WeeklyReviewService weeklyReviewService;

    @Test
    void freshPaperAccountProducesSimpleHealthyWeeklyReview() {
        WeeklyReview review = weeklyReviewService.current();
        assertThat(review.equityEur()).isEqualByComparingTo("5000.00");
        assertThat(review.cashEur()).isEqualByComparingTo("5000.00");
        assertThat(review.returnPct()).isEqualByComparingTo("0.00");
        assertThat(review.weeklyReturnPct()).isEqualByComparingTo("0.00");
        assertThat(review.maxDrawdownPct()).isEqualByComparingTo("0.00");
        assertThat(review.positionsEur()).isEmpty();
        assertThat(review.recentTrades()).isEmpty();
    }

    @TestConfiguration
    static class MarketStubs {
        @Bean @Primary MarketPriceProvider priceProvider() { return asset -> new BigDecimal("100.00"); }
        @Bean @Primary MarketHistoryProvider historyProvider() { return asset -> List.of(); }
    }
}
