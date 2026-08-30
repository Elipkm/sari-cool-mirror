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

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main>
      <header>
        <div><p class="eyebrow">WEEKLY REVIEW · PAPER TRADING</p><h1>Sari Autonomous Fund</h1></div>
        <div class="status" [class.warn]="review?.systemState !== 'NORMAL'">● {{ review?.systemState || 'Loading' }}</div>
      </header>

      <div *ngIf="error" class="error">Dashboard API unavailable. Start the backend and refresh.</div>

      <section class="grid metrics">
        <article><span>Portfolio</span><strong>{{ review?.equityEur | currency:'EUR' }}</strong><small>Return {{ signed(review?.returnPct) }}%</small></article>
        <article><span>This week</span><strong>{{ signed(review?.weeklyReturnPct) }}%</strong><small>vs weekly equity baseline</small></article>
        <article><span>Max drawdown</span><strong>{{ review?.maxDrawdownPct ?? 0 | number:'1.2-2' }}%</strong><small>Hard stop: 15%</small></article>
        <article><span>Cash</span><strong>{{ review?.cashEur | currency:'EUR' }}</strong><small>{{ positionCount }} open position(s)</small></article>
      </section>

      <section class="grid lower">
        <article>
          <p class="eyebrow">OPEN POSITIONS</p><h2>Capital at work</h2>
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
  error = false;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<WeeklyReview>('/api/review/weekly').subscribe({
      next: review => this.review = review,
      error: () => this.error = true
    });
  }

  get positions() { return Object.entries(this.review?.positionsEur || {}).map(([key, value]) => ({ key, value })); }
  get positionCount() { return this.positions.filter(p => p.value > 0).length; }
  signed(value?: number) { const n = value ?? 0; return `${n > 0 ? '+' : ''}${n.toFixed(2)}`; }
}

bootstrapApplication(AppComponent, { providers: [provideHttpClient()] }).catch(console.error);
