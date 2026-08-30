package at.sari.trader.paper;

import at.sari.trader.market.MarketPriceProvider;
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
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("5000.00");

    private final RiskEngine riskEngine;
    private final PaperTradeRepository tradeRepository;
    private final PaperAccountRepository accountRepository;
    private final PaperPositionRepository positionRepository;
    private final MarketPriceProvider marketPriceProvider;
    private final BigDecimal feePct;
    private final BigDecimal slippagePct;

    public PaperTradingService(
            RiskEngine riskEngine,
            PaperTradeRepository tradeRepository,
            PaperAccountRepository accountRepository,
            PaperPositionRepository positionRepository,
            MarketPriceProvider marketPriceProvider,
            @Value("${trading.paper.fee-pct:0.25}") BigDecimal feePct,
            @Value("${trading.paper.slippage-pct:0.10}") BigDecimal slippagePct
    ) {
        this.riskEngine = riskEngine;
        this.tradeRepository = tradeRepository;
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.marketPriceProvider = marketPriceProvider;
        this.feePct = feePct;
        this.slippagePct = slippagePct;
    }

    @Transactional
    public PaperTradeResult execute(TradeProposal requestedProposal) {
        BigDecimal marketPrice = marketPriceProvider.priceEur(requestedProposal.asset());
        TradeProposal proposal = new TradeProposal(
                requestedProposal.asset(), requestedProposal.side(), requestedProposal.strategy(), marketPrice,
                requestedProposal.invalidationPrice(), requestedProposal.signalStrength(), requestedProposal.thesis());

        if (proposal.side() == TradeProposal.Side.SELL) {
            return exitPosition(proposal.asset(), proposal.strategy(), proposal.thesis());
        }

        RiskDecision decision = riskEngine.evaluate(proposal);
        PaperTrade trade;
        if (!decision.allowed()) {
            trade = PaperTrade.rejected(proposal, decision.reasons());
        } else {
            BigDecimal fillPrice = buyFillPrice(proposal.referencePrice());
            BigDecimal fee = fee(decision.approvedNotionalEur());
            BigDecimal totalCost = decision.approvedNotionalEur().add(fee);
            PaperAccount account = account();

            if (totalCost.compareTo(account.getCashEur()) > 0) {
                trade = PaperTrade.rejected(proposal, List.of("insufficient cash after fees"));
            } else {
                BigDecimal quantity = decision.approvedNotionalEur().divide(fillPrice, 12, RoundingMode.DOWN);
                PaperPosition position = positionRepository.findById(proposal.asset().toUpperCase())
                        .orElseGet(() -> PaperPosition.of(proposal.asset()));
                position.add(quantity);
                account.debit(totalCost);
                positionRepository.save(position);
                accountRepository.save(account);
                trade = PaperTrade.executed(proposal, decision.approvedNotionalEur(), fillPrice, fee, decision.reasons());
            }
        }
        return PaperTradeResult.from(tradeRepository.save(trade));
    }

    @Transactional
    public PaperTradeResult exitPosition(String asset, String strategy, String reason) {
        String symbol = asset.toUpperCase();
        BigDecimal marketPrice = marketPriceProvider.priceEur(symbol);
        TradeProposal proposal = new TradeProposal(symbol, TradeProposal.Side.SELL, strategy, marketPrice,
                marketPrice.multiply(new BigDecimal("1.01")), 1.0, reason);
        PaperPosition position = positionRepository.findById(symbol).orElse(null);
        if (position == null || !position.isOpen()) {
            return PaperTradeResult.from(tradeRepository.save(PaperTrade.rejected(proposal, List.of("no open position"))));
        }

        BigDecimal fillPrice = sellFillPrice(marketPrice);
        BigDecimal notional = position.getQuantity().multiply(fillPrice).setScale(2, RoundingMode.DOWN);
        BigDecimal fee = fee(notional);
        PaperAccount account = account();
        account.credit(notional.subtract(fee));
        position.clear();
        accountRepository.save(account);
        positionRepository.save(position);
        PaperTrade trade = PaperTrade.executed(proposal, notional, fillPrice, fee, List.of("position fully closed"));
        return PaperTradeResult.from(tradeRepository.save(trade));
    }

    @Transactional(readOnly = true)
    public List<PaperTradeResult> ledger() {
        return tradeRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(PaperTradeResult::from).toList();
    }

    private PaperAccount account() {
        return accountRepository.findById(1L)
                .orElseGet(() -> accountRepository.save(PaperAccount.initial(INITIAL_CAPITAL)));
    }

    private BigDecimal fee(BigDecimal notional) {
        return notional.multiply(feePct).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal buyFillPrice(BigDecimal price) {
        BigDecimal factor = slippagePct.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
        return price.multiply(BigDecimal.ONE.add(factor)).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal sellFillPrice(BigDecimal price) {
        BigDecimal factor = slippagePct.divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
        return price.multiply(BigDecimal.ONE.subtract(factor)).setScale(8, RoundingMode.HALF_UP);
    }
}
