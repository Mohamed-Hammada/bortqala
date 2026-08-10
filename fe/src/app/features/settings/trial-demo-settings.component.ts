import {ChangeDetectionStrategy,Component,inject,signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {FormsModule} from '@angular/forms';
import {firstValueFrom} from 'rxjs';
import {I18nService} from '../../core/i18n.service';
import {NotificationService} from '../../core/notification.service';
import {apiErrorMessage} from '../../core/api-error';

interface Template{code:string;version:number;nameKey:string}
interface Sample{key:string;payloadJson:string}
interface TrialStatus{tenantId:string;commercialState:string;trialStartedAt:number;trialEndsAt:number;convertedAt:number;writeAllowed:boolean;demoTenant:boolean;templateCode?:string;templateVersion?:number;lastResetAt:number;lastResetBy?:string;sampleCount:number;samples:Sample[]}

@Component({selector:'app-trial-demo-settings',imports:[FormsModule],templateUrl:'./trial-demo-settings.component.html',styleUrl:'./trial-demo-settings.component.scss',changeDetection:ChangeDetectionStrategy.OnPush})
export class TrialDemoSettingsComponent{
  private readonly http=inject(HttpClient);readonly i18n=inject(I18nService);private readonly notifications=inject(NotificationService);
  readonly status=signal<TrialStatus|null>(null);readonly templates=signal<Template[]>([]);readonly loading=signal(true);readonly saving=signal(false);readonly error=signal<string|null>(null);
  days=14;demo=true;templateCode='CONTRACTOR_WORKFORCE_EG';templateVersion:number|undefined;
  constructor(){void this.load();}
  async load(){this.loading.set(true);this.error.set(null);try{const [status,templates]=await Promise.all([firstValueFrom(this.http.get<TrialStatus>('/api/v1/platform/trial')),firstValueFrom(this.http.get<Template[]>('/api/v1/platform/trial/templates'))]);this.status.set(status);this.templates.set(templates);if(status.templateCode)this.templateCode=status.templateCode;if(status.templateVersion)this.templateVersion=status.templateVersion;}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.loading.set(false);}}
  async start(){await this.command('/start',{days:Math.max(1,Math.min(90,this.days)),demo:this.demo,templateCode:this.demo?this.templateCode:null,templateVersion:this.demo?this.templateVersion:null,operationId:crypto.randomUUID()},'trialDemo.started');}
  async convert(){await this.command('/convert',{operationId:crypto.randomUUID()},'trialDemo.converted');}
  async reset(){await this.command('/reset',{operationId:crypto.randomUUID(),templateVersion:this.templateVersion??null},'trialDemo.resetDone');}
  format(value:number){return value?new Intl.DateTimeFormat(undefined,{dateStyle:'medium',timeStyle:'short'}).format(value):'—';}
  stateKey(state:string){return `trialDemo.state.${state.toLowerCase()}`;}
  private async command(path:string,body:unknown,successKey:string){if(this.saving())return;this.saving.set(true);this.error.set(null);try{this.status.set(await firstValueFrom(this.http.post<TrialStatus>(`/api/v1/platform/trial${path}`,body)));this.notifications.success(this.i18n.t(successKey));}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.saving.set(false);}}
}
