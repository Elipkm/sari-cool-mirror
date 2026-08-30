package at.sari.trader.automation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutomationRunRepository extends JpaRepository<AutomationRun, Long> {
    Optional<AutomationRun> findTopByOrderByStartedAtDesc();
}
