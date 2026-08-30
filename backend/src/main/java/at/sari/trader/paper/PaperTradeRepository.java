package at.sari.trader.paper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperTradeRepository extends JpaRepository<PaperTrade, Long> {
    List<PaperTrade> findTop10ByOrderByCreatedAtDesc();
}
