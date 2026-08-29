package at.sari.trader.risk;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Untrusted strategy/AI output. It deliberately contains no portfolio state,
 * risk budget or final order size. Those values are owned by trusted services.
 */
public record TradeProposal(
        @NotBlank String asset,
        @NotNull Side side,
        @NotBlank String strategy,
        @NotNull @Positive BigDecimal referencePrice,
        @NotNull @Positive BigDecimal invalidationPrice,
        @DecimalMin("0.0") @DecimalMax("1.0") double signalStrength,
        @NotBlank String thesis
) {
    public enum Side { BUY, SELL }
}
