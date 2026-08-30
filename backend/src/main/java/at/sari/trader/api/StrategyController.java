package at.sari.trader.api;

import at.sari.trader.strategy.StrategyRunResult;
import at.sari.trader.strategy.StrategyRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {
    private final StrategyRunnerService service;

    public StrategyController(StrategyRunnerService service) {
        this.service = service;
    }

    @PostMapping("/run/{asset}")
    public ResponseEntity<StrategyRunResult> run(@PathVariable String asset) {
        return ResponseEntity.ok(service.run(asset));
    }
}
