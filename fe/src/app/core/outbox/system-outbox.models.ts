export interface OutboxEventSummary {
  id: string;
  eventType: string;
  aggregateType: string;
  aggregateId: string;
  payloadJson: string;
  status: string;
  retryCount: number;
  maxRetries: number;
  lastError?: string | null;
  createdAt: string;
  processedAt?: string | null;
}

export interface OutboxStatsResponse {
  pendingCount: number;
  publishedCount: number;
  failedCount: number;
  deadLetterCount: number;
}

export interface OutboxPageResponse {
  items: OutboxEventSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
