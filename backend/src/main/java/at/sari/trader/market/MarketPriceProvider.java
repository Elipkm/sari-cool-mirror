package at.sari.trader.market;

import java.math.BigDecimal;

public interface MarketPriceProvider {
    BigDecimal priceEur(String asset);
}
