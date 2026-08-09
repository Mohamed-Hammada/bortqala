export type IntegrationStatus =
  | 'COMPATIBLE'
  | 'NEEDS_SDK_VERSION'
  | 'NEEDS_API_VERSION'
  | 'NEEDS_SERVER_VERSION'
  | 'NEEDS_VENDOR_MATRIX'
  | 'INCOMPATIBLE'
  | string;

export interface SupplierInfo {
  supplier: string;
  routes: string[];
  documentation_file?: string;
}

export interface HubHealth {
  ok: boolean;
  version?: string;
  suppliers?: string[];
  architecture?: string;
}

export interface RouteCandidate {
  route: string;
  kind: string;
  status: IntegrationStatus;
  reason: string;
  sdkVersionSpec: string;
  apiVersionSpec: string;
  serverVersionSpec: string;
  implementationStatus: string;
  officialDocumentation: string[];
}

export interface RouteResolution {
  supplier: string;
  modelPattern: string;
  generationOrVersion: string;
  preferredRoute: string | null;
  compatibleRoutes: string[];
  candidates: RouteCandidate[];
  notes: string;
  officialDocumentation: string[];
}

export interface DeviceIntegrationRequest {
  name: string;
  vendor: string;
  model: string;
  serialNumber: string | null;
  firmwareVersion: string | null;
  platformVersion: string | null;
  serverVersion: string | null;
  osName: string | null;
  architecture: string | null;
  sdkVersions: Record<string, string>;
  apiVersions: Record<string, string>;
  capabilityHints: string[];
  host: string | null;
  port: number | null;
  baseUrl: string | null;
  route: string | null;
  options: Record<string, unknown>;
  username: string | null;
  password: string | null;
  enabled: boolean;
  syncIntervalMinutes: number;
}

export interface RouteRequest extends Omit<DeviceIntegrationRequest, 'name' | 'serialNumber' | 'username' | 'password' | 'enabled' | 'syncIntervalMinutes'> {}

export interface DeviceIntegration {
  id: string;
  biometricDeviceId: string;
  hubDeviceId: string;
  name: string;
  vendor: string;
  model: string;
  serialNumber: string | null;
  firmwareVersion: string | null;
  platformVersion: string | null;
  serverVersion: string | null;
  osName: string | null;
  architecture: string | null;
  sdkVersions: Record<string, string>;
  apiVersions: Record<string, string>;
  capabilityHints: string[];
  host: string | null;
  port: number | null;
  baseUrl: string | null;
  route: string;
  routeStatus: string;
  routeKind: string;
  implementationStatus: string;
  officialDocumentation: string[];
  options: Record<string, unknown>;
  enabled: boolean;
  syncIntervalMinutes: number;
  username: string | null;
  hasPassword: boolean;
  lastProbeStatus: string | null;
  lastProbeMessage: string | null;
  lastProbeAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProbeResult {
  status: string;
  ok: boolean;
  route: string;
  detail: string;
  data: unknown;
  checkedAt: string;
}

export interface SyncResult {
  receivedRows: number;
  importedRows: number;
  duplicateRows: number;
  duplicateBatch: boolean;
}
