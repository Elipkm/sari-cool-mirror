package at.sari.trader;

import at.sari.trader.market.MarketPriceProvider;
import at.sari.trader.paper.PaperAccountRepository;
import at.sari.trader.paper.PaperPositionRepository;
import at.sari.trader.paper.PaperTradeRepository;
import at.sari.trader.paper.PaperTradeResult;
import at.sari.trader.risk.PortfolioState;
import at.sari.trader.risk.TradeProposal;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PaperTradingEndToEndTest.PriceConfig.class)
class PaperTradingEndToEndTest {
    @Autowired TestRestTemplate http;
    @Autowired PaperTradeRepository tradeRepository;
    @Autowired PaperAccountRepository accountRepository;
    @Autowired PaperPositionRepository positionRepository;

    @BeforeEach
    void cleanState() {
        tradeRepository.deleteAll();
        positionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void tradeUsesMarketPriceAndPersistsRestartSafePortfolioState() {
        TradeProposal proposal = new TradeProposal(
                "BTC",
                TradeProposal.Side.BUY,
                "trend_pullback",
                new BigDecimal("999.00"), // deliberately wrong: backend must ignore it
                new BigDecimal("95.00"),
                0.80,
                "Simple trend pullback"
        );

        ResponseEntity<PaperTradeResult> execution = http.postForEntity(
                "/api/paper-trades", proposal, PaperTradeResult.class);

        assertThat(execution.getStatusCode().is2xxSuccessful()).isTrue();
        PaperTradeResult trade = execution.getBody();
        assertThat(trade).isNotNull();
        assertThat(trade.status()).isEqualTo("EXECUTED");
        assertThat(trade.referencePrice()).isEqualByComparingTo("100.00");
        assertThat(trade.approvedNotionalEur()).isEqualByComparingTo("500.00");
        assertThat(trade.fillPriceEur()).isEqualByComparingTo("100.10000000");
        assertThat(trade.feeEur()).isEqualByComparingTo("1.25");

        PortfolioState portfolio = http.getForObject("/api/portfolio", PortfolioState.class);
        assertThat(portfolio.cashEur()).isEqualByComparingTo("4498.75");
        assertThat(portfolio.positionNotionalEur()).containsKey("BTC");
        assertThat(portfolio.positionNotionalEur().get("BTC")).isBetween(
                new BigDecimal("499.49"), new BigDecimal("499.51"));
        assertThat(portfolio.equityEur()).isBetween(
                new BigDecimal("4998.24"), new BigDecimal("4998.26"));
        assertThat(tradeRepository.count()).isEqualTo(1);
        assertThat(positionRepository.count()).isEqualTo(1);
        assertThat(accountRepository.count()).isEqualTo(1);
    }

    @TestConfiguration
    static class PriceConfig {
        @Bean
        @Primary
        MarketPriceProvider marketPriceProvider() {
            return asset -> switch (asset.toUpperCase()) {
                case "BTC" -> new BigDecimal("100.00");
                case "ETH" -> new BigDecimal("50.00");
                case "SOL" -> new BigDecimal("20.00");
                default -> throw new IllegalArgumentException("unsupported test asset");
            };
        }
    }
}
