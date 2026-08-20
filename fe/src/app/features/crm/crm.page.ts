import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import {
  CrmActivity,
  CrmActivityType,
  CrmChannelConfig,
  CrmChannelType,
  CrmConversation,
  CrmLead,
  CrmLeadSource,
  CrmLeadStatus,
  CrmMessage,
  CrmSummary,
  SaveChannelConfigRequest,
  SaveLeadRequest,
} from './crm.models';
import { CrmService } from './crm.service';

@Component({
  selector: 'app-crm-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './crm.page.html',
  styleUrls: ['./crm.page.scss'],
})
export class CrmPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly crmService = inject(CrmService);
  private readonly fb = inject(FormBuilder);

  readonly activeTab = signal<'pipeline' | 'leads' | 'inbox' | 'channels'>('pipeline');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly sendingMessage = signal<boolean>(false);

  readonly summary = signal<CrmSummary | null>(null);
  readonly leads = signal<CrmLead[]>([]);
  readonly conversations = signal<CrmConversation[]>([]);
  readonly activeConversation = signal<CrmConversation | null>(null);
  readonly messages = signal<CrmMessage[]>([]);
  readonly channels = signal<CrmChannelConfig[]>([]);

  readonly selectedLead = signal<CrmLead | null>(null);
  readonly leadActivities = signal<CrmActivity[]>([]);

  readonly showLeadModal = signal<boolean>(false);
  readonly showActivityModal = signal<boolean>(false);
  readonly showChannelModal = signal<boolean>(false);

  readonly leadStatuses: CrmLeadStatus[] = [
    'NEW',
    'CONTACTED',
    'QUALIFIED',
    'PROPOSAL_SENT',
    'NEGOTIATION',
    'WON',
    'LOST',
  ];

  readonly leadSources: CrmLeadSource[] = [
    'WHATSAPP',
    'FACEBOOK',
    'WEBSITE',
    'REFERRAL',
    'DIRECT_CALL',
    'EXHIBITION',
    'OTHER',
  ];

  readonly activityTypes: CrmActivityType[] = [
    'CALL',
    'MEETING',
    'WHATSAPP_MESSAGE',
    'EMAIL',
    'NOTE',
    'TASK',
  ];

  readonly channelTypes: CrmChannelType[] = [
    'WHATSAPP',
    'FACEBOOK_MESSENGER',
    'INSTAGRAM',
    'WEB_CHATBOT',
  ];

  leadForm!: FormGroup;
  activityForm!: FormGroup;
  channelForm!: FormGroup;
  replyMessageText = '';

  ngOnInit(): void {
    this.initForms();
    this.loadData();
  }

  private initForms(): void {
    this.leadForm = this.fb.group({
      id: [''],
      name: ['', Validators.required],
      phone: [''],
      email: [''],
      companyName: [''],
      source: ['WEBSITE', Validators.required],
      status: ['NEW', Validators.required],
      estimatedValue: [0, [Validators.required, Validators.min(0)]],
      assignedSalesAgentId: [''],
      notes: [''],
    });

    this.activityForm = this.fb.group({
      leadId: ['', Validators.required],
      activityType: ['CALL', Validators.required],
      summary: ['', Validators.required],
      details: [''],
      dueDate: [''],
      assignedToUserId: [''],
    });

    this.channelForm = this.fb.group({
      id: [''],
      channelType: ['WHATSAPP', Validators.required],
      channelName: ['', Validators.required],
      accountIdentifier: [''],
      apiToken: [''],
      webhookSecret: [''],
      botEnabled: [true],
      autoReplyGreeting: [''],
      active: [true],
    });
  }

  loadData(): void {
    this.loading.set(true);
    this.crmService.getSummary().subscribe({
      next: (sum) => this.summary.set(sum),
      error: () => {},
    });

    this.crmService.listLeads().subscribe({
      next: (lds) => {
        this.leads.set(lds);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.crmService.listConversations().subscribe({
      next: (convs) => this.conversations.set(convs),
      error: () => {},
    });

    this.crmService.listChannelConfigs().subscribe({
      next: (chans) => this.channels.set(chans),
      error: () => {},
    });
  }

  getLeadsInStage(status: CrmLeadStatus): CrmLead[] {
    return this.leads().filter((l) => l.status === status);
  }

  getStageLabel(status: CrmLeadStatus): string {
    switch (status) {
      case 'NEW':
        return this.i18n.t('crm.stageNew');
      case 'CONTACTED':
        return this.i18n.t('crm.stageContacted');
      case 'QUALIFIED':
        return this.i18n.t('crm.stageQualified');
      case 'PROPOSAL_SENT':
        return this.i18n.t('crm.stageProposal');
      case 'NEGOTIATION':
        return this.i18n.t('crm.stageNegotiation');
      case 'WON':
        return this.i18n.t('crm.stageWon');
      case 'LOST':
        return this.i18n.t('crm.stageLost');
      default:
        return status;
    }
  }

  openLeadModal(lead?: CrmLead): void {
    if (lead) {
      this.leadForm.patchValue({
        id: lead.id,
        name: lead.name,
        phone: lead.phone || '',
        email: lead.email || '',
        companyName: lead.companyName || '',
        source: lead.source,
        status: lead.status,
        estimatedValue: lead.estimatedValue,
        assignedSalesAgentId: lead.assignedSalesAgentId || '',
        notes: lead.notes || '',
      });
    } else {
      this.leadForm.reset({
        id: '',
        name: '',
        phone: '',
        email: '',
        companyName: '',
        source: 'WEBSITE',
        status: 'NEW',
        estimatedValue: 0,
        assignedSalesAgentId: '',
        notes: '',
      });
    }
    this.showLeadModal.set(true);
  }

  submitLead(): void {
    if (this.leadForm.invalid) return;
    this.saving.set(true);
    const req: SaveLeadRequest = this.leadForm.value;
    this.crmService.saveLead(req).subscribe({
      next: () => {
        this.saving.set(false);
        this.showLeadModal.set(false);
        this.loadData();
      },
      error: () => this.saving.set(false),
    });
  }

  updateLeadStatus(lead: CrmLead, newStatus: CrmLeadStatus): void {
    const req: SaveLeadRequest = {
      id: lead.id,
      name: lead.name,
      phone: lead.phone,
      email: lead.email,
      companyName: lead.companyName,
      source: lead.source,
      status: newStatus,
      estimatedValue: lead.estimatedValue,
      assignedSalesAgentId: lead.assignedSalesAgentId,
      notes: lead.notes,
    };
    this.crmService.saveLead(req).subscribe({
      next: () => this.loadData(),
      error: () => {},
    });
  }

  convertToCustomer(lead: CrmLead): void {
    this.crmService.convertLeadToCustomer(lead.id).subscribe({
      next: () => this.loadData(),
      error: () => {},
    });
  }

  viewLeadTimeline(lead: CrmLead): void {
    this.selectedLead.set(lead);
    this.crmService.listActivities(lead.id).subscribe({
      next: (acts) => this.leadActivities.set(acts),
      error: () => {},
    });
  }

  openActivityModal(lead: CrmLead): void {
    this.activityForm.reset({
      leadId: lead.id,
      activityType: 'CALL',
      summary: '',
      details: '',
      dueDate: '',
      assignedToUserId: '',
    });
    this.showActivityModal.set(true);
  }

  submitActivity(): void {
    if (this.activityForm.invalid) return;
    this.saving.set(true);
    const formVal = this.activityForm.value;
    const dueTimestamp = formVal.dueDate ? new Date(formVal.dueDate).getTime() : undefined;
    this.crmService
      .createActivity({
        leadId: formVal.leadId,
        activityType: formVal.activityType,
        summary: formVal.summary,
        details: formVal.details,
        dueDate: dueTimestamp,
        assignedToUserId: formVal.assignedToUserId,
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this.showActivityModal.set(false);
          if (this.selectedLead()?.id === formVal.leadId) {
            this.leadActivities.update((list) => [created, ...list]);
          }
        },
        error: () => this.saving.set(false),
      });
  }

  completeActivity(activity: CrmActivity): void {
    this.crmService.completeActivity(activity.id).subscribe({
      next: (updated) => {
        this.leadActivities.update((list) =>
          list.map((a) => (a.id === updated.id ? updated : a))
        );
      },
      error: () => {},
    });
  }

  selectConversation(conv: CrmConversation): void {
    this.activeConversation.set(conv);
    this.crmService.getConversationMessages(conv.id).subscribe({
      next: (msgs) => this.messages.set(msgs),
      error: () => {},
    });
  }

  submitReply(): void {
    const conv = this.activeConversation();
    if (!conv || !this.replyMessageText.trim()) return;

    this.sendingMessage.set(true);
    this.crmService
      .sendMessage(conv.id, {
        conversationId: conv.id,
        messageText: this.replyMessageText.trim(),
      })
      .subscribe({
        next: (msg) => {
          this.sendingMessage.set(false);
          this.replyMessageText = '';
          this.messages.update((msgs) => [...msgs, msg]);
          this.crmService.listConversations().subscribe({
            next: (convs) => this.conversations.set(convs),
          });
        },
        error: () => this.sendingMessage.set(false),
      });
  }

  openChannelModal(cfg?: CrmChannelConfig): void {
    if (cfg) {
      this.channelForm.patchValue({
        id: cfg.id,
        channelType: cfg.channelType,
        channelName: cfg.channelName,
        accountIdentifier: cfg.accountIdentifier || '',
        apiToken: '',
        webhookSecret: cfg.webhookSecret || '',
        botEnabled: cfg.botEnabled,
        autoReplyGreeting: cfg.autoReplyGreeting || '',
        active: cfg.active,
      });
    } else {
      this.channelForm.reset({
        id: '',
        channelType: 'WHATSAPP',
        channelName: '',
        accountIdentifier: '',
        apiToken: '',
        webhookSecret: '',
        botEnabled: true,
        autoReplyGreeting: 'أهلاً بك! كيف يمكننا مساعدتك اليوم؟',
        active: true,
      });
    }
    this.showChannelModal.set(true);
  }

  submitChannel(): void {
    if (this.channelForm.invalid) return;
    this.saving.set(true);
    const req: SaveChannelConfigRequest = this.channelForm.value;
    this.crmService.saveChannelConfig(req).subscribe({
      next: () => {
        this.saving.set(false);
        this.showChannelModal.set(false);
        this.crmService.listChannelConfigs().subscribe({
          next: (chans) => this.channels.set(chans),
        });
      },
      error: () => this.saving.set(false),
    });
  }
}
