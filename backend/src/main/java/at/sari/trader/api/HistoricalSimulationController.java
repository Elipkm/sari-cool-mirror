package at.sari.trader.api;

import at.sari.trader.simulation.HistoricalSimulationService;
import at.sari.trader.simulation.SimulationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/simulation")
public class HistoricalSimulationController {
    private final HistoricalSimulationService service;

    public HistoricalSimulationController(HistoricalSimulationService service) {
        this.service = service;
    }

    @PostMapping("/step")
    public ResponseEntity<SimulationResult> step(@RequestParam LocalDate startDate) {
        return ResponseEntity.ok(service.step(startDate));
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationResult> runToLatest(@RequestParam LocalDate startDate) {
        return ResponseEntity.ok(service.runToLatest(startDate));
    }
}
