import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import { ProjectExecutiveDashboardService } from './project-executive-dashboard.service';
import { ProjectExecutiveDashboardResponse, ProjectMatrixRow } from './project-executive-dashboard.models';

@Component({
  selector: 'app-project-executive-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    DecimalPipe,
    DatePipe
  ],
  templateUrl: './project-executive-dashboard.component.html',
  styleUrls: ['./project-executive-dashboard.component.scss']
})
export class ProjectExecutiveDashboardComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly dashboardService = inject(ProjectExecutiveDashboardService);

  readonly loading = signal<boolean>(false);
  readonly data = signal<ProjectExecutiveDashboardResponse | null>(null);

  readonly selectedCompanyId = signal<string>('');
  readonly selectedBranchId = signal<string>('');
  readonly searchTerm = signal<string>('');
  readonly statusFilter = signal<string>('ALL');

  readonly filteredProjects = computed(() => {
    const res = this.data();
    if (!res || !res.projects) return [];
    let list = res.projects;

    const term = this.searchTerm().trim().toLowerCase();
    if (term) {
      list = list.filter(p => p.projectName.toLowerCase().includes(term) || p.projectId.toLowerCase().includes(term));
    }

    const status = this.statusFilter();
    if (status !== 'ALL') {
      list = list.filter(p => p.status === status);
    }

    return list;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.dashboardService.getExecutiveDashboard(
      this.selectedCompanyId() || undefined,
      this.selectedBranchId() || undefined
    ).subscribe({
      next: res => {
        this.data.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
