package com.bemo.hr.crm.application;

import com.bemo.hr.crm.api.CrmApi;
import com.bemo.hr.crm.domain.*;
import com.bemo.hr.crm.infrastructure.*;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CrmService {

    private final CrmLeadRepository leadRepository;
    private final CrmActivityRepository activityRepository;
    private final CrmChannelConfigRepository channelConfigRepository;
    private final CrmConversationRepository conversationRepository;
    private final CrmMessageRepository messageRepository;
    private final BusinessPartyRepository businessPartyRepository;

    public CrmService(CrmLeadRepository leadRepository,
                      CrmActivityRepository activityRepository,
                      CrmChannelConfigRepository channelConfigRepository,
                      CrmConversationRepository conversationRepository,
                      CrmMessageRepository messageRepository,
                      BusinessPartyRepository businessPartyRepository) {
        this.leadRepository = leadRepository;
        this.activityRepository = activityRepository;
        this.channelConfigRepository = channelConfigRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.businessPartyRepository = businessPartyRepository;
    }

    @Transactional(readOnly = true)
    public List<CrmApi.LeadResponse> listLeads(CrmLeadStatus status) {
        List<CrmLead> list = status != null
                ? leadRepository.findByStatusOrderByCreatedAtDesc(status)
                : leadRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toLeadResponse).toList();
    }

    public CrmApi.LeadResponse saveLead(CrmApi.SaveLeadRequest request) {
        if (request.id() != null && !request.id().isBlank()) {
            CrmLead lead = leadRepository.findById(request.id())
                    .orElseThrow(() -> new NotFoundException("Lead not found: " + request.id()));
            lead.update(
                    request.name(),
                    request.phone(),
                    request.email(),
                    request.companyName(),
                    request.source(),
                    request.status(),
                    request.estimatedValue(),
                    request.assignedSalesAgentId(),
                    request.notes()
            );
            return toLeadResponse(leadRepository.save(lead));
        }

        int year = LocalDate.now().getYear();
        long count = leadRepository.count() + 1;
        String leadCode = String.format("LEAD-%d-%03d", year, count);

        CrmLead lead = new CrmLead(
                leadCode,
                request.name(),
                request.phone(),
                request.email(),
                request.companyName(),
                request.source(),
                request.status() != null ? request.status() : CrmLeadStatus.NEW,
                request.estimatedValue(),
                request.assignedSalesAgentId(),
                request.notes()
        );
        return toLeadResponse(leadRepository.save(lead));
    }

    public CrmApi.LeadResponse convertLeadToCustomer(String leadId) {
        CrmLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));

        if (lead.getBusinessPartyId() != null) {
            return toLeadResponse(lead);
        }

        // Avoid duplicate master: check if party exists with matching code or name
        String partyCode = "CUST-" + lead.getLeadCode();
        Optional<BusinessParty> existing = businessPartyRepository.findByCodeIgnoreCase(partyCode);
        BusinessParty party;
        if (existing.isPresent()) {
            party = existing.get();
        } else {
            party = new BusinessParty(
                    partyCode,
                    lead.getName(),
                    lead.getName(),
                    "CUSTOMER",
                    lead.getName(),
                    lead.getPhone(),
                    lead.getEmail(),
                    lead.getCompanyName(),
                    lead.getNotes(),
                    true,
                    "DIRECT",
                    null,
                    LocalDate.now().toString(),
                    null,
                    "EGP",
                    "STANDARD",
                    "IMMEDIATE",
                    null,
                    null
            );
            party = businessPartyRepository.save(party);
        }

        lead.convertToCustomer(party.getId());
        return toLeadResponse(leadRepository.save(lead));
    }

    @Transactional(readOnly = true)
    public List<CrmApi.ActivityResponse> listActivities(String leadId) {
        return activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId)
                .stream().map(this::toActivityResponse).toList();
    }

    public CrmApi.ActivityResponse createActivity(CrmApi.CreateActivityRequest request) {
        CrmActivity activity = new CrmActivity(
                request.leadId(),
                request.activityType(),
                request.summary(),
                request.details(),
                request.dueDate(),
                request.assignedToUserId()
        );
        return toActivityResponse(activityRepository.save(activity));
    }

    public CrmApi.ActivityResponse completeActivity(String activityId) {
        CrmActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found: " + activityId));
        activity.complete();
        return toActivityResponse(activityRepository.save(activity));
    }

    @Transactional(readOnly = true)
    public CrmApi.CrmSummaryResponse getSummary() {
        List<CrmLead> allLeads = leadRepository.findAll();
        BigDecimal totalPipelineValue = BigDecimal.ZERO;
        long wonCount = 0;
        long lostCount = 0;
        long activeCount = 0;

        Map<CrmLeadStatus, List<CrmLead>> grouped = allLeads.stream()
                .collect(Collectors.groupingBy(CrmLead::getStatus));

        List<CrmApi.PipelineStageSummary> stages = new ArrayList<>();
        for (CrmLeadStatus st : CrmLeadStatus.values()) {
            List<CrmLead> inStage = grouped.getOrDefault(st, List.of());
            long count = inStage.size();
            BigDecimal sum = inStage.stream()
                    .map(CrmLead::getEstimatedValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            stages.add(new CrmApi.PipelineStageSummary(st, count, sum));
            if (st != CrmLeadStatus.WON && st != CrmLeadStatus.LOST) {
                totalPipelineValue = totalPipelineValue.add(sum);
                activeCount += count;
            } else if (st == CrmLeadStatus.WON) {
                wonCount += count;
            } else if (st == CrmLeadStatus.LOST) {
                lostCount += count;
            }
        }

        double totalClosed = wonCount + lostCount;
        double winRate = totalClosed > 0 ? (wonCount / totalClosed) * 100.0 : 0.0;
        long unreadCount = conversationRepository.sumUnreadCount();

        return new CrmApi.CrmSummaryResponse(
                totalPipelineValue,
                activeCount,
                Math.round(winRate * 10.0) / 10.0,
                unreadCount,
                stages
        );
    }

    @Transactional(readOnly = true)
    public List<CrmApi.ChannelConfigResponse> listChannelConfigs() {
        return channelConfigRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toChannelConfigResponse).toList();
    }

    public CrmApi.ChannelConfigResponse saveChannelConfig(CrmApi.SaveChannelConfigRequest request) {
        if (request.id() != null && !request.id().isBlank()) {
            CrmChannelConfig config = channelConfigRepository.findById(request.id())
                    .orElseThrow(() -> new NotFoundException("Channel config not found: " + request.id()));
            config.update(
                    request.channelName(),
                    request.accountIdentifier(),
                    request.apiToken(),
                    request.webhookSecret(),
                    request.botEnabled(),
                    request.autoReplyGreeting(),
                    request.active()
            );
            return toChannelConfigResponse(channelConfigRepository.save(config));
        }

        String maskedToken = request.apiToken() != null && request.apiToken().length() > 6
                ? "••••" + request.apiToken().substring(request.apiToken().length() - 4)
                : request.apiToken();

        CrmChannelConfig config = new CrmChannelConfig(
                request.channelType(),
                request.channelName(),
                request.accountIdentifier(),
                maskedToken,
                request.webhookSecret(),
                request.botEnabled(),
                request.autoReplyGreeting(),
                request.active()
        );
        return toChannelConfigResponse(channelConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<CrmApi.ConversationResponse> listConversations() {
        return conversationRepository.findAllByOrderByUpdatedAtDesc()
                .stream().map(this::toConversationResponse).toList();
    }

    public List<CrmApi.MessageResponse> getConversationMessages(String conversationId) {
        CrmConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
        conv.resetUnread();
        conversationRepository.save(conv);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().map(this::toMessageResponse).toList();
    }

    public CrmApi.MessageResponse sendMessage(String conversationId, CrmApi.SendMessageRequest request) {
        CrmConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));

        CrmMessage message = new CrmMessage(
                conversationId,
                CrmMessageDirection.OUTBOUND,
                "AGENT",
                "Sales Agent",
                request.messageText(),
                request.attachmentUrl(),
                "DELIVERED",
                false
        );
        CrmMessage saved = messageRepository.save(message);

        conv.recordOutboundMessage(request.messageText());
        conversationRepository.save(conv);

        return toMessageResponse(saved);
    }

    public CrmApi.MessageResponse handleInboundWebhook(CrmApi.InboundWebhookRequest request) {
        Optional<CrmConversation> convOpt = conversationRepository
                .findFirstByChannelTypeAndExternalSenderId(request.channelType(), request.senderId());

        CrmConversation conversation;
        if (convOpt.isPresent()) {
            conversation = convOpt.get();
        } else {
            // Find or create a CRM Lead for this contact
            Optional<CrmLead> leadOpt = leadRepository.findByPhone(request.senderId());
            String leadId = leadOpt.map(CrmLead::getId).orElseGet(() -> {
                CrmLead newLead = new CrmLead(
                        "LEAD-" + LocalDate.now().getYear() + "-" + (leadRepository.count() + 1),
                        request.senderName() != null ? request.senderName() : "Inbound Contact",
                        request.senderId(),
                        null,
                        null,
                        request.channelType() == CrmChannelType.WHATSAPP ? CrmLeadSource.WHATSAPP : CrmLeadSource.FACEBOOK,
                        CrmLeadStatus.NEW,
                        BigDecimal.ZERO,
                        null,
                        "Created via omnichannel inbound conversation"
                );
                return leadRepository.save(newLead).getId();
            });

            conversation = new CrmConversation(
                    request.channelType(),
                    request.senderId(),
                    request.senderName(),
                    leadId
            );
            conversation = conversationRepository.save(conversation);
        }

        // Ingest inbound message
        CrmMessage inboundMsg = new CrmMessage(
                conversation.getId(),
                CrmMessageDirection.INBOUND,
                request.senderId(),
                request.senderName(),
                request.messageText(),
                request.attachmentUrl(),
                "RECEIVED",
                false
        );
        CrmMessage savedInbound = messageRepository.save(inboundMsg);

        // Check channel bot configuration
        Optional<CrmChannelConfig> channelCfg = channelConfigRepository
                .findFirstByChannelTypeAndActiveTrue(request.channelType());

        boolean botEnabled = channelCfg.map(CrmChannelConfig::isBotEnabled).orElse(false);
        conversation.recordInboundMessage(request.messageText(), botEnabled);
        conversationRepository.save(conversation);

        // Automated Chatbot Reply if enabled
        if (botEnabled) {
            String botGreeting = channelCfg.map(CrmChannelConfig::getAutoReplyGreeting)
                    .filter(g -> g != null && !g.isBlank())
                    .orElse("أهلاً بك في منصة بيمو! تم استلام رسالتك وسيتواصل معك أحد ممثلي المبيعات قريباً.");

            CrmMessage botReply = new CrmMessage(
                    conversation.getId(),
                    CrmMessageDirection.OUTBOUND,
                    "BOT",
                    "Bemo AI Assistant",
                    botGreeting,
                    null,
                    "DELIVERED",
                    true
            );
            messageRepository.save(botReply);
            conversation.recordOutboundMessage(botGreeting);
            conversation.resetUnread();
            conversationRepository.save(conversation);
        }

        return toMessageResponse(savedInbound);
    }

    private CrmApi.LeadResponse toLeadResponse(CrmLead lead) {
        return new CrmApi.LeadResponse(
                lead.getId(),
                lead.getLeadCode(),
                lead.getName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getCompanyName(),
                lead.getSource(),
                lead.getStatus(),
                lead.getEstimatedValue(),
                lead.getAssignedSalesAgentId(),
                lead.getBusinessPartyId(),
                lead.getNotes(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    private CrmApi.ActivityResponse toActivityResponse(CrmActivity a) {
        return new CrmApi.ActivityResponse(
                a.getId(),
                a.getLeadId(),
                a.getActivityType(),
                a.getSummary(),
                a.getDetails(),
                a.getDueDate(),
                a.getCompletedAt(),
                a.getAssignedToUserId(),
                a.getCreatedAt()
        );
    }

    private CrmApi.ChannelConfigResponse toChannelConfigResponse(CrmChannelConfig c) {
        return new CrmApi.ChannelConfigResponse(
                c.getId(),
                c.getChannelType(),
                c.getChannelName(),
                c.getAccountIdentifier(),
                c.getMaskedApiToken(),
                c.getWebhookSecret(),
                c.isBotEnabled(),
                c.getAutoReplyGreeting(),
                c.isActive(),
                c.getCreatedAt()
        );
    }

    private CrmApi.ConversationResponse toConversationResponse(CrmConversation c) {
        return new CrmApi.ConversationResponse(
                c.getId(),
                c.getChannelType(),
                c.getExternalSenderId(),
                c.getSenderName(),
                c.getLeadId(),
                c.getStatus(),
                c.getLastMessagePreview(),
                c.getUnreadCount(),
                c.getAssignedAgentId(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private CrmApi.MessageResponse toMessageResponse(CrmMessage m) {
        return new CrmApi.MessageResponse(
                m.getId(),
                m.getConversationId(),
                m.getDirection(),
                m.getSenderId(),
                m.getSenderName(),
                m.getMessageText(),
                m.getAttachmentUrl(),
                m.getDeliveryStatus(),
                m.isBotResponse(),
                m.getCreatedAt()
        );
    }
}
