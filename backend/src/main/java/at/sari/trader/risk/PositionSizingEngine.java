package at.sari.trader.risk;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PositionSizingEngine {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final RiskPolicy policy;

    public PositionSizingEngine(RiskPolicy policy) {
        this.policy = policy;
    }

    public BigDecimal calculateNotional(TradeProposal proposal, PortfolioState state) {
        BigDecimal stopDistancePct = proposal.referencePrice()
                .subtract(proposal.invalidationPrice())
                .abs()
                .divide(proposal.referencePrice(), 10, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED);

        if (stopDistancePct.signum() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal riskBudget = state.equityEur()
                .multiply(BigDecimal.valueOf(policy.riskPerTradePct()))
                .divide(ONE_HUNDRED, 10, RoundingMode.DOWN);

        BigDecimal byRisk = riskBudget
                .multiply(ONE_HUNDRED)
                .divide(stopDistancePct, 2, RoundingMode.DOWN);

        BigDecimal byPositionCap = state.equityEur()
                .multiply(BigDecimal.valueOf(policy.maxPositionPct()))
                .divide(ONE_HUNDRED, 2, RoundingMode.DOWN);

        return byRisk.min(byPositionCap).max(BigDecimal.ZERO);
    }
}
