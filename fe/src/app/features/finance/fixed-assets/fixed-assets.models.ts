export type AssetCategory = 'VEHICLE' | 'MACHINERY' | 'EQUIPMENT' | 'BUILDING' | 'OTHER';
export type AssetStatus = 'ACTIVE' | 'FULLY_DEPRECIATED' | 'DISPOSED';

export interface FixedAssetResponse {
  id: string;
  name: string;
  category: AssetCategory;
  acquisitionDate: number;
  acquisitionCost: number;
  salvageValue: number;
  usefulLifeMonths: number;
  monthlyCharge: number;
  accumulatedDepreciation: number;
  netBookValue: number;
  lastPostedYearMonth: string | null;
  status: AssetStatus;
  disposalDate: number | null;
  disposalProceeds: number | null;
  branchId: string | null;
  costCenterId: string | null;
  version: number;
}

export interface FixedAssetPayload {
  name: string;
  category: AssetCategory;
  acquisitionDate: number;
  acquisitionCost: number;
  salvageValue: number;
  usefulLifeMonths: number;
  branchId?: string | null;
  costCenterId?: string | null;
}

export interface DisposalRequest {
  disposalDate: number;
  proceeds: number;
}

export interface DepreciationRunResult {
  assetId: string;
  assetName: string;
  charge: number;
  outcome: string;
  entryNumber: string | null;
}

export interface DepreciationRunResponse {
  yearMonth: string;
  postedCount: number;
  resultCount: number;
  totalCharge: number;
  results: DepreciationRunResult[];
}

export const ASSET_CATEGORIES: AssetCategory[] = ['VEHICLE', 'MACHINERY', 'EQUIPMENT', 'BUILDING', 'OTHER'];
