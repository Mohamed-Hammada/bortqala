package com.bemo.hr.crm.api;

import com.bemo.hr.crm.domain.*;

import java.math.BigDecimal;
import java.util.List;

public final class CrmApi {

    private CrmApi() {}

    public record LeadResponse(
            String id,
            String leadCode,
            String name,
            String phone,
            String email,
            String companyName,
            CrmLeadSource source,
            CrmLeadStatus status,
            BigDecimal estimatedValue,
            String assignedSalesAgentId,
            String businessPartyId,
            String notes,
            long createdAt,
            long updatedAt
    ) {}

    public record SaveLeadRequest(
            String id,
            String name,
            String phone,
            String email,
            String companyName,
            CrmLeadSource source,
            CrmLeadStatus status,
            BigDecimal estimatedValue,
            String assignedSalesAgentId,
            String notes
    ) {}

    public record ActivityResponse(
            String id,
            String leadId,
            CrmActivityType activityType,
            String summary,
            String details,
            Long dueDate,
            Long completedAt,
            String assignedToUserId,
            long createdAt
    ) {}

    public record CreateActivityRequest(
            String leadId,
            CrmActivityType activityType,
            String summary,
            String details,
            Long dueDate,
            String assignedToUserId
    ) {}

    public record ChannelConfigResponse(
            String id,
            CrmChannelType channelType,
            String channelName,
            String accountIdentifier,
            String maskedApiToken,
            String webhookSecret,
            boolean botEnabled,
            String autoReplyGreeting,
            boolean active,
            long createdAt
    ) {}

    public record SaveChannelConfigRequest(
            String id,
            CrmChannelType channelType,
            String channelName,
            String accountIdentifier,
            String apiToken,
            String webhookSecret,
            boolean botEnabled,
            String autoReplyGreeting,
            boolean active
    ) {}

    public record ConversationResponse(
            String id,
            CrmChannelType channelType,
            String externalSenderId,
            String senderName,
            String leadId,
            CrmConversationStatus status,
            String lastMessagePreview,
            int unreadCount,
            String assignedAgentId,
            long createdAt,
            long updatedAt
    ) {}

    public record MessageResponse(
            String id,
            String conversationId,
            CrmMessageDirection direction,
            String senderId,
            String senderName,
            String messageText,
            String attachmentUrl,
            String deliveryStatus,
            boolean isBotResponse,
            long createdAt
    ) {}

    public record SendMessageRequest(
            String conversationId,
            String messageText,
            String attachmentUrl
    ) {}

    public record InboundWebhookRequest(
            CrmChannelType channelType,
            String senderId,
            String senderName,
            String messageText,
            String attachmentUrl
    ) {}

    public record PipelineStageSummary(
            CrmLeadStatus status,
            long count,
            BigDecimal totalValue
    ) {}

    public record CrmSummaryResponse(
            BigDecimal totalPipelineValue,
            long activeDeals,
            double winRate,
            long unreadMessages,
            List<PipelineStageSummary> stages
    ) {}
}
