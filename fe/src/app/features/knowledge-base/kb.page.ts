import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { HelpdeskService } from '../helpdesk/helpdesk.service';
import { KbArticle } from '../helpdesk/helpdesk.models';

@Component({
  selector: 'app-kb-page',
  standalone: true,
  imports: [],
  templateUrl: './kb.page.html',
  styleUrl: './kb.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KbPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly service = inject(HelpdeskService);

  readonly loading = signal(true);
  readonly articles = signal<KbArticle[]>([]);
  readonly searchQuery = signal('');
  readonly showCreate = signal(false);
  readonly viewingArticle = signal<KbArticle | null>(null);

  readonly newTitleAr = signal('');
  readonly newTitleEn = signal('');
  readonly newBodyAr = signal('');
  readonly newBodyEn = signal('');
  readonly newTags = signal('');

  ngOnInit() { this.loadArticles(); }

  async loadArticles() {
    this.loading.set(true);
    try {
      this.articles.set(await this.service.listKbArticles(this.searchQuery() || undefined));
    } catch { this.notification.error(this.i18n.t('common.loadError')); }
    finally { this.loading.set(false); }
  }

  async search() { await this.loadArticles(); }

  async createArticle() {
    if (!this.newTitleAr() || !this.newTitleEn()) return;
    try {
      await this.service.createKbArticle({
        titleAr: this.newTitleAr(), titleEn: this.newTitleEn(),
        bodyAr: this.newBodyAr(), bodyEn: this.newBodyEn(), tags: this.newTags(),
      });
      this.showCreate.set(false);
      this.newTitleAr.set(''); this.newTitleEn.set('');
      this.newBodyAr.set(''); this.newBodyEn.set(''); this.newTags.set('');
      this.notification.success(this.i18n.t('kb.articleCreated'));
      await this.loadArticles();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async publishArticle(id: string) {
    try {
      await this.service.publishKbArticle(id);
      this.notification.success(this.i18n.t('kb.articlePublished'));
      await this.loadArticles();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async vote(id: string, up: boolean) {
    try {
      await this.service.voteKbArticle(id, up);
      this.notification.success(this.i18n.t('kb.voted'));
      await this.loadArticles();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  viewArticle(a: KbArticle) { this.viewingArticle.set(a); }
}
