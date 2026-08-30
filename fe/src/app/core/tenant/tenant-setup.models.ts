export type BusinessVertical =
  | 'GENERAL'
  | 'MEDICAL'
  | 'CIVIL'
  | 'RETAIL'
  | 'MANUFACTURING'
  | 'SERVICES';

export interface ConfigureVerticalRequest {
  vertical: BusinessVertical;
}

export interface TenantVerticalResponse {
  appId: string;
  vertical: BusinessVertical;
  activeFeatures: string[];
  provisionedPolicyGroups: string[];
}
