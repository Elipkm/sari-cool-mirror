package at.sari.trader.api;

import at.sari.trader.risk.PortfolioState;
import at.sari.trader.risk.PortfolioStateProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    private final PortfolioStateProvider portfolioStateProvider;

    public PortfolioController(PortfolioStateProvider portfolioStateProvider) {
        this.portfolioStateProvider = portfolioStateProvider;
    }

    @GetMapping
    PortfolioState portfolio() {
        return portfolioStateProvider.currentState();
    }
}
