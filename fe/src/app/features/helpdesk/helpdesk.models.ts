export interface HelpdeskCategory {
  id: string;
  nameAr: string;
  nameEn: string;
  slaFirstResponseHours: number;
  slaResolutionHours: number;
  active: boolean;
  version: number;
}

export interface Ticket {
  id: string;
  ticketNo: number;
  requesterUserId: string;
  categoryId: string;
  title: string;
  description: string;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  status: 'NEW' | 'OPEN' | 'WAITING_CUSTOMER' | 'RESOLVED' | 'CLOSED';
  assigneeUserId: string | null;
  firstResponseAtEpochMs: number | null;
  resolvedAtEpochMs: number | null;
  dueFirstResponseEpochMs: number | null;
  dueResolutionEpochMs: number | null;
  slaBreachFirstResponse: boolean;
  slaBreachResolution: boolean;
  createdAtEpochMs: number | null;
  version: number;
}

export interface TicketMessage {
  id: string;
  ticketId: string;
  authorUserId: string;
  body: string;
  internalNote: boolean;
  attachmentName: string | null;
  createdAtEpochMs: number | null;
}

export interface KbArticle {
  id: string;
  slug: string;
  titleAr: string;
  titleEn: string;
  bodyAr: string;
  bodyEn: string;
  tags: string;
  published: boolean;
  views: number;
  helpfulUp: number;
  helpfulDown: number;
  authorUserId: string;
  createdAtEpochMs: number | null;
  version: number;
}

export interface TicketListResponse {
  tickets: Ticket[];
  openCount: number;
  myOpenCount: number;
}
