package at.sari.trader.api;

import at.sari.trader.strategy.BacktestResult;
import at.sari.trader.strategy.BacktestService;
import at.sari.trader.strategy.StrategyRunResult;
import at.sari.trader.strategy.StrategyRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {
    private final StrategyRunnerService service;
    private final BacktestService backtestService;

    public StrategyController(StrategyRunnerService service, BacktestService backtestService) {
        this.service = service;
        this.backtestService = backtestService;
    }

    @PostMapping("/run/{asset}")
    public ResponseEntity<StrategyRunResult> run(@PathVariable String asset) {
        return ResponseEntity.ok(service.run(asset));
    }

    @GetMapping("/backtest/{asset}")
    public ResponseEntity<BacktestResult> backtest(@PathVariable String asset) {
        return ResponseEntity.ok(backtestService.run(asset));
    }
}
