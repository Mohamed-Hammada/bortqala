import {ChangeDetectionStrategy,Component,inject,signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {firstValueFrom} from 'rxjs';
import {I18nService} from '../../core/i18n.service';
import {NotificationService} from '../../core/notification.service';
import {apiErrorMessage} from '../../core/api-error';
interface Feature{key:string;enabled:boolean;dependencies:string[];configJson?:string;version:number;updatedBy?:string;changeReason?:string;updatedAt:number}interface Module{key:string;features:Feature[]}
@Component({selector:'app-entitlement-settings',imports:[FormsModule],templateUrl:'./entitlement-settings.component.html',styleUrl:'./entitlement-settings.component.scss',changeDetection:ChangeDetectionStrategy.OnPush})
export class EntitlementSettingsComponent{private http=inject(HttpClient);readonly i18n=inject(I18nService);private notifications=inject(NotificationService);readonly modules=signal<Module[]>([]);readonly loading=signal(true);readonly saving=signal<string|null>(null);readonly error=signal<string|null>(null);readonly reasons=signal<Record<string,string>>({});
  constructor(){void this.load();}async load(){this.loading.set(true);this.error.set(null);try{this.modules.set(await firstValueFrom(this.http.get<Module[]>('/api/v1/platform/entitlements')));}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.loading.set(false);}}
  reason(key:string){return this.reasons()[key]??'';}setReason(key:string,value:string){this.reasons.update(r=>({...r,[key]:value}));}
  moduleLabel(key:string){const labels:Record<string,string>={HR:'entitlements.module.hr',WORKFORCE:'entitlements.module.workforce',PAYROLL:'entitlements.module.payroll',PROCUREMENT:'entitlements.module.procurement',INVENTORY:'entitlements.module.inventory',SALES:'entitlements.module.sales',MANUFACTURING:'entitlements.module.manufacturing',QUALITY:'entitlements.module.quality',FINANCE:'entitlements.module.finance',PLATFORM:'entitlements.module.platform'};return this.i18n.t(labels[key]??'entitlements.title');}

  async toggle(feature:Feature){const reason=this.reason(feature.key).trim();if(!reason){this.error.set(this.i18n.t('entitlements.reasonRequired'));return;}if(this.saving())return;this.saving.set(feature.key);this.error.set(null);try{await firstValueFrom(this.http.put(`/api/v1/platform/entitlements/${encodeURIComponent(feature.key)}`,{enabled:!feature.enabled,configJson:feature.configJson??null,reason,expectedVersion:feature.version}));this.notifications.success(this.i18n.t('entitlements.saved'));this.setReason(feature.key,'');await this.load();}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.saving.set(null);}}

  // BORTQALA_FEEDBACK_20260816_ENTITLEMENT_LABELS
  featureLabel(key: string): string {
    const ar = this.i18n.locale().toLowerCase().startsWith('ar');
    const normalized = (key || '').replace(/\.enabled$/,'');
    const labels: Record<string, [string,string]> = {
      procurement:['Procurement','المشتريات'], sales:['Sales','المبيعات'], navigation:['Navigation','التنقل'],
      'navigation.recents':['Recent navigation','التنقلات الأخيرة'], 'employeeAttendance':['Employee Attendance','حضور الموظفين'],
      'employee.attendance':['Employee Attendance','حضور الموظفين'], attendance:['Employee Attendance','حضور الموظفين'],
      'biometric.fileImport':['Biometric file import','استيراد ملفات البصمة'], notifications:['Notifications','الإشعارات'],
      finance:['Finance','المالية'], workforce:['Workforce','العمالة'], 'workforce.attendance':['Workforce attendance','حضور العمالة']
    };
    const direct=labels[normalized]; if(direct) return direct[ar?1:0];
    return normalized.split('.').map(part => part.replace(/([a-z0-9])([A-Z])/g,'$1 $2').replace(/[_-]+/g,' ').replace(/^./,ch=>ch.toUpperCase())).join(' / ');
  }
  dependencyLabels(keys: string[]): string { return (keys ?? []).map(key => this.featureLabel(key)).join(', '); }

}
