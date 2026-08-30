import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { HelpdeskService } from './helpdesk.service';
import { HelpdeskCategory, Ticket, TicketMessage } from './helpdesk.models';

@Component({
  selector: 'app-helpdesk-page',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './helpdesk.page.html',
  styleUrl: './helpdesk.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HelpdeskPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly service = inject(HelpdeskService);

  readonly loading = signal(true);
  readonly categories = signal<HelpdeskCategory[]>([]);
  readonly tickets = signal<Ticket[]>([]);
  readonly openCount = signal(0);
  readonly myOpenCount = signal(0);
  readonly selectedTicket = signal<Ticket | null>(null);
  readonly messages = signal<TicketMessage[]>([]);
  readonly showCreateCategory = signal(false);
  readonly showCreateTicket = signal(false);
  readonly showTicketDetail = signal(false);
  readonly filterStatus = signal('');
  readonly newMessage = signal('');
  readonly internalNote = signal(false);

  readonly catNameAr = signal('');
  readonly catNameEn = signal('');
  readonly catSlaFirstResp = signal(8);
  readonly catSlaResolution = signal(48);

  readonly ticketCategoryId = signal('');
  readonly ticketTitle = signal('');
  readonly ticketDescription = signal('');
  readonly ticketPriority = signal('NORMAL');

  ngOnInit() { this.loadAll(); }

  async loadAll() {
    this.loading.set(true);
    try {
      const [cats, ticketResp] = await Promise.all([
        this.service.listCategories(),
        this.service.listTickets(this.filterStatus() || undefined),
      ]);
      this.categories.set(cats);
      this.tickets.set(ticketResp.tickets);
      this.openCount.set(ticketResp.openCount);
      this.myOpenCount.set(ticketResp.myOpenCount);
    } catch {
      this.notification.error(this.i18n.t('common.loadError'));
    } finally {
      this.loading.set(false);
    }
  }

  async createCategory() {
    if (!this.catNameAr() || !this.catNameEn()) return;
    try {
      await this.service.createCategory({
        nameAr: this.catNameAr(), nameEn: this.catNameEn(),
        slaFirstResponseHours: this.catSlaFirstResp(), slaResolutionHours: this.catSlaResolution(),
      });
      this.showCreateCategory.set(false);
      this.catNameAr.set(''); this.catNameEn.set('');
      this.notification.success(this.i18n.t('helpdesk.ticketCreated'));
      await this.loadAll();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async createTicket() {
    if (!this.ticketCategoryId() || !this.ticketTitle()) return;
    try {
      await this.service.createTicket({
        categoryId: this.ticketCategoryId(), title: this.ticketTitle(),
        description: this.ticketDescription(), priority: this.ticketPriority(),
      });
      this.showCreateTicket.set(false);
      this.ticketTitle.set(''); this.ticketDescription.set('');
      this.notification.success(this.i18n.t('helpdesk.ticketCreated'));
      await this.loadAll();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async openTicket(ticket: Ticket) {
    this.selectedTicket.set(ticket);
    this.showTicketDetail.set(true);
    try {
      this.messages.set(await this.service.listMessages(ticket.id, true));
    } catch { this.messages.set([]); }
  }

  async sendMessage() {
    const t = this.selectedTicket();
    if (!t || !this.newMessage()) return;
    try {
      await this.service.addMessage(t.id, this.newMessage(), this.internalNote());
      this.newMessage.set('');
      this.internalNote.set(false);
      this.messages.set(await this.service.listMessages(t.id, true));
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  async transitionTo(status: string) {
    const t = this.selectedTicket();
    if (!t) return;
    try {
      await this.service.transitionTicket(t.id, status);
      this.selectedTicket.set({ ...t, status: status as Ticket['status'] });
      this.notification.success(this.i18n.t('helpdesk.ticketUpdated'));
      await this.loadAll();
    } catch { this.notification.error(this.i18n.t('common.error')); }
  }

  priorityColor(p: string): string {
    return ({ URGENT: 'var(--danger)', HIGH: 'var(--warning-text)', NORMAL: 'var(--gold)', LOW: 'var(--muted)' })[p] ?? 'var(--muted)';
  }

  statusColor(s: string): string {
    return ({ NEW: 'var(--gold)', OPEN: 'var(--success-soft)', WAITING_CUSTOMER: 'var(--warning-soft)', RESOLVED: 'var(--success)', CLOSED: 'var(--muted)' })[s] ?? 'var(--muted)';
  }

  isSlaBreach(t: Ticket): boolean { return t.slaBreachFirstResponse || t.slaBreachResolution; }
  categoryName(id: string): string {
    const c = this.categories().find(x => x.id === id);
    return c ? (this.i18n.locale() === 'ar-EG' ? c.nameAr : c.nameEn) : id;
  }
}
