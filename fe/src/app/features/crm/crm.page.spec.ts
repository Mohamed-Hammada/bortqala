import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CrmPage } from './crm.page';
import { CrmSummary, CrmLead, CrmConversation } from './crm.models';

describe('CrmPage', () => {
  let component: CrmPage;
  let fixture: ComponentFixture<CrmPage>;
  let httpMock: HttpTestingController;

  const mockSummary: CrmSummary = {
    totalPipelineValue: 150000,
    activeDeals: 3,
    winRate: 66.7,
    unreadMessages: 2,
    stages: [
      { status: 'NEW', count: 1, totalValue: 20000 },
      { status: 'CONTACTED', count: 1, totalValue: 30000 },
      { status: 'QUALIFIED', count: 1, totalValue: 50000 },
      { status: 'PROPOSAL_SENT', count: 0, totalValue: 0 },
      { status: 'NEGOTIATION', count: 0, totalValue: 0 },
      { status: 'WON', count: 2, totalValue: 100000 },
      { status: 'LOST', count: 1, totalValue: 10000 },
    ],
  };

  const mockLeads: CrmLead[] = [
    {
      id: 'lead-1',
      leadCode: 'LEAD-2026-001',
      name: 'Arab Construction Co',
      phone: '01011223344',
      source: 'WHATSAPP',
      status: 'QUALIFIED',
      estimatedValue: 50000,
      createdAt: 1724000000000,
      updatedAt: 1724000000000,
    },
  ];

  const mockConversations: CrmConversation[] = [
    {
      id: 'conv-1',
      channelType: 'WHATSAPP',
      externalSenderId: '01011223344',
      senderName: 'Arab Construction Co',
      leadId: 'lead-1',
      status: 'OPEN',
      lastMessagePreview: 'Inquiry about pricing',
      unreadCount: 1,
      createdAt: 1724000000000,
      updatedAt: 1724000000000,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, CrmPage],
    }).compileComponents();

    fixture = TestBed.createComponent(CrmPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('initializes and loads summary, leads, conversations, and channels', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/v1/crm/summary').flush(mockSummary);
    httpMock.expectOne('/api/v1/crm/leads').flush(mockLeads);
    httpMock.expectOne('/api/v1/crm/conversations').flush(mockConversations);
    httpMock.expectOne('/api/v1/crm/channels').flush([]);

    expect(component.summary()?.totalPipelineValue).toBe(150000);
    expect(component.leads().length).toBe(1);
    expect(component.conversations().length).toBe(1);
  });

  it('filters leads by pipeline stage correctly', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/v1/crm/summary').flush(mockSummary);
    httpMock.expectOne('/api/v1/crm/leads').flush(mockLeads);
    httpMock.expectOne('/api/v1/crm/conversations').flush(mockConversations);
    httpMock.expectOne('/api/v1/crm/channels').flush([]);

    const qualified = component.getLeadsInStage('QUALIFIED');
    expect(qualified.length).toBe(1);

    const won = component.getLeadsInStage('WON');
    expect(won.length).toBe(0);
  });

  it('selects conversation and loads messages', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/v1/crm/summary').flush(mockSummary);
    httpMock.expectOne('/api/v1/crm/leads').flush(mockLeads);
    httpMock.expectOne('/api/v1/crm/conversations').flush(mockConversations);
    httpMock.expectOne('/api/v1/crm/channels').flush([]);

    component.selectConversation(mockConversations[0]);
    const req = httpMock.expectOne('/api/v1/crm/conversations/conv-1/messages');
    req.flush([
      {
        id: 'msg-1',
        conversationId: 'conv-1',
        direction: 'INBOUND',
        messageText: 'Inquiry about pricing',
        isBotResponse: false,
        createdAt: 1724000000000,
      },
    ]);

    expect(component.activeConversation()?.id).toBe('conv-1');
    expect(component.messages().length).toBe(1);
  });

  it('submits outbound message in active conversation', () => {
    fixture.detectChanges();

    httpMock.expectOne('/api/v1/crm/summary').flush(mockSummary);
    httpMock.expectOne('/api/v1/crm/leads').flush(mockLeads);
    httpMock.expectOne('/api/v1/crm/conversations').flush(mockConversations);
    httpMock.expectOne('/api/v1/crm/channels').flush([]);

    component.selectConversation(mockConversations[0]);
    httpMock.expectOne('/api/v1/crm/conversations/conv-1/messages').flush([]);

    component.replyMessageText = 'Thank you for contacting us!';
    component.submitReply();

    const sendReq = httpMock.expectOne('/api/v1/crm/conversations/conv-1/messages');
    expect(sendReq.request.method).toBe('POST');
    sendReq.flush({
      id: 'msg-2',
      conversationId: 'conv-1',
      direction: 'OUTBOUND',
      messageText: 'Thank you for contacting us!',
      isBotResponse: false,
      createdAt: 1724000001000,
    });

    httpMock.expectOne('/api/v1/crm/conversations').flush(mockConversations);

    expect(component.replyMessageText).toBe('');
    expect(component.messages().length).toBe(1);
  });
});
