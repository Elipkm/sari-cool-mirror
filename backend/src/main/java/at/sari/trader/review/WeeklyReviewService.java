package at.sari.trader.review;

import at.sari.trader.paper.PaperTradeRepository;
import at.sari.trader.risk.PortfolioState;
import at.sari.trader.risk.PortfolioStateProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class WeeklyReviewService {
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("5000.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PortfolioStateProvider portfolioStateProvider;
    private final PaperTradeRepository tradeRepository;

    public WeeklyReviewService(PortfolioStateProvider portfolioStateProvider, PaperTradeRepository tradeRepository) {
        this.portfolioStateProvider = portfolioStateProvider;
        this.tradeRepository = tradeRepository;
    }

    public WeeklyReview current() {
        PortfolioState state = portfolioStateProvider.currentState();
        return new WeeklyReview(
                state.equityEur(),
                state.cashEur(),
                percentChange(state.equityEur(), INITIAL_CAPITAL),
                percentChange(state.equityEur(), state.startOfWeekEquityEur()),
                drawdown(state.equityEur(), state.highWaterMarkEquityEur()),
                state.circuitBreakerState(),
                state.positionNotionalEur(),
                tradeRepository.findTop10ByOrderByCreatedAtDesc()
        );
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal baseline) {
        if (baseline == null || baseline.signum() == 0) return BigDecimal.ZERO;
        return current.subtract(baseline).multiply(HUNDRED).divide(baseline, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal drawdown(BigDecimal current, BigDecimal highWaterMark) {
        if (highWaterMark == null || highWaterMark.signum() == 0 || current.compareTo(highWaterMark) >= 0) return BigDecimal.ZERO;
        return highWaterMark.subtract(current).multiply(HUNDRED).divide(highWaterMark, 2, RoundingMode.HALF_UP);
    }
}
