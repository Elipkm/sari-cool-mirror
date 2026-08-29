# Architecture

## Boundary of autonomy

The system is intentionally split between probabilistic intelligence and deterministic control.

```text
Market Data / News / Portfolio
            |
            v
    Research + Quant Layer
            |
            v
   Portfolio Manager Agent
            |
       TradeProposal
            |
            v
      HARD RISK ENGINE
       |           |
    REJECT       ALLOW
                    |
                    v
            Execution Adapter
                    |
                    v
          Decision / Trade Ledger
                    |
                    v
                Dashboard
```

## Components

### Spring Boot control plane
Owns portfolio state, risk policy, proposal validation, execution authorization, audit events and eventually exchange adapters. It must never trust an LLM to enforce limits.

### Quant service
Python service for indicators, backtesting, volatility, regime features and strategy evaluation. Numerical calculations belong here rather than in prompts.

### AI layer (next milestone)
Agents will produce typed proposals and research notes. Planned roles:

- Research Agent
- Market Regime Agent
- Strategy Agents
- Portfolio Manager Agent
- Review/Critic Agent

They may propose actions but cannot directly place orders.

### Dashboard
Human governance interface. Weekly/monthly review should surface performance, benchmark, drawdown, exposure, rejected decisions, rationale quality, strategy attribution and proposed charter changes.

## Execution safety

The future exchange adapter must enforce:

1. trade-only API credentials;
2. no withdrawals or transfers;
3. isolated sub-account;
4. idempotent order submission;
5. order-size validation again at execution time;
6. kill switch independent of the model;
7. immutable audit log.

## Autonomy rollout

1. Historical backtest.
2. Paper trading.
3. AI proposes / human approves.
4. Autonomous tiny paper/live sandbox allocation.
5. Fixed-capital autonomous account.
6. Higher autonomy only after stable risk-adjusted results.

Primary evaluation metrics: return after fees, benchmark-relative return, max drawdown, Sharpe/Sortino, turnover, slippage and strategy-level expectancy.
