package at.sari.trader.paper;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class PaperPosition {
    @Id
    private String asset;
    private BigDecimal quantity;

    protected PaperPosition() {}

    public static PaperPosition of(String asset) {
        PaperPosition position = new PaperPosition();
        position.asset = asset.toUpperCase();
        position.quantity = BigDecimal.ZERO;
        return position;
    }

    public void add(BigDecimal quantity) { this.quantity = this.quantity.add(quantity); }
    public void clear() { this.quantity = BigDecimal.ZERO; }
    public boolean isOpen() { return quantity != null && quantity.signum() > 0; }
    public String getAsset() { return asset; }
    public BigDecimal getQuantity() { return quantity; }
}
