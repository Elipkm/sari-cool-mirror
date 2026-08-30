package at.sari.trader.paper;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class PaperAccount {
    @Id
    private Long id;
    private BigDecimal cashEur;
    private BigDecimal startOfDayEquityEur;
    private BigDecimal startOfWeekEquityEur;
    private BigDecimal highWaterMarkEquityEur;

    protected PaperAccount() {}

    public static PaperAccount initial(BigDecimal capital) {
        PaperAccount account = new PaperAccount();
        account.id = 1L;
        account.cashEur = capital;
        account.startOfDayEquityEur = capital;
        account.startOfWeekEquityEur = capital;
        account.highWaterMarkEquityEur = capital;
        return account;
    }

    public void debit(BigDecimal amount) { cashEur = cashEur.subtract(amount); }
    public void credit(BigDecimal amount) { cashEur = cashEur.add(amount); }
    public BigDecimal getCashEur() { return cashEur; }
    public BigDecimal getStartOfDayEquityEur() { return startOfDayEquityEur; }
    public BigDecimal getStartOfWeekEquityEur() { return startOfWeekEquityEur; }
    public BigDecimal getHighWaterMarkEquityEur() { return highWaterMarkEquityEur; }
    public void updateHighWaterMark(BigDecimal equity) { if (equity.compareTo(highWaterMarkEquityEur) > 0) highWaterMarkEquityEur = equity; }
}
