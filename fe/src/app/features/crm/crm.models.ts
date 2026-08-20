export type CrmLeadStatus =
  | 'NEW'
  | 'CONTACTED'
  | 'QUALIFIED'
  | 'PROPOSAL_SENT'
  | 'NEGOTIATION'
  | 'WON'
  | 'LOST';

export type CrmLeadSource =
  | 'WHATSAPP'
  | 'FACEBOOK'
  | 'WEBSITE'
  | 'REFERRAL'
  | 'DIRECT_CALL'
  | 'EXHIBITION'
  | 'OTHER';

export type CrmActivityType =
  | 'CALL'
  | 'MEETING'
  | 'WHATSAPP_MESSAGE'
  | 'EMAIL'
  | 'NOTE'
  | 'TASK';

export type CrmChannelType =
  | 'WHATSAPP'
  | 'FACEBOOK_MESSENGER'
  | 'INSTAGRAM'
  | 'WEB_CHATBOT';

export type CrmConversationStatus =
  | 'OPEN'
  | 'BOT_HANDLED'
  | 'AGENT_HANDLED'
  | 'RESOLVED'
  | 'CLOSED';

export type CrmMessageDirection = 'INBOUND' | 'OUTBOUND';

export interface CrmLead {
  id: string;
  leadCode: string;
  name: string;
  phone?: string;
  email?: string;
  companyName?: string;
  source: CrmLeadSource;
  status: CrmLeadStatus;
  estimatedValue: number;
  assignedSalesAgentId?: string;
  businessPartyId?: string;
  notes?: string;
  createdAt: number;
  updatedAt: number;
}

export interface SaveLeadRequest {
  id?: string;
  name: string;
  phone?: string;
  email?: string;
  companyName?: string;
  source: CrmLeadSource;
  status: CrmLeadStatus;
  estimatedValue: number;
  assignedSalesAgentId?: string;
  notes?: string;
}

export interface CrmActivity {
  id: string;
  leadId: string;
  activityType: CrmActivityType;
  summary: string;
  details?: string;
  dueDate?: number;
  completedAt?: number;
  assignedToUserId?: string;
  createdAt: number;
}

export interface CreateActivityRequest {
  leadId: string;
  activityType: CrmActivityType;
  summary: string;
  details?: string;
  dueDate?: number;
  assignedToUserId?: string;
}

export interface CrmChannelConfig {
  id: string;
  channelType: CrmChannelType;
  channelName: string;
  accountIdentifier?: string;
  maskedApiToken?: string;
  webhookSecret?: string;
  botEnabled: boolean;
  autoReplyGreeting?: string;
  active: boolean;
  createdAt: number;
}

export interface SaveChannelConfigRequest {
  id?: string;
  channelType: CrmChannelType;
  channelName: string;
  accountIdentifier?: string;
  apiToken?: string;
  webhookSecret?: string;
  botEnabled: boolean;
  autoReplyGreeting?: string;
  active: boolean;
}

export interface CrmConversation {
  id: string;
  channelType: CrmChannelType;
  externalSenderId: string;
  senderName?: string;
  leadId?: string;
  status: CrmConversationStatus;
  lastMessagePreview?: string;
  unreadCount: number;
  assignedAgentId?: string;
  createdAt: number;
  updatedAt: number;
}

export interface CrmMessage {
  id: string;
  conversationId: string;
  direction: CrmMessageDirection;
  senderId?: string;
  senderName?: string;
  messageText: string;
  attachmentUrl?: string;
  deliveryStatus?: string;
  isBotResponse: boolean;
  createdAt: number;
}

export interface SendMessageRequest {
  conversationId: string;
  messageText: string;
  attachmentUrl?: string;
}

export interface PipelineStageSummary {
  status: CrmLeadStatus;
  count: number;
  totalValue: number;
}

export interface CrmSummary {
  totalPipelineValue: number;
  activeDeals: number;
  winRate: number;
  unreadMessages: number;
  stages: PipelineStageSummary[];
}
