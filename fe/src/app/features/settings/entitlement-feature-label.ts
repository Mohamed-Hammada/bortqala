import {I18nService} from '../../core/i18n.service';

const FEATURE_KEYS: Record<string,string> = {
  navigation:'entitlements.feature.navigation',
  employeeAttendance:'entitlements.feature.employeeAttendance',
  'biometric.fileImport':'entitlements.feature.biometricFileImport',
  'biometric.liveSync':'entitlements.feature.biometricLiveSync',
  workforce:'entitlements.feature.workforce',
  'workforce.attendance':'entitlements.feature.workforceAttendance',
  'workforce.dashboard':'entitlements.feature.workforceDashboard',
  'workforce.contractorAccounts':'entitlements.feature.contractorAccounts',
  payroll:'entitlements.feature.payroll',
  procurement:'entitlements.feature.procurement',
  purchasing:'entitlements.feature.purchasing',
  'inventory.advanced':'entitlements.feature.inventoryAdvanced',
  sales:'entitlements.feature.sales',
  manufacturing:'entitlements.feature.manufacturing',
  quality:'entitlements.feature.quality',
  finance:'entitlements.feature.finance',
  exports:'entitlements.feature.exports',
  notifications:'entitlements.feature.notifications',
  'navigation.favorites':'entitlements.feature.navigationFavorites',
  'navigation.recents':'entitlements.feature.navigationRecents'
};
const SEGMENT_KEYS: Record<string,string> = {biometric:'entitlements.feature.biometric',fileImport:'entitlements.feature.fileImport',notifications:'entitlements.feature.notifications',navigation:'entitlements.feature.navigation',recents:'entitlements.feature.recents',finance:'entitlements.feature.finance',procurement:'entitlements.feature.procurement',workforce:'entitlements.feature.workforce'};

export function entitlementFeatureLabel(i18n: I18nService, key: string): string {
  const normalized = (key || '').replace(/\.enabled$/,'');
  const directKey = FEATURE_KEYS[normalized];
  if (directKey) return i18n.t(directKey);
  return normalized.split('.').map(part => {
    const catalogKey = SEGMENT_KEYS[part];
    return catalogKey ? i18n.t(catalogKey) : humanizeFeaturePart(part);
  }).join(' / ');
}

function humanizeFeaturePart(value: string): string {
  return value.replace(/([a-z0-9])([A-Z])/g,'$1 $2').replace(/[_-]+/g,' ').replace(/^./,ch=>ch.toUpperCase());
}
