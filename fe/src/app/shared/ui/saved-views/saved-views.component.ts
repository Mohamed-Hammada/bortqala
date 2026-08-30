import { ChangeDetectionStrategy, Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { I18nService } from '../../../core/i18n.service';
import { GridView } from '../../../features/settings/integrations.models';
import { GridViewStore } from '../../grid-view.store';

@Component({
  selector: 'app-saved-views',
  standalone: true,
  template: `
    <div class="saved-views-row">
      <button class="view-chip active" (click)="applyDefault()">
        {{ i18n.t('gridViews.default') }}
      </button>
      @for (view of store.views(); track view.id) {
        <button class="view-chip" [class.active]="activeViewId === view.id" (click)="applyView(view)">
          {{ view.name }}
          <span class="chip-remove" (click)="removeView(view); $event.stopPropagation()">✕</span>
        </button>
      }
      <button class="view-chip save-chip" (click)="showSave.set(!showSave())">
        {{ i18n.t('gridViews.save') }}
      </button>
      @if (showSave()) {
        <div class="save-form">
          <input
            type="text"
            [placeholder]="i18n.t('gridViews.viewName')"
            [value]="newName()"
            (input)="newName.set($any($event.target).value)"
            (keydown.enter)="saveCurrentView()"
          />
          <input
            type="text"
            [placeholder]="i18n.t('gridViews.shareWith')"
            [value]="newSharedRoles()"
            (input)="newSharedRoles.set($any($event.target).value)"
          />
          <button class="btn-save" (click)="saveCurrentView()" [disabled]="!newName().trim()">
            {{ i18n.t('common.save') }}
          </button>
        </div>
      }
    </div>
  `,
  styleUrl: './saved-views.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavedViewsComponent implements OnInit {
  @Input() pageKey = '';
  @Input() activeViewId: string | null = null;
  @Input() getCurrentFilters: () => string = () => '[]';
  @Input() getCurrentHiddenColumns: () => string = () => '';
  @Input() getCurrentSort: () => string = () => '';
  @Output() viewApplied = new EventEmitter<GridView>();

  readonly i18n = inject(I18nService);
  readonly store = inject(GridViewStore);
  readonly showSave = signal(false);
  readonly newName = signal('');
  readonly newSharedRoles = signal('');

  ngOnInit(): void {
    if (this.pageKey) {
      this.store.load(this.pageKey);
    }
  }

  applyDefault() {
    this.viewApplied.emit(null as unknown as GridView);
  }

  applyView(view: GridView) {
    this.viewApplied.emit(view);
  }

  async saveCurrentView() {
    const name = this.newName().trim();
    if (!name) return;
    const result = await this.store.save(this.pageKey, {
      pageKey: this.pageKey,
      name,
      filters: this.getCurrentFilters(),
      hiddenColumns: this.getCurrentHiddenColumns(),
      sort: this.getCurrentSort(),
      sharedRoles: this.newSharedRoles().trim() || undefined,
    });
    if (result) {
      this.showSave.set(false);
      this.newName.set('');
      this.newSharedRoles.set('');
    }
  }

  async removeView(view: GridView) {
    await this.store.remove(view.id, this.pageKey);
  }
}
