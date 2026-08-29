package at.sari.trader.api;

import at.sari.trader.paper.PaperTradeResult;
import at.sari.trader.paper.PaperTradingService;
import at.sari.trader.risk.TradeProposal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/paper-trades")
public class PaperTradeController {
    private final PaperTradingService paperTradingService;

    public PaperTradeController(PaperTradingService paperTradingService) {
        this.paperTradingService = paperTradingService;
    }

    @PostMapping
    PaperTradeResult execute(@Valid @RequestBody TradeProposal proposal) {
        return paperTradingService.execute(proposal);
    }

    @GetMapping
    List<PaperTradeResult> ledger() {
        return paperTradingService.ledger();
    }
}
