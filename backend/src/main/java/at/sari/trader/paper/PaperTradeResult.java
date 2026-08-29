package at.sari.trader.paper;

import java.math.BigDecimal;
import java.time.Instant;

public record PaperTradeResult(
        Long id,
        Instant createdAt,
        String status,
        String asset,
        String side,
        String strategy,
        BigDecimal referencePrice,
        BigDecimal approvedNotionalEur,
        BigDecimal fillPriceEur,
        BigDecimal feeEur,
        String decisionReasons
) {
    public static PaperTradeResult from(PaperTrade trade) {
        return new PaperTradeResult(
                trade.getId(),
                trade.getCreatedAt(),
                trade.getStatus(),
                trade.getAsset(),
                trade.getSide(),
                trade.getStrategy(),
                trade.getReferencePrice(),
                trade.getApprovedNotionalEur(),
                trade.getFillPriceEur(),
                trade.getFeeEur(),
                trade.getDecisionReasons()
        );
    }
}
