export interface ApiKey {
  id: string;
  name: string;
  scopes: string;
  rateLimitPerMin: number;
  active: boolean;
  lastUsedAtEpochMs: number | null;
  createdBy: string;
  createdAtEpochMs: number;
  updatedAtEpochMs: number;
  version: number;
}

export interface ApiKeyCreateResponse {
  id: string;
  name: string;
  fullKey: string;
  scopes: string;
  rateLimitPerMin: number;
  active: boolean;
  createdAtEpochMs: number;
}

export interface ApiKeyCreateRequest {
  name: string;
  scopes: string;
  rateLimitPerMin: number;
}

export interface WebhookEndpoint {
  id: string;
  url: string;
  events: string;
  active: boolean;
  createdAtEpochMs: number;
  updatedAtEpochMs: number;
  version: number;
}

export interface WebhookEndpointCreateRequest {
  url: string;
  events: string;
}

export interface WebhookDelivery {
  id: number;
  endpointId: string;
  event: string;
  payload: string;
  status: string;
  attempts: number;
  lastError: string | null;
  responseStatus: number | null;
  createdAtEpochMs: number;
}

export interface SearchResultItem {
  type: string;
  id: string;
  title: string;
  subtitle: string;
  url: string;
}

export interface GridView {
  id: string;
  userId: string;
  pageKey: string;
  name: string;
  filters: string | null;
  hiddenColumns: string | null;
  sort: string | null;
  sharedRoles: string | null;
  createdAtEpochMs: number;
}

export interface GridViewSaveRequest {
  pageKey: string;
  name: string;
  filters?: string;
  hiddenColumns?: string;
  sort?: string;
  sharedRoles?: string;
}

export interface BulkUpdateRequest {
  entityType: string;
  field: string;
  value: string;
  ids: string[];
}

export interface BulkUpdateResultItem {
  id: string;
  success: boolean;
  error: string | null;
}
