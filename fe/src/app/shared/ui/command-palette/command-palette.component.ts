import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, HostListener, inject, Input, OnDestroy, OnInit, Output, signal, viewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { SearchResultItem } from '../../../features/settings/integrations.models';

export interface PaletteAction {
  type: 'navigation' | 'action';
  title: string;
  subtitle?: string;
  url?: string;
  icon?: string;
}

@Component({
  selector: 'app-command-palette',
  standalone: true,
  templateUrl: './command-palette.component.html',
  styleUrl: './command-palette.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommandPaletteComponent implements OnInit, OnDestroy {
  @Input() open = false;
  @Output() closed = new EventEmitter<void>();

  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly query = signal('');
  readonly selectedIndex = signal(0);
  readonly results = signal<PaletteAction[]>([]);
  readonly loading = signal(false);
  readonly inputRef = viewChild<ElementRef<HTMLInputElement>>('searchInput');

  private staticActions: PaletteAction[] = [];

  ngOnInit(): void {
    this.buildStaticActions();
  }

  ngOnDestroy(): void {
    // cleanup handled by parent
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(e: KeyboardEvent): void {
    if (!this.open) return;
    if (e.key === 'Escape') {
      e.preventDefault();
      this.close();
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      this.selectedIndex.update(i => Math.min(i + 1, this.results().length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      this.selectedIndex.update(i => Math.max(i - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const item = this.results()[this.selectedIndex()];
      if (item) this.executeItem(item);
    }
  }

  async onQueryChange(value: string) {
    this.query.set(value);
    this.selectedIndex.set(0);

    if (!value.trim()) {
      this.results.set(this.staticActions);
      return;
    }

    const q = value.toLowerCase().trim();
    const matchedStatic = this.staticActions.filter(a =>
      a.title.toLowerCase().includes(q) || (a.subtitle && a.subtitle.toLowerCase().includes(q))
    );

    if (q.length >= 2) {
      this.loading.set(true);
      try {
        const response = await firstValueFrom(
          this.http.get<{ results: SearchResultItem[] }>(`/api/v1/platform/search?q=${encodeURIComponent(q)}`),
        );
        const dynamicResults: PaletteAction[] = response.results.map(r => ({
          type: 'navigation' as const,
          title: r.title,
          subtitle: r.subtitle,
          url: r.url,
          icon: this.iconForType(r.type),
        }));
        this.results.set([...matchedStatic, ...dynamicResults].slice(0, 20));
      } catch {
        this.results.set(matchedStatic);
      } finally {
        this.loading.set(false);
      }
    } else {
      this.results.set(matchedStatic.slice(0, 20));
    }
  }

  executeItem(item: PaletteAction) {
    if (item.url) {
      this.router.navigateByUrl(item.url);
    }
    this.close();
  }

  close() {
    this.query.set('');
    this.results.set([]);
    this.closed.emit();
  }

  focusInput() {
    setTimeout(() => {
      this.inputRef()?.nativeElement?.focus();
    }, 50);
  }

  private buildStaticActions() {
    const navItems: PaletteAction[] = [
      { type: 'navigation', title: 'Dashboard', subtitle: this.i18n.t('nav.dashboard'), url: '/dashboard', icon: '📊' },
      { type: 'navigation', title: 'Employees', subtitle: this.i18n.t('nav.employees'), url: '/employees', icon: '👥' },
      { type: 'navigation', title: 'Reports', subtitle: this.i18n.t('nav.reports'), url: '/reports', icon: '📋' },
      { type: 'navigation', title: 'Payroll', subtitle: this.i18n.t('nav.payroll'), url: '/payroll', icon: '💰' },
      { type: 'navigation', title: 'Parties', subtitle: this.i18n.t('nav.parties'), url: '/parties', icon: '🏢' },
      { type: 'navigation', title: 'Operations', subtitle: this.i18n.t('nav.operations'), url: '/operations', icon: '📦' },
      { type: 'navigation', title: 'Categories', subtitle: this.i18n.t('nav.categories'), url: '/categories', icon: '🏷' },
      { type: 'navigation', title: 'Imports', subtitle: this.i18n.t('nav.imports'), url: '/imports', icon: '📥' },
      { type: 'navigation', title: 'Settings', subtitle: this.i18n.t('nav.settingsHint'), url: '/settings', icon: '⚙' },
    ];
    this.staticActions = navItems;
    this.results.set(navItems);
  }

  private iconForType(type: string): string {
    switch (type) {
      case 'employee': return '👤';
      case 'customer': return '🤝';
      case 'supplier': return '🏭';
      case 'invoice': return '📄';
      case 'purchase_order': return '📑';
      case 'project': return '🏗';
      case 'journal': return '⚖️';
      case 'payment': return '💳';
      case 'product': return '📦';
      default: return '🔍';
    }
  }
}
