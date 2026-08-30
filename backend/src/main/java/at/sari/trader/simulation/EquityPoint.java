package at.sari.trader.simulation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EquityPoint(LocalDate date, BigDecimal equityEur, BigDecimal benchmarkEquityEur) {}
