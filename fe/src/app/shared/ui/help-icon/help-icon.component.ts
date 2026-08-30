import { ChangeDetectionStrategy, Component, inject, Input, signal } from '@angular/core';
import { I18nService } from '../../../core/i18n.service';

@Component({
  selector: 'app-help-icon',
  standalone: true,
  template: `
    <span class="help-trigger" (click)="toggle()" [attr.aria-label]="i18n.t('help.title')" role="button" tabindex="0">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
        <path d="M8 1a7 7 0 100 14A7 7 0 008 1zm0 10.5a.75.75 0 110-1.5.75.75 0 010 1.5zM8.75 4.75a.75.75 0 00-1.5 0V8a.75.75 0 001.5 0V4.75z"/>
      </svg>
    </span>
    @if (open()) {
      <div class="help-tooltip">
        <p>{{ helpText() }}</p>
        <button class="help-close" (click)="open.set(false)">✕</button>
      </div>
    }
  `,
  styles: [`
    :host { position: relative; display: inline-block; }
    .help-trigger {
      cursor: pointer;
      color: var(--muted, #666);
      display: inline-flex;
      align-items: center;
      padding: 0.2rem;
      border-radius: 4px;
      transition: color 0.15s;
    }
    .help-trigger:hover { color: var(--gold, #c8a23c); }
    .help-tooltip {
      position: absolute;
      top: 100%;
      right: 0;
      margin-top: 0.5rem;
      width: 320px;
      padding: 1rem;
      background: var(--surface, #fff);
      border: 1px solid var(--line, #e0e0e0);
      border-radius: 8px;
      box-shadow: 0 4px 16px rgba(0,0,0,0.12);
      z-index: 100;
      font-size: 0.85rem;
      line-height: 1.5;
    }
    .help-tooltip p {
      margin: 0 0 0.5rem;
      color: var(--text, #333);
    }
    .help-close {
      position: absolute;
      top: 0.5rem;
      left: 0.5rem;
      background: none;
      border: none;
      cursor: pointer;
      color: var(--muted, #666);
      font-size: 0.8rem;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HelpIconComponent {
  @Input() pageKey = '';

  readonly i18n = inject(I18nService);
  readonly open = signal(false);

  readonly helpText = () => {
    const key = `help.${this.pageKey}`;
    const text = this.i18n.t(key);
    return text === key ? this.i18n.t('help.title') : text;
  };

  toggle() {
    this.open.set(!this.open());
  }
}
