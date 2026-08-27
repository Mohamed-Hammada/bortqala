export interface PaymentLink {
  id: string;
  kind: string;
  refId: string | null;
  amount: number;
  currency: string;
  token: string;
  status: string;
  gatewayRef: string | null;
  description: string | null;
  companyName: string | null;
  expiresAtEpochMs: number | null;
  paidAtEpochMs: number | null;
  createdAtEpochMs: number | null;
  version: number;
}

export interface PublicPagePayload {
  companyName: string | null;
  description: string | null;
  amount: number;
  currency: string;
  expired: boolean;
  paid: boolean;
}

export interface CreateLinkPayload {
  kind: string;
  refId?: string;
  amount: number;
  description?: string;
  expiresAtEpochMs?: number;
}

export interface OutboundLogEntry {
  id: string;
  recipientType: string;
  recipientId: string;
  phoneNumber: string;
  templateKey: string;
  status: string;
  providerMessageId: string | null;
  errorMessage: string | null;
  retryCount: number;
  sentAtEpochMs: number | null;
  createdAtEpochMs: number | null;
}

export interface WhatsAppSettings {
  configured: boolean;
  provider: string;
  templates: { key: string; templateName: string }[];
}

export interface RecurringTemplate {
  id: string;
  kind: string;
  templateName: string;
  payloadSnapshot: string;
  cadence: string;
  cadenceDays: number | null;
  nextRunAtEpochMs: number;
  active: boolean;
  lastCreatedRef: string | null;
  version: number;
}

export interface DunningRule {
  id: string;
  daysOverdue: number;
  templateKey: string;
  channel: string;
  active: boolean;
  version: number;
}

export interface JobEntry {
  id: string;
  type: string;
  status: string;
  error: string | null;
  createdAtEpochMs: number;
}
