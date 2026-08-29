package at.sari.trader.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Set;

@ConfigurationProperties(prefix = "trading.risk")
public record RiskPolicy(
        BigDecimal portfolioValueEur,
        double maxPositionPct,
        double maxDailyLossPct,
        double maxDrawdownPct,
        double minimumConfidence,
        Set<String> assetWhitelist
) {}
