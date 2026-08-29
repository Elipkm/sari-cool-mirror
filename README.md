# Autonomous Crypto Trading Agent

A bounded-autonomy crypto trading system. The AI can research, form theses and propose trades, but deterministic risk rules decide whether an order is allowed.

> Status: MVP foundation. **Paper trading first. No real-money exchange execution is enabled.**

## Core principle

`Observe -> Analyze -> Propose -> Risk Check -> Execute -> Record -> Review`

The risk engine outranks every model and agent. LLM output is advisory and never bypasses hard limits.

## Repository layout

- `backend/` Spring Boot control plane, risk engine and decision API
- `quant/` Python/FastAPI quantitative market-analysis service
- `dashboard/` Angular review dashboard
- `config/trading-charter.yml` human-owned trading constitution
- `docs/ARCHITECTURE.md` system design and roadmap

## MVP scope

1. Paper account with fixed starting capital.
2. Trade proposals represented as structured data.
3. Deterministic checks for position size, confidence, daily loss, drawdown and asset whitelist.
4. Quant service exposing basic market indicators.
5. Dashboard for portfolio/risk state and agent decisions.
6. Full decision ledger before any live trading integration.

## Run locally

Requirements: Docker + Docker Compose.

```bash
docker compose up --build
```

Services:

- Backend: http://localhost:8080
- Quant service: http://localhost:8000
- Dashboard: http://localhost:4200
- PostgreSQL: localhost:5432

Test the risk engine:

```bash
curl -X POST http://localhost:8080/api/decisions/evaluate \
  -H "Content-Type: application/json" \
  -d '{"asset":"BTC","side":"BUY","amountEur":250,"confidence":0.82,"currentAssetExposurePct":5,"dailyLossPct":0,"drawdownPct":0}'
```

## Safety model

Live exchange credentials must eventually use trade-only permissions. Withdrawals, transfers and API-key administration must remain disabled. A dedicated exchange sub-account should contain only capital allocated to the system.

## Next milestones

- Persist decisions and portfolio snapshots in PostgreSQL.
- Add exchange-neutral paper execution adapter.
- Add historical backtesting and benchmark comparison.
- Add market-regime classifier and strategy agents.
- Add LLM research/portfolio-manager layer with structured outputs.
- Add weekly AI investment-committee report.
- Only after a proven paper-trading period: optional live exchange adapter with strict permission checks.

This project is experimental software, not financial advice.
