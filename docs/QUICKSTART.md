# Quick Start

The system is paper-only.

## Start

```bash
docker compose up --build
```

Backend: `http://localhost:8080`

Dashboard: `http://localhost:4200`

## Test the strategy on history

Open the dashboard and use **Historical Simulator**:

1. Choose a start date.
2. Select **Test run (+1 day)** to inspect one completed market day.
3. Select **Run to latest** to evaluate the full period.

The simulation starts with €5,000, processes BTC, ETH and SOL, and includes the configured fees, slippage and deterministic risk rules. It shows the equity curve, equal-weight buy-and-hold comparison, drawdown, costs, decisions and simulated trades.

Historical simulation is held in memory and is completely separate from the persistent paper account. Changing the start date or restarting the backend resets it.

## Make one paper trade

```bash
curl -X POST http://localhost:8080/api/paper-trades \
  -H "Content-Type: application/json" \
  -d '{
    "asset":"BTC",
    "side":"BUY",
    "strategy":"trend_pullback",
    "referencePrice":100,
    "invalidationPrice":95,
    "signalStrength":0.80,
    "thesis":"Simple trend pullback with a clear invalidation level"
  }'
```

The backend decides the allowed position size. The caller never chooses it.

## Read the ledger

```bash
curl http://localhost:8080/api/paper-trades
```

You should see the paper trade with its approved size, simulated fill price, fee and decision reason.

## Run the proof test

```bash
cd backend
mvn test
```

That test starts the application, submits a paper trade through HTTP, executes it through the risk gate, stores it, and reads it back through HTTP.
