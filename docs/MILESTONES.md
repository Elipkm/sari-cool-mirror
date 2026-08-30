# Milestones

The project advances only in small, usable releases.

Every milestone must end with:

1. a working user-visible capability
2. an automated end-to-end test proving the main flow
3. a short release report in `docs/releases/`
4. a versioned release point
5. documentation that a future me can understand quickly

If a milestone needs much more than this scope, split it.

## v0.1.0 — Paper Trade Loop

**Goal:** prove one complete safe trade flow.

`Trade Idea -> Risk Check -> Paper Execution -> Ledger`

Done when:
- POST a structured trade idea
- deterministic risk engine approves/rejects it
- approved trades receive simple fee + slippage simulation
- approved and rejected decisions are persisted
- GET the ledger through the API
- end-to-end test proves the full flow

## v0.2.0 — Real Market State

**Goal:** make paper decisions use real market prices and a real paper-account state.

Done when:
- one reliable market-price source
- persisted cash and positions
- portfolio value derived from current prices
- risk engine uses authoritative account state
- restart does not lose paper-account state
- end-to-end test proves state changes after a trade

## v0.3.0 — One Swing Strategy

**Goal:** let the system generate its own simple trade ideas.

Done when:
- one understandable trend/pullback strategy
- clear entry and invalidation rules
- BTC, ETH and SOL only
- no LLM required for the trading decision
- historical test with fees/slippage
- end-to-end test proves market data -> signal -> risk -> paper trade

## v0.4.0 — Weekly Review

**Goal:** make the system easy to use in real life.

Done when one page shows:
- account value and P/L
- positions
- trades and reasons
- rule status
- BTC/ETH benchmark comparison
- anything needing attention
- end-to-end test proves backend data reaches the dashboard

## v0.5.0 — Autonomous Paper Loop

**Goal:** run the paper strategy unattended and collect a real track record.

Done when:
- BTC, ETH and SOL are evaluated daily
- signals pass through the deterministic risk and paper execution path
- automation runs and failures are persisted
- the dashboard shows the latest automation result
- manual and scheduled runs use the same code path

## v0.6.0 — Historical Validation

**Goal:** evaluate the complete strategy/risk behavior without waiting months.

Done when:
- select a historical start date
- advance exactly one completed day for inspection
- run the entire selected period to the latest completed day
- historical BUY signals use the production deterministic risk engine
- fees and slippage are included
- strategy equity is compared with equal-weight BTC/ETH/SOL buy-and-hold
- equity curve, trade ledger, drawdown, win rate and costs are visible
- simulation remains isolated from the real paper account
- end-to-end test proves the full historical flow

## v0.7.0 — Strategy Decision

**Goal:** make an evidence-based go/no-go decision on the current strategy.

Done when:
- several start dates and market regimes are evaluated
- parameters are frozen before evaluation
- results are separated into development and out-of-sample periods
- explicit return, drawdown and trade-count criteria are documented
- an explicit ACCEPT, REVISE or REJECT verdict is recorded
- no parameter tuning reuses the same out-of-sample result as fresh evidence

## v0.8.0 — Reliable Autonomous Operation

**Goal:** make unattended paper operation resilient to ordinary failures.

Done when:
- missed daily runs catch up safely after restart
- containers restart automatically
- stale market data and API failures are visible
- health status distinguishes data, backend and scheduler problems
- restart and recovery are covered end to end

## v0.9.0 — Live Safety Rehearsal

**Goal:** prove live-shaped safety controls without placing real orders.

Done when:
- fake exchange adapter exercises order and reconciliation flows
- kill switch and hard capital ceiling are proven
- paper/live mode cannot be confused
- credential permissions are documented and validated
- all live boundaries remain mocked or sandboxed

## v1.0.0 — Small Live Account

**Goal:** optional real-world execution with a deliberately small isolated account.

Only start if v0.5.0 demonstrates a useful edge and stable operation.

Done when:
- Kraken adapter behind the existing exchange boundary
- trade-only API key
- no withdrawal/transfer permission
- account reconciliation
- kill switch
- paper/live mode is obvious
- live amount has a hard ceiling
- end-to-end test uses sandbox/mocked exchange boundaries before release

## Rule

Do not build v0.3 features while v0.2 is unfinished. Do not build live trading while paper trading has not earned it.
