package at.sari.trader.automation;

import at.sari.trader.strategy.StrategyRunResult;
import at.sari.trader.strategy.StrategyRunnerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AutonomousPaperLoop {
    private static final List<String> ASSETS = List.of("BTC", "ETH", "SOL");

    private final StrategyRunnerService strategyRunnerService;
    private final AutomationRunRepository runRepository;

    public AutonomousPaperLoop(StrategyRunnerService strategyRunnerService, AutomationRunRepository runRepository) {
        this.strategyRunnerService = strategyRunnerService;
        this.runRepository = runRepository;
    }

    @Scheduled(cron = "${trading.automation.cron:0 15 0 * * *}", zone = "UTC")
    public void scheduledRun() {
        runOnce();
    }

    public synchronized AutomationRunResult runOnce() {
        Instant startedAt = Instant.now();
        List<StrategyRunResult> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String asset : ASSETS) {
            try {
                results.add(strategyRunnerService.run(asset));
            } catch (Exception ex) {
                errors.add(asset + ": " + ex.getClass().getSimpleName());
            }
        }

        String status = errors.isEmpty() ? "SUCCESS" : results.isEmpty() ? "FAILED" : "PARTIAL";
        String summary = summarize(results, errors);
        AutomationRun persisted = runRepository.save(AutomationRun.completed(startedAt, status, summary));

        return new AutomationRunResult(
                persisted.getStartedAt(),
                persisted.getCompletedAt(),
                persisted.getStatus(),
                List.copyOf(results),
                persisted.getSummary()
        );
    }

    private String summarize(List<StrategyRunResult> results, List<String> errors) {
        long buys = results.stream().filter(r -> r.action().name().equals("BUY")).count();
        long exits = results.stream().filter(r -> r.action().name().equals("EXIT")).count();
        long holds = results.stream().filter(r -> r.action().name().equals("HOLD")).count();
        String base = "Scanned " + ASSETS.size() + " assets: BUY=" + buys + ", EXIT=" + exits + ", HOLD=" + holds;
        return errors.isEmpty() ? base : base + "; errors=" + String.join(", ", errors);
    }
}
