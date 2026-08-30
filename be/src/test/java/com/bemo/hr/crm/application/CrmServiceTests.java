package com.bemo.hr.crm.application;

import com.bemo.hr.crm.api.CrmApi;
import com.bemo.hr.crm.domain.*;
import com.bemo.hr.crm.infrastructure.*;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmServiceTests {

    @Mock
    private CrmLeadRepository leadRepository;

    @Mock
    private CrmActivityRepository activityRepository;

    @Mock
    private CrmChannelConfigRepository channelConfigRepository;

    @Mock
    private CrmConversationRepository conversationRepository;

    @Mock
    private CrmMessageRepository messageRepository;

    @Mock
    private BusinessPartyRepository businessPartyRepository;

    private CrmService crmService;

    @BeforeEach
    void setUp() {
        crmService = new CrmService(
                leadRepository,
                activityRepository,
                channelConfigRepository,
                conversationRepository,
                messageRepository,
                businessPartyRepository
        );
    }

    @Test
    void saveLead_createsNewLeadWithGeneratedCode() {
        when(leadRepository.count()).thenReturn(5L);
        when(leadRepository.save(any(CrmLead.class))).thenAnswer(inv -> inv.getArgument(0));

        CrmApi.SaveLeadRequest request = new CrmApi.SaveLeadRequest(
                null,
                "Acme Contracting",
                "01012345678",
                "info@acme.com",
                "Acme Corp",
                CrmLeadSource.WHATSAPP,
                CrmLeadStatus.NEW,
                BigDecimal.valueOf(50000),
                "agent-1",
                "Interested in enterprise license"
        );

        CrmApi.LeadResponse response = crmService.saveLead(request);

        assertThat(response.name()).isEqualTo("Acme Contracting");
        assertThat(response.leadCode()).contains("LEAD-");
        assertThat(response.estimatedValue()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(response.status()).isEqualTo(CrmLeadStatus.NEW);
        verify(leadRepository).save(any(CrmLead.class));
    }

    @Test
    void convertLeadToCustomer_createsBusinessPartyAndMarksLeadWon() {
        CrmLead lead = new CrmLead(
                "LEAD-2026-001",
                "Modern Build Ltd",
                "01099887766",
                "sales@modern.com",
                "Modern Build",
                CrmLeadSource.WEBSITE,
                CrmLeadStatus.PROPOSAL_SENT,
                BigDecimal.valueOf(120000),
                "agent-2",
                "Conversion test"
        );

        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(businessPartyRepository.findByCodeIgnoreCase("CUST-LEAD-2026-001")).thenReturn(Optional.empty());
        when(businessPartyRepository.save(any(BusinessParty.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leadRepository.save(any(CrmLead.class))).thenAnswer(inv -> inv.getArgument(0));

        CrmApi.LeadResponse response = crmService.convertLeadToCustomer(lead.getId());

        assertThat(response.status()).isEqualTo(CrmLeadStatus.WON);
        assertThat(response.businessPartyId()).isNotNull();
        verify(businessPartyRepository).save(any(BusinessParty.class));
        verify(leadRepository).save(lead);
    }

    @Test
    void createAndCompleteActivity() {
        CrmActivity activity = new CrmActivity(
                "lead-100",
                CrmActivityType.CALL,
                "Discovery call",
                "Discussed requirements",
                System.currentTimeMillis() + 86400000L,
                "agent-1"
        );

        when(activityRepository.save(any(CrmActivity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));

        CrmApi.CreateActivityRequest createReq = new CrmApi.CreateActivityRequest(
                "lead-100",
                CrmActivityType.CALL,
                "Discovery call",
                "Discussed requirements",
                System.currentTimeMillis() + 86400000L,
                "agent-1"
        );

        CrmApi.ActivityResponse created = crmService.createActivity(createReq);
        assertThat(created.summary()).isEqualTo("Discovery call");

        CrmApi.ActivityResponse completed = crmService.completeActivity(activity.getId());
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void getSummary_aggregatesPipelineCorrectly() {
        CrmLead lead1 = new CrmLead("L1", "Lead 1", "0101", "l1@bemo.com", "Co1",
                CrmLeadSource.WEBSITE, CrmLeadStatus.NEW, BigDecimal.valueOf(10000), null, null);
        CrmLead lead2 = new CrmLead("L2", "Lead 2", "0102", "l2@bemo.com", "Co2",
                CrmLeadSource.WHATSAPP, CrmLeadStatus.WON, BigDecimal.valueOf(30000), null, null);
        CrmLead lead3 = new CrmLead("L3", "Lead 3", "0103", "l3@bemo.com", "Co3",
                CrmLeadSource.FACEBOOK, CrmLeadStatus.QUALIFIED, BigDecimal.valueOf(20000), null, null);

        when(leadRepository.findAll()).thenReturn(List.of(lead1, lead2, lead3));
        when(conversationRepository.sumUnreadCount()).thenReturn(4L);

        CrmApi.CrmSummaryResponse summary = crmService.getSummary();

        assertThat(summary.totalPipelineValue()).isEqualByComparingTo(BigDecimal.valueOf(30000)); // lead1 + lead3
        assertThat(summary.activeDeals()).isEqualTo(2); // lead1, lead3
        assertThat(summary.unreadMessages()).isEqualTo(4L);
    }

    @Test
    void handleInboundWebhook_withChatbotAutoReply() {
        CrmChannelConfig channel = new CrmChannelConfig(
                CrmChannelType.WHATSAPP,
                "Official WhatsApp",
                "phone-12345",
                "••••1234",
                "secret",
                true,
                "Welcome to Bemo!",
                true
        );

        when(conversationRepository.findFirstByChannelTypeAndExternalSenderId(CrmChannelType.WHATSAPP, "01012345678"))
                .thenReturn(Optional.empty());
        when(leadRepository.findByPhone("01012345678")).thenReturn(Optional.empty());
        when(leadRepository.save(any(CrmLead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any(CrmConversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any(CrmMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(channelConfigRepository.findFirstByChannelTypeAndActiveTrue(CrmChannelType.WHATSAPP))
                .thenReturn(Optional.of(channel));

        CrmApi.InboundWebhookRequest webhookReq = new CrmApi.InboundWebhookRequest(
                CrmChannelType.WHATSAPP,
                "01012345678",
                "Ahmed Hassan",
                "Hello, I need pricing details",
                null
        );

        CrmApi.MessageResponse inboundResponse = crmService.handleInboundWebhook(webhookReq);

        assertThat(inboundResponse.direction()).isEqualTo(CrmMessageDirection.INBOUND);
        assertThat(inboundResponse.messageText()).isEqualTo("Hello, I need pricing details");
        verify(messageRepository, atLeast(2)).save(any(CrmMessage.class)); // 1 inbound + 1 bot reply
    }
}
