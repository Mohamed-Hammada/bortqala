export interface AuditLog {
  id: string;
  action: string;
  entityType: string;
  entityId?: string;
  username: string;
  detailsJson?: string;
  ipAddress?: string;
  reason?: string;
  isBreakGlass?: boolean;
  userAgent?: string;
  occurredAt: number;
}

export interface AuditLogPage {
  content: AuditLog[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}
