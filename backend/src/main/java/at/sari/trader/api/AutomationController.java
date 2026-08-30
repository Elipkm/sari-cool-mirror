package at.sari.trader.api;

import at.sari.trader.automation.AutomationRun;
import at.sari.trader.automation.AutomationRunRepository;
import at.sari.trader.automation.AutomationRunResult;
import at.sari.trader.automation.AutonomousPaperLoop;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/automation")
public class AutomationController {
    private final AutonomousPaperLoop loop;
    private final AutomationRunRepository repository;

    public AutomationController(AutonomousPaperLoop loop, AutomationRunRepository repository) {
        this.loop = loop;
        this.repository = repository;
    }

    @PostMapping("/run")
    AutomationRunResult runNow() {
        return loop.runOnce();
    }

    @GetMapping("/last")
    AutomationRun last() {
        return repository.findTopByOrderByStartedAtDesc().orElse(null);
    }
}
