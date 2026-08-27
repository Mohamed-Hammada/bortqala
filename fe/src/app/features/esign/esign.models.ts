export type PacketStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'DECLINED';
export type StepStatus = 'PENDING' | 'SIGNED' | 'DECLINED' | 'CANCELLED';
export type SignatureMethod = 'DRAWN' | 'TYPED' | 'UPLOADED';

export interface SignatureStep {
  id: string;
  stepOrder: number;
  signerName: string;
  signerUserId: string | null;
  roleLabel: string | null;
  status: StepStatus;
  signedAt: number | null;
  ipAddress: string | null;
  contentSha256: string | null;
  method: SignatureMethod | null;
  declineReason: string | null;
}

export interface SignaturePacket {
  id: string;
  title: string;
  documentName: string | null;
  contentHash: string;
  status: PacketStatus;
  manifestJson: string | null;
  steps: SignatureStep[];
  createdAt: number;
  updatedAt: number;
}

export interface CreatePacketStep {
  stepOrder: number;
  signerName: string;
  signerUserId?: string;
  roleLabel?: string;
}

export interface CreatePacketRequest {
  title: string;
  documentName?: string;
  contentHash: string;
  steps: CreatePacketStep[];
}

export interface SignStepRequest {
  contentSha256: string;
  method: SignatureMethod;
  ipAddress?: string;
}

export interface ManifestStep {
  id: string;
  stepOrder: number;
  signerName: string;
  signerUserId: string | null;
  roleLabel: string | null;
  status: StepStatus;
  signedAt: number | null;
  ipAddress: string | null;
  contentSha256: string | null;
  method: SignatureMethod | null;
  declineReason: string | null;
}

export interface ManifestExport {
  packetId: string;
  title: string;
  documentName: string | null;
  contentHash: string;
  status: PacketStatus;
  steps: ManifestStep[];
  exportedAt: number;
}

export const PACKET_STATUSES: PacketStatus[] = ['DRAFT', 'IN_PROGRESS', 'COMPLETED', 'DECLINED'];

export const STEPS_SORTED = (steps: SignatureStep[]) =>
  [...steps].sort((a, b) => a.stepOrder - b.stepOrder);