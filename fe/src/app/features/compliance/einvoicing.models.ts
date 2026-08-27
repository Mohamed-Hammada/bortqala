export type EinvoicingProviderType = 'EGYPT_ETA' | 'KSA_ZATCA' | 'NONE';
export type EinvoicingEnvironment = 'TEST' | 'PRODUCTION';

export interface EinvoicingSettings {
  id: string;
  provider: EinvoicingProviderType;
  environment: EinvoicingEnvironment;
  createdAt: number;
  updatedAt: number;
}

export interface EinvoicingProviderInfo {
  type: EinvoicingProviderType;
  labelKey: string;
  supported: boolean;
}

export interface SaveEinvoicingSettingsRequest {
  provider: EinvoicingProviderType;
  environment: EinvoicingEnvironment;
}