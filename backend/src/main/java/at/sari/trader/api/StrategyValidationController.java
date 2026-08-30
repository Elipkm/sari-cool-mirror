package at.sari.trader.api;

import at.sari.trader.validation.StrategyValidationReport;
import at.sari.trader.validation.StrategyValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/validation")
public class StrategyValidationController {
    private final StrategyValidationService service;

    public StrategyValidationController(StrategyValidationService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<StrategyValidationReport> evaluate() {
        return ResponseEntity.ok(service.evaluate());
    }
}
