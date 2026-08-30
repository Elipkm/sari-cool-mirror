import { bootstrapApplication } from '@angular/platform-browser';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, provideHttpClient } from '@angular/common/http';

interface Trade {
  createdAt: string;
  asset: string;
  side: string;
  status: string;
  strategy: string;
  approvedNotionalEur: number;
  thesis: string;
}

interface WeeklyReview {
  equityEur: number;
  cashEur: number;
  returnPct: number;
  weeklyReturnPct: number;
  maxDrawdownPct: number;
  systemState: string;
  positionsEur: Record<string, number>;
  recentTrades: Trade[];
}

interface AutomationRun {
  startedAt: string;
  completedAt: string;
  status: string;
  summary: string;
}

interface SimulationDecision {
  asset: string;
  action: string;
  execution: string;
  closeEur: number;
  approvedNotionalEur: number;
  reason: string;
}

interface EquityPoint {
  date: string;
  equityEur: number;
  benchmarkEquityEur: number;
}

interface SimulationTrade {
  date: string;
  asset: string;
  side: string;
  notionalEur: number;
  fillPriceEur: number;
  feeEur: number;
  realizedPnlEur?: number;
  reason: string;
}

interface SimulationResult {
  startDate: string;
  currentDate: string;
  iteration: number;
  startingCapitalEur: number;
  equityEur: number;
  cashEur: number;
  returnPct: number;
  buyAndHoldReturnPct: number;
  maxDrawdownPct: number;
  completedTrades: number;
  winRatePct: number;
  averageTradePnlEur: number;
  totalFeesEur: number;
  positionsEur: Record<string, number>;
  decisions: SimulationDecision[];
  equityCurve: EquityPoint[];
  trades: SimulationTrade[];
  hasNextDay: boolean;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main>
      <header>
        <div><p class="eyebrow">WEEKLY REVIEW · AUTONOMOUS PAPER TRADING</p><h1>Sari Autonomous Fund</h1></div>
        <div class="status" [class.warn]="review?.systemState !== 'NORMAL'">● {{ review?.systemState || 'Loading' }}</div>
      </header>

      <div *ngIf="error" class="error">Dashboard API unavailable. Start the backend and refresh.</div>

      <section class="grid metrics">
        <article><span>Portfolio</span><strong>{{ review?.equityEur | currency:'EUR' }}</strong><small>Return {{ signed(review?.returnPct) }}%</small></article>
        <article><span>This week</span><strong>{{ signed(review?.weeklyReturnPct) }}%</strong><small>vs weekly equity baseline</small></article>
        <article><span>Max drawdown</span><strong>{{ review?.maxDrawdownPct ?? 0 | number:'1.2-2' }}%</strong><small>Hard stop: 15%</small></article>
        <article><span>Cash</span><strong>{{ review?.cashEur | currency:'EUR' }}</strong><small>{{ positionCount }} open position(s)</small></article>
      </section>

      <section class="simulator">
        <article>
          <div class="sim-header">
            <div>
              <p class="eyebrow">HISTORICAL SIMULATOR · ISOLATED FROM PAPER ACCOUNT</p>
              <h2>Test the strategy against completed market history</h2>
            </div>
            <div class="sim-controls">
              <label>Start date<input type="date" [value]="simulationStartDate" (change)="simulationStartDate = $any($event.target).value"></label>
              <button class="secondary" (click)="testRun()" [disabled]="simulationBusy">
                {{ simulationBusy && simulationMode === 'step' ? 'Running…' : 'Test run (+1 day)' }}
              </button>
              <button (click)="runToLatest()" [disabled]="simulationBusy">
                {{ simulationBusy && simulationMode === 'all' ? 'Testing…' : 'Run to latest' }}
              </button>
            </div>
          </div>
          <div *ngIf="simulationError" class="error">{{ simulationError }}</div>
          <p *ngIf="!simulation" class="empty">Choose a historical start date. Each click processes one completed day for BTC, ETH and SOL.</p>
          <ng-container *ngIf="simulation">
            <div class="sim-summary">
              <div><span>Simulated date</span><strong>{{ simulation.currentDate }}</strong><small>Iteration {{ simulation.iteration }}</small></div>
              <div><span>Portfolio</span><strong>{{ simulation.equityEur | currency:'EUR' }}</strong><small>Strategy {{ signed(simulation.returnPct) }}%</small></div>
              <div><span>Buy & hold</span><strong>{{ signed(simulation.buyAndHoldReturnPct) }}%</strong><small>Equal-weight BTC / ETH / SOL</small></div>
              <div><span>Max drawdown</span><strong>{{ simulation.maxDrawdownPct | number:'1.2-2' }}%</strong><small>{{ simulation.completedTrades }} completed trade(s)</small></div>
              <div><span>Win rate</span><strong>{{ simulation.winRatePct | number:'1.2-2' }}%</strong><small>Closed positions only</small></div>
              <div><span>Average trade P/L</span><strong>{{ simulation.averageTradePnlEur | currency:'EUR' }}</strong><small>After costs</small></div>
              <div><span>Total fees</span><strong>{{ simulation.totalFeesEur | currency:'EUR' }}</strong><small>0.25% per execution</small></div>
              <div><span>Cash</span><strong>{{ simulation.cashEur | currency:'EUR' }}</strong><small>{{ simulatedPositionCount }} open position(s)</small></div>
            </div>

            <div class="chart" *ngIf="simulation.equityCurve.length > 1">
              <div class="chart-head"><b>Equity curve</b><span><i class="strategy-key"></i> Strategy <i class="benchmark-key"></i> Buy & hold</span></div>
              <svg viewBox="0 0 1000 240" preserveAspectRatio="none" role="img" aria-label="Strategy and benchmark equity curves">
                <polyline class="benchmark-line" [attr.points]="benchmarkLine"></polyline>
                <polyline class="strategy-line" [attr.points]="strategyLine"></polyline>
              </svg>
              <div class="chart-axis"><span>{{ simulation.startDate }}</span><span>{{ simulation.currentDate }}</span></div>
            </div>

            <p class="eyebrow section-gap">LATEST DAY</p>
            <div class="decision" *ngFor="let decision of simulation.decisions">
              <b>{{ decision.action }} {{ decision.asset }}<small *ngIf="decision.execution !== 'NONE'">{{ decision.execution }}</small></b>
              <span>{{ decision.closeEur | currency:'EUR' }}</span>
              <small>{{ decision.reason }}</small>
            </div>
            <div class="sim-ledger" *ngIf="simulation.trades.length">
              <p class="eyebrow section-gap">SIMULATED TRADE LEDGER</p>
              <div class="trade" *ngFor="let trade of recentSimulationTrades">
                <div class="trade-head"><b>{{ trade.side }} {{ trade.asset }}</b><span>{{ trade.notionalEur | currency:'EUR' }} · fee {{ trade.feeEur | currency:'EUR' }}</span></div>
                <p>{{ trade.reason }}</p>
                <small>{{ trade.date }} · fill {{ trade.fillPriceEur | currency:'EUR' }}<ng-container *ngIf="trade.realizedPnlEur != null"> · P/L {{ trade.realizedPnlEur | currency:'EUR' }}</ng-container></small>
              </div>
            </div>
            <p *ngIf="!simulation.hasNextDay" class="empty">You reached the latest completed market day. Choose an earlier start date to reset.</p>
          </ng-container>
        </article>
      </section>

      <section class="grid lower">
        <article>
          <p class="eyebrow">AUTOMATION</p><h2>Daily paper loop</h2>
          <div class="rule"><b>Schedule</b><span>00:15 UTC daily</span></div>
          <div class="rule"><b>Last run</b><span>{{ automation?.status || 'Not run yet' }}</span></div>
          <div class="rule" *ngIf="automation"><b>Completed</b><span>{{ automation.completedAt | date:'medium' }}</span></div>
          <p *ngIf="automation"><small>{{ automation.summary }}</small></p>

          <p class="eyebrow section-gap">OPEN POSITIONS</p><h2>Capital at work</h2>
          <div *ngIf="positionCount === 0" class="empty">No open positions.</div>
          <div class="rule" *ngFor="let p of positions"><b>{{ p.key }}</b><span>{{ p.value | currency:'EUR' }}</span></div>

          <p class="eyebrow section-gap">RISK</p>
          <div class="rule"><b>System state</b><span>{{ review?.systemState }}</span></div>
          <div class="rule"><b>Drawdown</b><span>{{ review?.maxDrawdownPct ?? 0 | number:'1.2-2' }} / 15%</span></div>
          <div class="rule"><b>Leverage</b><span>Disabled</span></div>
        </article>

        <article>
          <p class="eyebrow">RECENT ACTIVITY</p><h2>What happened and why</h2>
          <div *ngIf="!review?.recentTrades?.length" class="empty">No trades yet.<br><small>The strategy is waiting for a valid setup.</small></div>
          <div class="trade" *ngFor="let trade of review?.recentTrades">
            <div class="trade-head"><b>{{ trade.side }} {{ trade.asset }}</b><span>{{ trade.status }} · {{ trade.approvedNotionalEur | currency:'EUR' }}</span></div>
            <p>{{ trade.thesis }}</p><small>{{ trade.strategy }} · {{ trade.createdAt | date:'medium' }}</small>
          </div>
        </article>
      </section>
    </main>`
})
class AppComponent implements OnInit {
  review?: WeeklyReview;
  automation?: AutomationRun;
  simulation?: SimulationResult;
  simulationStartDate = this.defaultStartDate();
  simulationBusy = false;
  simulationMode: 'step' | 'all' | '' = '';
  simulationError = '';
  error = false;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<WeeklyReview>('/api/review/weekly').subscribe({
      next: review => this.review = review,
      error: () => this.error = true
    });
    this.http.get<AutomationRun | null>('/api/automation/last').subscribe({
      next: run => this.automation = run || undefined
    });
  }

  get positions() { return Object.entries(this.review?.positionsEur || {}).map(([key, value]) => ({ key, value })); }
  get positionCount() { return this.positions.filter(p => p.value > 0).length; }
  get simulatedPositionCount() { return Object.keys(this.simulation?.positionsEur || {}).length; }
  get recentSimulationTrades() { return [...(this.simulation?.trades || [])].reverse().slice(0, 20); }
  get strategyLine() { return this.chartLine('equityEur'); }
  get benchmarkLine() { return this.chartLine('benchmarkEquityEur'); }
  signed(value?: number) { const n = value ?? 0; return `${n > 0 ? '+' : ''}${n.toFixed(2)}`; }

  testRun() {
    this.runSimulation('step');
  }

  runToLatest() {
    this.runSimulation('all');
  }

  private runSimulation(mode: 'step' | 'all') {
    this.simulationBusy = true;
    this.simulationMode = mode;
    this.simulationError = '';
    const endpoint = mode === 'all' ? 'run' : 'step';
    this.http.post<SimulationResult>(`/api/simulation/${endpoint}?startDate=${encodeURIComponent(this.simulationStartDate)}`, null).subscribe({
      next: result => {
        this.simulation = result;
        this.simulationBusy = false;
        this.simulationMode = '';
      },
      error: response => {
        this.simulationError = response?.error?.detail || response?.error?.message || 'Simulation could not run.';
        this.simulationBusy = false;
        this.simulationMode = '';
      }
    });
  }

  private chartLine(key: 'equityEur' | 'benchmarkEquityEur') {
    const points = this.simulation?.equityCurve || [];
    if (points.length < 2) return '';
    const values = points.flatMap(point => [point.equityEur, point.benchmarkEquityEur]);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = Math.max(1, max - min);
    return points.map((point, index) => {
      const x = 12 + index * 976 / (points.length - 1);
      const y = 228 - ((point[key] - min) / range) * 216;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  private defaultStartDate() {
    const date = new Date();
    date.setUTCDate(date.getUTCDate() - 90);
    return date.toISOString().slice(0, 10);
  }
}

bootstrapApplication(AppComponent, { providers: [provideHttpClient()] }).catch(console.error);
