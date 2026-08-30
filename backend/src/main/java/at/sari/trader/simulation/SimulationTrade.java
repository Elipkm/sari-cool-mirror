package at.sari.trader.simulation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulationTrade(
        LocalDate date,
        String asset,
        String side,
        BigDecimal notionalEur,
        BigDecimal fillPriceEur,
        BigDecimal feeEur,
        BigDecimal realizedPnlEur,
        String reason
) {}
