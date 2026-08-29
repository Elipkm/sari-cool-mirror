package at.sari.trader.api;

import at.sari.trader.risk.RiskDecision;
import at.sari.trader.risk.RiskEngine;
import at.sari.trader.risk.TradeProposal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/decisions")
public class DecisionController {
    private final RiskEngine riskEngine;

    public DecisionController(RiskEngine riskEngine) {
        this.riskEngine = riskEngine;
    }

    @GetMapping("/health")
    Map<String, Object> health() {
        return Map.of("status", "UP", "mode", "PAPER");
    }

    @PostMapping("/evaluate")
    RiskDecision evaluate(@Valid @RequestBody TradeProposal proposal) {
        return riskEngine.evaluate(proposal);
    }
}
