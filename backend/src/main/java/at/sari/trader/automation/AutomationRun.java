package at.sari.trader.automation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "automation_run")
public class AutomationRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant startedAt;
    @Column(nullable = false)
    private Instant completedAt;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(nullable = false, length = 2000)
    private String summary;

    protected AutomationRun() {}

    public static AutomationRun completed(Instant startedAt, String status, String summary) {
        AutomationRun run = new AutomationRun();
        run.startedAt = startedAt;
        run.completedAt = Instant.now();
        run.status = status;
        run.summary = summary;
        return run;
    }

    public Long getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getStatus() { return status; }
    public String getSummary() { return summary; }
}
