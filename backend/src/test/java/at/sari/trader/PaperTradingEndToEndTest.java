package at.sari.trader;

import at.sari.trader.paper.PaperTradeRepository;
import at.sari.trader.paper.PaperTradeResult;
import at.sari.trader.risk.TradeProposal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaperTradingEndToEndTest {
    @Autowired
    private TestRestTemplate http;

    @Autowired
    private PaperTradeRepository repository;

    @BeforeEach
    void cleanLedger() {
        repository.deleteAll();
    }

    @Test
    void validTradeFlowsThroughRiskPaperExecutionAndPersistentLedger() {
        TradeProposal proposal = new TradeProposal(
                "BTC",
                TradeProposal.Side.BUY,
                "trend_pullback",
                new BigDecimal("100.00"),
                new BigDecimal("95.00"),
                0.80,
                "Simple trend pullback with a clear 5% invalidation level"
        );

        ResponseEntity<PaperTradeResult> executionResponse = http.postForEntity(
                "/api/paper-trades",
                proposal,
                PaperTradeResult.class
        );

        assertThat(executionResponse.getStatusCode().is2xxSuccessful()).isTrue();
        PaperTradeResult executed = executionResponse.getBody();
        assertThat(executed).isNotNull();
        assertThat(executed.status()).isEqualTo("EXECUTED");
        assertThat(executed.asset()).isEqualTo("BTC");
        assertThat(executed.approvedNotionalEur()).isEqualByComparingTo("500.00");
        assertThat(executed.fillPriceEur()).isEqualByComparingTo("100.10000000");
        assertThat(executed.feeEur()).isEqualByComparingTo("1.25");

        ResponseEntity<PaperTradeResult[]> ledgerResponse = http.getForEntity(
                "/api/paper-trades",
                PaperTradeResult[].class
        );

        assertThat(ledgerResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(ledgerResponse.getBody()).hasSize(1);
        assertThat(ledgerResponse.getBody()[0].id()).isEqualTo(executed.id());
        assertThat(repository.count()).isEqualTo(1);
    }
}
