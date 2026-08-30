package at.sari.trader.simulation;

import at.sari.trader.market.DailyCandle;
import at.sari.trader.market.MarketHistoryProvider;
import at.sari.trader.risk.CircuitBreakerState;
import at.sari.trader.risk.PortfolioState;
import at.sari.trader.risk.RiskDecision;
import at.sari.trader.risk.RiskEngine;
import at.sari.trader.risk.TradeProposal;
import at.sari.trader.strategy.StrategyDecision;
import at.sari.trader.strategy.TrendPullbackStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HistoricalSimulationService {
    private static final List<String> ASSETS = List.of("BTC", "ETH", "SOL");
    private static final BigDecimal STARTING_CAPITAL = new BigDecimal("5000.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final MarketHistoryProvider historyProvider;
    private final TrendPullbackStrategy strategy;
    private final RiskEngine riskEngine;
    private final BigDecimal feeRate;
    private final BigDecimal slippageRate;
    private Session session;

    public HistoricalSimulationService(
            MarketHistoryProvider historyProvider,
            TrendPullbackStrategy strategy,
            RiskEngine riskEngine,
            @Value("${trading.paper.fee-pct:0.25}") BigDecimal feePct,
            @Value("${trading.paper.slippage-pct:0.10}") BigDecimal slippagePct
    ) {
        this.historyProvider = historyProvider;
        this.strategy = strategy;
        this.riskEngine = riskEngine;
        this.feeRate = feePct.divide(HUNDRED, 10, RoundingMode.HALF_UP);
        this.slippageRate = slippagePct.divide(HUNDRED, 10, RoundingMode.HALF_UP);
    }

    public synchronized SimulationResult step(LocalDate requestedStartDate) {
        if (requestedStartDate == null) throw badRequest("startDate is required");
        if (session == null || !session.requestedStartDate.equals(requestedStartDate)) {
            session = createSession(requestedStartDate, LocalDate.now(ZoneOffset.UTC).minusDays(1));
        }
        return advanceOneDay();
    }

    public synchronized SimulationResult runToLatest(LocalDate requestedStartDate) {
        if (requestedStartDate == null) throw badRequest("startDate is required");
        session = createSession(requestedStartDate, LocalDate.now(ZoneOffset.UTC).minusDays(1));
        SimulationResult result = null;
        while (session.nextDay < session.days.size()) result = advanceOneDay();
        return result;
    }

    public synchronized SimulationResult runRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) throw badRequest("startDate and endDate are required");
        if (endDate.isBefore(startDate)) throw badRequest("endDate must not be before startDate");
        session = createSession(startDate, endDate);
        SimulationResult result = null;
        while (session.nextDay < session.days.size()) result = advanceOneDay();
        return result;
    }

    private SimulationResult advanceOneDay() {
        if (session.nextDay >= session.days.size()) throw badRequest("no more completed market days available");
        LocalDate day = session.days.get(session.nextDay++);
        if (day.getDayOfWeek() == DayOfWeek.MONDAY && session.currentDate != null) {
            session.startOfWeekEquity = session.lastEquity;
        }
        session.startOfDayEquity = session.lastEquity;

        Map<String, BigDecimal> closes = closesFor(day);
        List<SimulationDecision> decisions = new ArrayList<>();
        for (String asset : ASSETS) {
            List<DailyCandle> candles = session.history.get(asset);
            int candleIndex = indexOf(candles, day);
            BigDecimal close = closes.get(asset);
            Position position = session.positions.get(asset);
            StrategyDecision decision = strategy.decide(candles.subList(0, candleIndex + 1), position.isOpen());
            decisions.add(execute(day, asset, close, decision, closes));
        }

        session.iteration++;
        session.currentDate = day;
        BigDecimal equity = equity(closes);
        updateDrawdown(equity);
        session.lastEquity = equity;
        session.equityCurve.add(new EquityPoint(day, equity, benchmarkEquity(closes)));
        return result(equity, closes, decisions);
    }

    private SimulationDecision execute(LocalDate day, String asset, BigDecimal close,
                                       StrategyDecision decision, Map<String, BigDecimal> closes) {
        Position position = session.positions.get(asset);
        if (!position.isOpen() && decision.action() == StrategyDecision.Action.BUY) {
            TradeProposal proposal = new TradeProposal(
                    asset, TradeProposal.Side.BUY, "trend-pullback-v1", close,
                    decision.invalidationPrice(), 1.0, decision.reason());
            RiskDecision risk = riskEngine.evaluate(proposal, portfolioState(closes));
            if (!risk.allowed()) {
                return new SimulationDecision(asset, "BUY", "REJECTED", close, BigDecimal.ZERO,
                        String.join("; ", risk.reasons()));
            }

            BigDecimal notional = risk.approvedNotionalEur();
            BigDecimal fee = fee(notional);
            BigDecimal totalCost = notional.add(fee);
            if (totalCost.compareTo(session.cash) > 0) {
                return new SimulationDecision(asset, "BUY", "REJECTED", close, BigDecimal.ZERO,
                        "insufficient cash after fees");
            }
            BigDecimal fill = buyFill(close);
            BigDecimal quantity = notional.divide(fill, 12, RoundingMode.DOWN);
            position.open(quantity, totalCost);
            session.cash = session.cash.subtract(totalCost);
            session.totalFees = session.totalFees.add(fee);
            session.trades.add(new SimulationTrade(day, asset, "BUY", notional, fill, fee, null, decision.reason()));
            return new SimulationDecision(asset, "BUY", "EXECUTED", close, notional, decision.reason());
        }

        if (position.isOpen() && decision.action() == StrategyDecision.Action.EXIT) {
            BigDecimal fill = sellFill(close);
            BigDecimal notional = position.quantity.multiply(fill).setScale(2, RoundingMode.DOWN);
            BigDecimal fee = fee(notional);
            BigDecimal proceeds = notional.subtract(fee);
            BigDecimal pnl = proceeds.subtract(position.entryCost).setScale(2, RoundingMode.HALF_UP);
            session.cash = session.cash.add(proceeds);
            session.totalFees = session.totalFees.add(fee);
            session.completedTrades++;
            session.totalRealizedPnl = session.totalRealizedPnl.add(pnl);
            if (pnl.signum() > 0) session.winningTrades++;
            position.close();
            session.trades.add(new SimulationTrade(day, asset, "EXIT", notional, fill, fee, pnl, decision.reason()));
            return new SimulationDecision(asset, "EXIT", "EXECUTED", close, notional, decision.reason());
        }

        return new SimulationDecision(asset, decision.action().name(), "NONE", close, BigDecimal.ZERO, decision.reason());
    }

    private Session createSession(LocalDate requestedStartDate, LocalDate endDate) {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        if (!requestedStartDate.isBefore(todayUtc)) throw badRequest("startDate must be before today");
        if (!endDate.isBefore(todayUtc)) throw badRequest("endDate must be before today");

        Map<String, List<DailyCandle>> history = new LinkedHashMap<>();
        for (String asset : ASSETS) {
            List<DailyCandle> completed = historyProvider.dailyCandles(asset, 720).stream()
                    .filter(candle -> candle.time().atZone(ZoneOffset.UTC).toLocalDate().isBefore(todayUtc))
                    .toList();
            if (completed.size() < 51) throw badRequest("not enough completed history for " + asset);
            history.put(asset, completed);
        }

        Set<LocalDate> common = new LinkedHashSet<>(dates(history.get(ASSETS.getFirst())));
        for (String asset : ASSETS.subList(1, ASSETS.size())) common.retainAll(dates(history.get(asset)));
        List<LocalDate> eligibleDays = common.stream()
                .filter(date -> !date.isBefore(requestedStartDate))
                .filter(date -> !date.isAfter(endDate))
                .filter(date -> ASSETS.stream().allMatch(asset -> indexOf(history.get(asset), date) >= 50))
                .sorted().toList();
        if (eligibleDays.isEmpty()) throw badRequest("startDate is outside available completed history");

        LocalDate actualStart = eligibleDays.getFirst();
        Map<String, BigDecimal> startPrices = new LinkedHashMap<>();
        Map<String, Position> positions = new LinkedHashMap<>();
        for (String asset : ASSETS) {
            startPrices.put(asset, history.get(asset).get(indexOf(history.get(asset), actualStart)).close());
            positions.put(asset, new Position());
        }
        return new Session(requestedStartDate, actualStart, history, eligibleDays, startPrices, positions);
    }

    private PortfolioState portfolioState(Map<String, BigDecimal> closes) {
        return new PortfolioState(
                equity(closes), session.startOfDayEquity, session.startOfWeekEquity,
                session.peakEquity, session.cash, positionValues(closes), Map.of(), CircuitBreakerState.NORMAL);
    }

    private Map<String, BigDecimal> closesFor(LocalDate day) {
        Map<String, BigDecimal> closes = new LinkedHashMap<>();
        for (String asset : ASSETS) {
            List<DailyCandle> candles = session.history.get(asset);
            closes.put(asset, candles.get(indexOf(candles, day)).close());
        }
        return closes;
    }

    private Map<String, BigDecimal> positionValues(Map<String, BigDecimal> closes) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (String asset : ASSETS) {
            BigDecimal value = session.positions.get(asset).quantity.multiply(closes.get(asset))
                    .setScale(2, RoundingMode.HALF_UP);
            if (value.signum() > 0) values.put(asset, value);
        }
        return values;
    }

    private BigDecimal equity(Map<String, BigDecimal> closes) {
        BigDecimal value = session.cash;
        for (String asset : ASSETS) value = value.add(session.positions.get(asset).quantity.multiply(closes.get(asset)));
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal benchmarkEquity(Map<String, BigDecimal> closes) {
        BigDecimal returnPct = benchmarkReturnPct(closes);
        return STARTING_CAPITAL.multiply(BigDecimal.ONE.add(returnPct.divide(HUNDRED, 10, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal benchmarkReturnPct(Map<String, BigDecimal> closes) {
        BigDecimal total = BigDecimal.ZERO;
        for (String asset : ASSETS) {
            BigDecimal assetReturn = closes.get(asset).subtract(session.startPrices.get(asset))
                    .divide(session.startPrices.get(asset), 10, RoundingMode.HALF_UP).multiply(HUNDRED);
            total = total.add(assetReturn);
        }
        return total.divide(BigDecimal.valueOf(ASSETS.size()), 2, RoundingMode.HALF_UP);
    }

    private void updateDrawdown(BigDecimal equity) {
        if (equity.compareTo(session.peakEquity) > 0) session.peakEquity = equity;
        if (equity.compareTo(session.peakEquity) < 0) {
            BigDecimal drawdown = session.peakEquity.subtract(equity)
                    .divide(session.peakEquity, 10, RoundingMode.HALF_UP).multiply(HUNDRED);
            if (drawdown.compareTo(session.maxDrawdownPct) > 0) session.maxDrawdownPct = drawdown;
        }
    }

    private SimulationResult result(BigDecimal equity, Map<String, BigDecimal> closes,
                                    List<SimulationDecision> decisions) {
        BigDecimal strategyReturn = percentReturn(STARTING_CAPITAL, equity);
        BigDecimal winRate = session.completedTrades == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(session.winningTrades).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(session.completedTrades), 2, RoundingMode.HALF_UP);
        BigDecimal averagePnl = session.completedTrades == 0 ? BigDecimal.ZERO
                : session.totalRealizedPnl.divide(BigDecimal.valueOf(session.completedTrades), 2, RoundingMode.HALF_UP);
        return new SimulationResult(
                session.actualStartDate, session.currentDate, session.iteration, STARTING_CAPITAL,
                equity, session.cash.setScale(2, RoundingMode.HALF_UP), strategyReturn,
                benchmarkReturnPct(closes), session.maxDrawdownPct.setScale(2, RoundingMode.HALF_UP),
                session.completedTrades, winRate, averagePnl, session.totalFees.setScale(2, RoundingMode.HALF_UP),
                Map.copyOf(positionValues(closes)), List.copyOf(decisions),
                List.copyOf(session.equityCurve), List.copyOf(session.trades),
                session.nextDay < session.days.size());
    }

    private BigDecimal percentReturn(BigDecimal start, BigDecimal end) {
        return end.subtract(start).divide(start, 10, RoundingMode.HALF_UP).multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal fee(BigDecimal notional) {
        return notional.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal buyFill(BigDecimal close) {
        return close.multiply(BigDecimal.ONE.add(slippageRate)).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal sellFill(BigDecimal close) {
        return close.multiply(BigDecimal.ONE.subtract(slippageRate)).setScale(8, RoundingMode.HALF_UP);
    }

    private List<LocalDate> dates(List<DailyCandle> candles) {
        return candles.stream().map(c -> c.time().atZone(ZoneOffset.UTC).toLocalDate()).toList();
    }

    private int indexOf(List<DailyCandle> candles, LocalDate date) {
        for (int i = 0; i < candles.size(); i++) {
            if (candles.get(i).time().atZone(ZoneOffset.UTC).toLocalDate().equals(date)) return i;
        }
        return -1;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static final class Position {
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal entryCost = BigDecimal.ZERO;
        private boolean isOpen() { return quantity.signum() > 0; }
        private void open(BigDecimal newQuantity, BigDecimal newEntryCost) {
            quantity = newQuantity;
            entryCost = newEntryCost;
        }
        private void close() {
            quantity = BigDecimal.ZERO;
            entryCost = BigDecimal.ZERO;
        }
    }

    private static final class Session {
        private final LocalDate requestedStartDate;
        private final LocalDate actualStartDate;
        private final Map<String, List<DailyCandle>> history;
        private final List<LocalDate> days;
        private final Map<String, BigDecimal> startPrices;
        private final Map<String, Position> positions;
        private final List<EquityPoint> equityCurve = new ArrayList<>();
        private final List<SimulationTrade> trades = new ArrayList<>();
        private BigDecimal cash = STARTING_CAPITAL;
        private BigDecimal lastEquity = STARTING_CAPITAL;
        private BigDecimal startOfDayEquity = STARTING_CAPITAL;
        private BigDecimal startOfWeekEquity = STARTING_CAPITAL;
        private BigDecimal peakEquity = STARTING_CAPITAL;
        private BigDecimal maxDrawdownPct = BigDecimal.ZERO;
        private BigDecimal totalFees = BigDecimal.ZERO;
        private BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        private LocalDate currentDate;
        private int nextDay;
        private int iteration;
        private int completedTrades;
        private int winningTrades;

        private Session(LocalDate requestedStartDate, LocalDate actualStartDate,
                        Map<String, List<DailyCandle>> history, List<LocalDate> days,
                        Map<String, BigDecimal> startPrices, Map<String, Position> positions) {
            this.requestedStartDate = requestedStartDate;
            this.actualStartDate = actualStartDate;
            this.history = history;
            this.days = days;
            this.startPrices = startPrices;
            this.positions = positions;
        }
    }
}
