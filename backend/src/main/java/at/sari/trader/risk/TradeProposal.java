package at.sari.trader.risk;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TradeProposal(
        @NotBlank String asset,
        @NotNull Side side,
        @NotNull @Positive BigDecimal amountEur,
        @DecimalMin("0.0") @DecimalMax("1.0") double confidence,
        @DecimalMin("0.0") double currentAssetExposurePct,
        double dailyLossPct,
        @DecimalMin("0.0") double drawdownPct
) {
    public enum Side { BUY, SELL }
}
