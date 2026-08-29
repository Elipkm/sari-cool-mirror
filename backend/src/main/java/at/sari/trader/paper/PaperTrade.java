package at.sari.trader.paper;

import at.sari.trader.risk.TradeProposal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "paper_trade")
public class PaperTrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false, length = 16)
    private String asset;
    @Column(nullable = false, length = 8)
    private String side;
    @Column(nullable = false, length = 100)
    private String strategy;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal referencePrice;
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal invalidationPrice;
    @Column(nullable = false, precision = 24, scale = 2)
    private BigDecimal approvedNotionalEur;
    @Column(precision = 24, scale = 8)
    private BigDecimal fillPriceEur;
    @Column(nullable = false, precision = 24, scale = 2)
    private BigDecimal feeEur;
    @Column(nullable = false)
    private double signalStrength;
    @Column(nullable = false, length = 2000)
    private String thesis;
    @Column(nullable = false, length = 2000)
    private String decisionReasons;

    protected PaperTrade() {}

    public static PaperTrade executed(
            TradeProposal proposal,
            BigDecimal approvedNotionalEur,
            BigDecimal fillPriceEur,
            BigDecimal feeEur,
            List<String> reasons
    ) {
        return create(proposal, "EXECUTED", approvedNotionalEur, fillPriceEur, feeEur, reasons);
    }

    public static PaperTrade rejected(TradeProposal proposal, List<String> reasons) {
        return create(proposal, "REJECTED", BigDecimal.ZERO, null, BigDecimal.ZERO, reasons);
    }

    private static PaperTrade create(
            TradeProposal proposal,
            String status,
            BigDecimal approvedNotionalEur,
            BigDecimal fillPriceEur,
            BigDecimal feeEur,
            List<String> reasons
    ) {
        PaperTrade trade = new PaperTrade();
        trade.createdAt = Instant.now();
        trade.asset = proposal.asset().toUpperCase();
        trade.side = proposal.side().name();
        trade.strategy = proposal.strategy();
        trade.status = status;
        trade.referencePrice = proposal.referencePrice();
        trade.invalidationPrice = proposal.invalidationPrice();
        trade.approvedNotionalEur = approvedNotionalEur;
        trade.fillPriceEur = fillPriceEur;
        trade.feeEur = feeEur;
        trade.signalStrength = proposal.signalStrength();
        trade.thesis = proposal.thesis();
        trade.decisionReasons = String.join("; ", reasons);
        return trade;
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getAsset() { return asset; }
    public String getSide() { return side; }
    public String getStrategy() { return strategy; }
    public String getStatus() { return status; }
    public BigDecimal getReferencePrice() { return referencePrice; }
    public BigDecimal getInvalidationPrice() { return invalidationPrice; }
    public BigDecimal getApprovedNotionalEur() { return approvedNotionalEur; }
    public BigDecimal getFillPriceEur() { return fillPriceEur; }
    public BigDecimal getFeeEur() { return feeEur; }
    public double getSignalStrength() { return signalStrength; }
    public String getThesis() { return thesis; }
    public String getDecisionReasons() { return decisionReasons; }
}
