import { bootstrapApplication } from '@angular/platform-browser';
import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <main>
      <header><div><p class="eyebrow">PAPER TRADING</p><h1>Sari Autonomous Fund</h1></div><div class="status">● System healthy</div></header>
      <section class="grid metrics">
        <article><span>Portfolio</span><strong>€5,000.00</strong><small>Starting capital</small></article>
        <article><span>Total return</span><strong>0.00%</strong><small>BTC benchmark: —</small></article>
        <article><span>Max drawdown</span><strong>0.00%</strong><small>Hard stop: 15%</small></article>
        <article><span>Cash</span><strong>100%</strong><small>No live positions</small></article>
      </section>
      <section class="grid lower">
        <article><p class="eyebrow">MANDATE</p><h2>Bounded autonomy</h2><p>The AI may research and propose trades. Deterministic risk rules have final authority.</p><div class="rule"><b>Max position</b><span>10%</span></div><div class="rule"><b>Daily loss stop</b><span>2%</span></div><div class="rule"><b>Max drawdown</b><span>15%</span></div><div class="rule"><b>Leverage</b><span>Disabled</span></div></article>
        <article><p class="eyebrow">LATEST DECISIONS</p><h2>Decision ledger</h2><div class="empty">No decisions yet.<br><small>The first paper-trading proposal will appear here.</small></div></article>
      </section>
    </main>`
})
class AppComponent {}

bootstrapApplication(AppComponent).catch(console.error);
