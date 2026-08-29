package at.sari.trader.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "trading.risk")
public record RiskPolicy(
        double riskPerTradePct,
        double maxPositionPct,
        double maxTotalCryptoExposurePct,
        double maxDailyLossPct,
        double maxWeeklyLossPct,
        double cautionDrawdownPct,
        double reduceOnlyDrawdownPct,
        double maxDrawdownPct,
        double maxReferenceToStopPct,
        Set<String> assetWhitelist
) {}
