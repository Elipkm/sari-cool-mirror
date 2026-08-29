package at.sari.trader.paper;

import at.sari.trader.risk.RiskDecision;
import at.sari.trader.risk.RiskEngine;
import at.sari.trader.risk.TradeProposal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PaperTradingService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final RiskEngine riskEngine;
    private final PaperTradeRepository repository;
    private final BigDecimal feePct;
    private final BigDecimal slippagePct;

    public PaperTradingService(
            RiskEngine riskEngine,
            PaperTradeRepository repository,
            @Value("${trading.paper.fee-pct:0.25}") BigDecimal feePct,
            @Value("${trading.paper.slippage-pct:0.10}") BigDecimal slippagePct
    ) {
        this.riskEngine = riskEngine;
        this.repository = repository;
        this.feePct = feePct;
        this.slippagePct = slippagePct;
    }

    @Transactional
    public PaperTradeResult execute(TradeProposal proposal) {
        RiskDecision decision = riskEngine.evaluate(proposal);

        PaperTrade trade;
        if (!decision.allowed()) {
            trade = PaperTrade.rejected(proposal, decision.reasons());
        } else {
            BigDecimal fillPrice = applySlippage(proposal);
            BigDecimal fee = decision.approvedNotionalEur()
                    .multiply(feePct)
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            trade = PaperTrade.executed(
                    proposal,
                    decision.approvedNotionalEur(),
                    fillPrice,
                    fee,
                    decision.reasons()
            );
        }

        return PaperTradeResult.from(repository.save(trade));
    }

    @Transactional(readOnly = true)
    public List<PaperTradeResult> ledger() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(PaperTradeResult::from)
                .toList();
    }

    private BigDecimal applySlippage(TradeProposal proposal) {
        BigDecimal factor = slippagePct.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
        return proposal.side() == TradeProposal.Side.BUY
                ? proposal.referencePrice().multiply(BigDecimal.ONE.add(factor)).setScale(8, RoundingMode.HALF_UP)
                : proposal.referencePrice().multiply(BigDecimal.ONE.subtract(factor)).setScale(8, RoundingMode.HALF_UP);
    }
}
