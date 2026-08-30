package at.sari.trader.market;

import java.math.BigDecimal;
import java.time.Instant;

public record DailyCandle(Instant time, BigDecimal close) {}
