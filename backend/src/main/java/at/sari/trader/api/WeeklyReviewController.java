package at.sari.trader.api;

import at.sari.trader.review.WeeklyReview;
import at.sari.trader.review.WeeklyReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class WeeklyReviewController {
    private final WeeklyReviewService weeklyReviewService;

    public WeeklyReviewController(WeeklyReviewService weeklyReviewService) {
        this.weeklyReviewService = weeklyReviewService;
    }

    @GetMapping("/weekly")
    WeeklyReview weekly() {
        return weeklyReviewService.current();
    }
}
