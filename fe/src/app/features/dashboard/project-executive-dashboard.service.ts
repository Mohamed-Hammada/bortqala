import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProjectExecutiveDashboardResponse } from './project-executive-dashboard.models';

@Injectable({
  providedIn: 'root'
})
export class ProjectExecutiveDashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/executive-dashboard/projects';

  getExecutiveDashboard(companyId?: string, branchId?: string): Observable<ProjectExecutiveDashboardResponse> {
    let params = new HttpParams();
    if (companyId) {
      params = params.set('companyId', companyId);
    }
    if (branchId) {
      params = params.set('branchId', branchId);
    }
    return this.http.get<ProjectExecutiveDashboardResponse>(this.baseUrl, { params });
  }
}
