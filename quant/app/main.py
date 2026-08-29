from statistics import fmean
from typing import Literal

from fastapi import FastAPI
from pydantic import BaseModel, Field

app = FastAPI(title="Sari Quant Service", version="0.1.0")


class MarketSeries(BaseModel):
    asset: str
    closes: list[float] = Field(min_length=20)


class Signal(BaseModel):
    asset: str
    regime: Literal["bullish", "bearish", "neutral"]
    momentum_pct: float
    sma_20: float


@app.get("/health")
def health():
    return {"status": "UP", "mode": "PAPER"}


@app.post("/signals/basic", response_model=Signal)
def basic_signal(series: MarketSeries):
    closes = series.closes
    sma_20 = fmean(closes[-20:])
    momentum_pct = ((closes[-1] / closes[-20]) - 1) * 100

    if closes[-1] > sma_20 and momentum_pct > 1:
        regime = "bullish"
    elif closes[-1] < sma_20 and momentum_pct < -1:
        regime = "bearish"
    else:
        regime = "neutral"

    return Signal(
        asset=series.asset.upper(),
        regime=regime,
        momentum_pct=round(momentum_pct, 4),
        sma_20=round(sma_20, 4),
    )
