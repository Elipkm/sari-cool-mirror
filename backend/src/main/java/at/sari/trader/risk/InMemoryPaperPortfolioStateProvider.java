package at.sari.trader.risk;

import at.sari.trader.market.MarketPriceProvider;
import at.sari.trader.paper.PaperAccount;
import at.sari.trader.paper.PaperAccountRepository;
import at.sari.trader.paper.PaperPositionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persistent paper-account state. The historical class name is kept to avoid
 * needless project churn; it is no longer in-memory.
 */
@Component
@Profile("!live")
public class InMemoryPaperPortfolioStateProvider implements PortfolioStateProvider {
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("5000.00");

    private final PaperAccountRepository accountRepository;
    private final PaperPositionRepository positionRepository;
    private final MarketPriceProvider marketPriceProvider;

    public InMemoryPaperPortfolioStateProvider(
            PaperAccountRepository accountRepository,
            PaperPositionRepository positionRepository,
            MarketPriceProvider marketPriceProvider
    ) {
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.marketPriceProvider = marketPriceProvider;
    }

    @Override
    @Transactional
    public PortfolioState currentState() {
        PaperAccount account = accountRepository.findById(1L)
                .orElseGet(() -> accountRepository.save(PaperAccount.initial(INITIAL_CAPITAL)));

        Map<String, BigDecimal> positions = positionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getAsset().toUpperCase(),
                        p -> p.getQuantity().multiply(marketPriceProvider.priceEur(p.getAsset()))
                ));

        BigDecimal equity = positions.values().stream()
                .reduce(account.getCashEur(), BigDecimal::add);
        account.updateHighWaterMark(equity);

        return new PortfolioState(
                equity,
                account.getStartOfDayEquityEur(),
                account.getStartOfWeekEquityEur(),
                account.getHighWaterMarkEquityEur(),
                account.getCashEur(),
                positions,
                Map.of(),
                CircuitBreakerState.NORMAL
        );
    }
}
