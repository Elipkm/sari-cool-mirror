package at.sari.trader.risk;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEngine {
    private final RiskPolicy policy;

    public RiskEngine(RiskPolicy policy) {
        this.policy = policy;
    }

    public RiskDecision evaluate(TradeProposal proposal) {
        List<String> violations = new ArrayList<>();

        if (!policy.assetWhitelist().contains(proposal.asset().toUpperCase())) {
            violations.add("asset is not whitelisted");
        }
        if (proposal.confidence() < policy.minimumConfidence()) {
            violations.add("confidence is below minimum");
        }
        if (proposal.dailyLossPct() >= policy.maxDailyLossPct()) {
            violations.add("daily loss limit reached");
        }
        if (proposal.drawdownPct() >= policy.maxDrawdownPct()) {
            violations.add("maximum drawdown reached: trading halted");
        }

        BigDecimal maxPositionValue = policy.portfolioValueEur()
                .multiply(BigDecimal.valueOf(policy.maxPositionPct()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);

        if (proposal.side() == TradeProposal.Side.BUY && proposal.amountEur().compareTo(maxPositionValue) > 0) {
            violations.add("order exceeds max position value of EUR " + maxPositionValue);
        }
        if (proposal.side() == TradeProposal.Side.BUY && proposal.currentAssetExposurePct() >= policy.maxPositionPct()) {
            violations.add("asset exposure is already at maximum");
        }

        return violations.isEmpty() ? RiskDecision.allow() : RiskDecision.reject(violations);
    }
}
