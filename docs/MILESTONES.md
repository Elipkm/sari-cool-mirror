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

## v0.5.0 — Paper Validation

**Goal:** decide objectively whether the strategy deserves real money.

Done when:
- continuous paper run data is available
- benchmark comparison after fees
- drawdown and risk metrics
- simple weekly report
- explicit go/no-go criteria for live trading
- no live adapter enabled by default

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
