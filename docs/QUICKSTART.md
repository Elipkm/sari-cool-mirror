# Quick Start

The system is paper-only.

## Start

```bash
docker compose up --build
```

Backend: `http://localhost:8080`

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
