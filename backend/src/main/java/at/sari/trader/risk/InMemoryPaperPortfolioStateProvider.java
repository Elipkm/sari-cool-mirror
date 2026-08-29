package at.sari.trader.risk;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Profile("!live")
public class InMemoryPaperPortfolioStateProvider implements PortfolioStateProvider {
    @Override
    public PortfolioState currentState() {
        // Bootstrap state only. This will be replaced by persisted paper-account state next.
        BigDecimal equity = new BigDecimal("5000.00");
        return new PortfolioState(
                equity,
                equity,
                equity,
                equity,
                Map.of(),
                Map.of(),
                CircuitBreakerState.NORMAL
        );
    }
}
