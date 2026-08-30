package at.sari.trader.risk;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEngine {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final RiskPolicy policy;
    private final PortfolioStateProvider portfolioStateProvider;
    private final PositionSizingEngine positionSizingEngine;

    public RiskEngine(
            RiskPolicy policy,
            PortfolioStateProvider portfolioStateProvider,
            PositionSizingEngine positionSizingEngine
    ) {
        this.policy = policy;
        this.portfolioStateProvider = portfolioStateProvider;
        this.positionSizingEngine = positionSizingEngine;
    }

    public RiskDecision evaluate(TradeProposal proposal) {
        return evaluate(proposal, portfolioStateProvider.currentState());
    }

    /** Evaluate a proposal against an explicit trusted state, used by isolated historical validation. */
    public RiskDecision evaluate(TradeProposal proposal, PortfolioState state) {
        List<String> violations = new ArrayList<>();

        if (!policy.assetWhitelist().contains(proposal.asset().toUpperCase())) {
            violations.add("asset is not whitelisted");
        }

        validateStopDirection(proposal, violations);

        BigDecimal stopDistancePct = percentDistance(proposal.referencePrice(), proposal.invalidationPrice());
        if (stopDistancePct.compareTo(BigDecimal.valueOf(policy.maxReferenceToStopPct())) > 0) {
            violations.add("invalidation distance exceeds configured maximum");
        }

        double dailyLossPct = lossPct(state.startOfDayEquityEur(), state.equityEur());
        double weeklyLossPct = lossPct(state.startOfWeekEquityEur(), state.equityEur());
        double drawdownPct = lossPct(state.highWaterMarkEquityEur(), state.equityEur());

        CircuitBreakerState effectiveState = effectiveCircuitBreakerState(state.circuitBreakerState(), drawdownPct);

        if (effectiveState == CircuitBreakerState.HALTED) {
            violations.add("circuit breaker is HALTED");
        }
        if (proposal.side() == TradeProposal.Side.BUY && effectiveState == CircuitBreakerState.REDUCE_ONLY) {
            violations.add("circuit breaker is REDUCE_ONLY: new risk is forbidden");
        }
        if (dailyLossPct >= policy.maxDailyLossPct()) {
            violations.add("daily loss limit reached");
        }
        if (weeklyLossPct >= policy.maxWeeklyLossPct()) {
            violations.add("weekly loss limit reached");
        }
        if (drawdownPct >= policy.maxDrawdownPct()) {
            violations.add("maximum drawdown reached");
        }

        BigDecimal proposedNotional = positionSizingEngine.calculateNotional(proposal, state);
        if (proposedNotional.signum() <= 0) {
            violations.add("position sizing produced zero notional");
        }

        if (proposal.side() == TradeProposal.Side.BUY) {
            BigDecimal maxPosition = percentOf(state.equityEur(), policy.maxPositionPct());
            BigDecimal resultingAssetExposure = state.exposureFor(proposal.asset()).add(proposedNotional);
            if (resultingAssetExposure.compareTo(maxPosition) > 0) {
                violations.add("resulting asset exposure exceeds maximum position limit");
            }

            BigDecimal maxTotalExposure = percentOf(state.equityEur(), policy.maxTotalCryptoExposurePct());
            if (state.totalCryptoExposureEur().add(proposedNotional).compareTo(maxTotalExposure) > 0) {
                violations.add("resulting total crypto exposure exceeds portfolio limit");
            }

            if (proposedNotional.compareTo(state.cashEur()) > 0) {
                violations.add("insufficient available cash");
            }
        }

        return violations.isEmpty()
                ? RiskDecision.allow(proposedNotional, effectiveState)
                : RiskDecision.reject(effectiveState, violations);
    }

    private void validateStopDirection(TradeProposal proposal, List<String> violations) {
        int comparison = proposal.invalidationPrice().compareTo(proposal.referencePrice());
        if (proposal.side() == TradeProposal.Side.BUY && comparison >= 0) {
            violations.add("BUY invalidation price must be below reference price");
        }
        if (proposal.side() == TradeProposal.Side.SELL && comparison <= 0) {
            violations.add("SELL invalidation price must be above reference price");
        }
    }

    private CircuitBreakerState effectiveCircuitBreakerState(CircuitBreakerState current, double drawdownPct) {
        if (current == CircuitBreakerState.HALTED || drawdownPct >= policy.maxDrawdownPct()) {
            return CircuitBreakerState.HALTED;
        }
        if (current == CircuitBreakerState.REDUCE_ONLY || drawdownPct >= policy.reduceOnlyDrawdownPct()) {
            return CircuitBreakerState.REDUCE_ONLY;
        }
        if (current == CircuitBreakerState.CAUTION || drawdownPct >= policy.cautionDrawdownPct()) {
            return CircuitBreakerState.CAUTION;
        }
        return CircuitBreakerState.NORMAL;
    }

    private BigDecimal percentOf(BigDecimal value, double pct) {
        return value.multiply(BigDecimal.valueOf(pct)).divide(ONE_HUNDRED, 2, RoundingMode.DOWN);
    }

    private BigDecimal percentDistance(BigDecimal reference, BigDecimal other) {
        return reference.subtract(other).abs()
                .divide(reference, 10, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED);
    }

    private double lossPct(BigDecimal baseline, BigDecimal current) {
        if (baseline == null || baseline.signum() <= 0 || current.compareTo(baseline) >= 0) {
            return 0.0;
        }
        return baseline.subtract(current)
                .divide(baseline, 10, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .doubleValue();
    }
}
