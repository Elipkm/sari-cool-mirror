# Simple Autonomous Crypto Trader

A small personal trading system designed to be useful in the real world, not impressive on paper.

> Status: **paper trading only**. No real-money exchange execution is enabled.

## Overall guide

**Keep it as simple as possible.**

The project should always prefer:

- fewer components over more components
- simple rules over clever rules
- deterministic logic over opaque automation
- one good strategy over many experimental strategies
- clear risk limits over complex portfolio theory
- easy operation over technical sophistication
- understandable decisions over black-box predictions
- useful weekly review over constant monitoring

A new component, model, data source or rule is only added when it solves a real problem or measurably improves results.

## Core trading principle

`Observe -> Decide -> Risk Check -> Trade -> Record -> Review`

The AI may research and form a trading thesis. It does **not** control capital directly.

The deterministic risk layer decides whether a trade is allowed and how large it may be.

## Simple rules

1. Trade only a small whitelist of liquid assets: initially BTC, ETH and SOL.
2. No leverage.
3. Every trade needs an entry idea and an invalidation/stop price.
4. Risk only a small fixed fraction of account equity per trade.
5. Stop opening new positions after predefined loss/drawdown limits.
6. Never allow withdrawal or transfer permissions on an exchange API key.
7. Record every decision and result.
8. Compare performance against simply holding BTC/ETH.
9. Change strategy slowly and only from evidence.
10. Human approval is required to change hard risk rules.

## Product goal

The finished product should feel simple:

1. Start the system.
2. It watches the market and manages its fixed account.
3. It trades only when the rules allow it.
4. I receive alerts only when something important happens.
5. Once a week I open one dashboard and see:
   - account value
   - profit/loss
   - open positions
   - trades made
   - why they were made
   - whether rules were respected
   - BTC/ETH benchmark comparison
   - whether anything needs my attention

That is the target user experience.

## Architecture principle

Keep the runtime path short:

`Market Data -> Strategy/AI -> Risk Engine -> Exchange -> Ledger`

The dashboard reads from the ledger and portfolio state.

Everything else is optional until proven necessary.

## Repository

- `backend/` Spring Boot trading core, risk engine, portfolio state and execution control
- `dashboard/` Angular review dashboard
- `config/trading-charter.yml` human-owned trading rules
- `docs/` architecture and design notes
- `quant/` quantitative experiments; should remain optional and may be removed if the backend can handle the required calculations simply

## Development order

Build only what is needed for the next usable stage:

1. Persistent paper account.
2. Market prices.
3. One simple swing strategy.
4. Deterministic position sizing and risk rules.
5. Paper execution with realistic fees/slippage.
6. Decision/trade ledger.
7. Minimal weekly dashboard.
8. Paper-trading evaluation against buy-and-hold.
9. Only then consider an LLM research layer.
10. Only after convincing paper results consider a tiny live account.

## Safety

The exchange integration must use a dedicated account/sub-account with only allocated trading capital.

Allowed API capabilities:

- read balances and positions
- read market/account state
- create/cancel orders

Forbidden capabilities:

- withdrawals
- transfers
- API-key administration

The AI must never call the exchange directly. Every order goes through the deterministic risk and execution layer.

## Definition of success

Success is **not** maximum autonomy, number of agents, number of indicators, or architectural complexity.

Success means the system is:

- easy to run
- easy to understand
- hard to misuse
- cheap to operate
- disciplined
- measurable
- and, after fees and risk, better enough than a simple benchmark to justify its existence

If complexity does not clearly help one of those goals, do not add it.

This project is experimental software, not financial advice.
