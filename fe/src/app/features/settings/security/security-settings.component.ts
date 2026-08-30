import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SecurityPackService } from '../../../core/auth/security-pack.service';
import {
  RoleIpRuleResponse,
  SecurityPolicyResponse,
  TotpEnrollResponse,
  TotpStatusResponse,
  TrustedDeviceResponse,
} from '../../../core/auth/security-pack.models';
import { AuthService } from '../../../core/auth/auth.service';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-security-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './security-settings.component.html',
  styleUrl: './security-settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecuritySettingsComponent implements OnInit {
  private readonly securityService = inject(SecurityPackService);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly confirm = inject(ConfirmDialogService);

  readonly loading = signal(false);
  readonly savingPolicy = signal(false);
  readonly enrolling = signal(false);
  readonly activating = signal(false);
  readonly disabling = signal(false);

  // 2FA state
  readonly totpStatus = signal<TotpStatusResponse | null>(null);
  readonly enrollData = signal<TotpEnrollResponse | null>(null);
  readonly activationCode = signal('');
  readonly disablePassword = signal('');
  readonly showDisableModal = signal(false);
  readonly showBackupCodesModal = signal(false);
  readonly backupCodesList = signal<string[]>([]);

  // Password Policy state
  readonly minPasswordLength = signal(8);
  readonly requireUppercase = signal(true);
  readonly requireLowercase = signal(true);
  readonly requireDigits = signal(true);
  readonly requireSpecialChars = signal(false);
  readonly passwordHistoryCount = signal(3);
  readonly maxPasswordAgeDays = signal(0);
  readonly sessionTimeoutMinutes = signal(30);
  readonly superAdminIpBypass = signal(true);

  // Devices & IP Rules
  readonly devices = signal<TrustedDeviceResponse[]>([]);
  readonly ipRules = signal<RoleIpRuleResponse[]>([]);

  // New IP Rule form
  readonly newRuleRole = signal('ADMIN');
  readonly newRuleCidr = signal('');
  readonly newRuleDesc = signal('');
  readonly addingRule = signal(false);

  readonly isAdmin = computed(() =>
    this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN'])
  );

  readonly availableRoles = [
    { code: 'SUPER_ADMIN', label: 'Super Admin' },
    { code: 'ADMIN', label: 'Admin' },
    { code: 'FINANCIAL_MANAGER', label: 'Financial Manager' },
    { code: 'HR_MANAGER', label: 'HR Manager' },
    { code: 'ACCOUNTANT', label: 'Accountant' },
    { code: 'AUDITOR', label: 'Auditor' },
    { code: 'DATA_ENTRY', label: 'Data Entry' },
    { code: 'VIEWER', label: 'Viewer' },
  ];

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.securityService.getTotpStatus().subscribe({
      next: (status) => {
        this.totpStatus.set(status);
      },
      error: (err) => this.notification.error(apiErrorMessage(err)),
    });

    this.securityService.loadDevices().subscribe({
      next: (devs) => this.devices.set(devs),
      error: (err) => this.notification.error(apiErrorMessage(err)),
    });

    if (this.isAdmin()) {
      this.securityService.getPolicy().subscribe({
        next: (pol) => this.applyPolicy(pol),
        error: (err) => this.notification.error(apiErrorMessage(err)),
      });

      this.securityService.loadIpRules().subscribe({
        next: (rules) => this.ipRules.set(rules),
        error: (err) => this.notification.error(apiErrorMessage(err)),
        complete: () => this.loading.set(false),
      });
    } else {
      this.loading.set(false);
    }
  }

  private applyPolicy(p: SecurityPolicyResponse): void {
    this.minPasswordLength.set(p.minPasswordLength);
    this.requireUppercase.set(p.requireUppercase);
    this.requireLowercase.set(p.requireLowercase);
    this.requireDigits.set(p.requireDigits);
    this.requireSpecialChars.set(p.requireSpecialChars);
    this.passwordHistoryCount.set(p.passwordHistoryCount);
    this.maxPasswordAgeDays.set(p.maxPasswordAgeDays);
    this.sessionTimeoutMinutes.set(p.sessionTimeoutMinutes);
    this.superAdminIpBypass.set(p.superAdminIpBypass);
  }

  // --- 2FA Actions ---

  startEnroll(): void {
    this.enrolling.set(true);
    this.securityService.enrollTotp().subscribe({
      next: (res) => {
        this.enrollData.set(res);
        this.backupCodesList.set(res.backupCodes);
        this.enrolling.set(false);
      },
      error: (err) => {
        this.enrolling.set(false);
        this.notification.error(apiErrorMessage(err));
      },
    });
  }

  confirmActivation(): void {
    const code = this.activationCode().trim();
    if (!code || code.length < 6) return;

    this.activating.set(true);
    this.securityService.activateTotp(code).subscribe({
      next: () => {
        this.activating.set(false);
        this.enrollData.set(null);
        this.activationCode.set('');
        this.totpStatus.set({
          enabled: true,
          enabledAt: new Date().toISOString(),
          remainingBackupCodes: this.backupCodesList().length || 10,
        });
        this.showBackupCodesModal.set(true);
        this.notification.success(
          this.i18n.t('settings.security.totpEnabled')
        );
      },
      error: (err) => {
        this.activating.set(false);
        this.notification.error(apiErrorMessage(err));
      },
    });
  }

  confirmDisable(): void {
    const pwd = this.disablePassword().trim();
    if (!pwd) return;

    this.disabling.set(true);
    this.securityService.disableTotp(pwd).subscribe({
      next: () => {
        this.disabling.set(false);
        this.showDisableModal.set(false);
        this.disablePassword.set('');
        this.totpStatus.set({
          enabled: false,
          enabledAt: null,
          remainingBackupCodes: 0,
        });
        this.notification.success(
          this.i18n.t('settings.security.totpDisabled')
        );
      },
      error: (err) => {
        this.disabling.set(false);
        this.notification.error(apiErrorMessage(err));
      },
    });
  }

  downloadBackupCodes(): void {
    const codes = this.backupCodesList().join('\n');
    const blob = new Blob([codes], { type: 'text/plain;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'bemo-backup-codes.txt';
    link.click();
    window.URL.revokeObjectURL(url);
  }

  regenerateBackupCodes(): void {
    this.securityService.regenerateBackupCodes('current').subscribe({
      next: (codes) => {
        this.backupCodesList.set(codes);
        this.showBackupCodesModal.set(true);
        this.notification.success(
          this.i18n.t('settings.security.backupCodesTitle')
        );
      },
      error: (err) => this.notification.error(apiErrorMessage(err)),
    });
  }

  // --- Password Policy Actions ---

  savePolicy(): void {
    this.savingPolicy.set(true);
    this.securityService
      .updatePolicy({
        minPasswordLength: this.minPasswordLength(),
        requireUppercase: this.requireUppercase(),
        requireLowercase: this.requireLowercase(),
        requireDigits: this.requireDigits(),
        requireSpecialChars: this.requireSpecialChars(),
        passwordHistoryCount: this.passwordHistoryCount(),
        maxPasswordAgeDays: this.maxPasswordAgeDays(),
        sessionTimeoutMinutes: this.sessionTimeoutMinutes(),
        superAdminIpBypass: this.superAdminIpBypass(),
      })
      .subscribe({
        next: (pol) => {
          this.applyPolicy(pol);
          this.savingPolicy.set(false);
          this.notification.success(
            this.i18n.t('settings.security.passwordPolicyTitle')
          );
        },
        error: (err) => {
          this.savingPolicy.set(false);
          this.notification.error(apiErrorMessage(err));
        },
      });
  }

  // --- Device Actions ---

  revokeDevice(id: string): void {
    this.confirm
      .confirmOptions({
        titleKey: 'settings.security.revokeDevice',
        messageKey: 'settings.security.revokeConfirm',
        confirmKey: 'settings.security.revokeDevice',
        danger: true,
      })
      .then((ok) => {
        if (!ok) return;
        this.securityService.revokeDevice(id).subscribe({
          next: () => {
            this.devices.update((list) =>
              list.map((d) =>
                d.id === id
                  ? { ...d, revoked: true, revokedAt: new Date().toISOString() }
                  : d
              )
            );
            this.notification.success(
              this.i18n.t('settings.security.revokeDevice')
            );
          },
          error: (err) => this.notification.error(apiErrorMessage(err)),
        });
      });
  }

  // --- IP Rule Actions ---

  addIpRule(): void {
    const cidr = this.newRuleCidr().trim();
    if (!cidr) return;

    this.addingRule.set(true);
    this.securityService
      .createIpRule({
        roleCode: this.newRuleRole(),
        cidrBlock: cidr,
        description: this.newRuleDesc().trim(),
      })
      .subscribe({
        next: (rule) => {
          this.ipRules.update((list) => [rule, ...list]);
          this.newRuleCidr.set('');
          this.newRuleDesc.set('');
          this.addingRule.set(false);
          this.notification.success(
            this.i18n.t('settings.security.addRule')
          );
        },
        error: (err) => {
          this.addingRule.set(false);
          this.notification.error(apiErrorMessage(err));
        },
      });
  }

  deleteIpRule(ruleId: string): void {
    this.securityService.deleteIpRule(ruleId).subscribe({
      next: () => {
        this.ipRules.update((list) => list.filter((r) => r.id !== ruleId));
        this.notification.success(
          this.i18n.t('settings.security.deleteRule')
        );
      },
      error: (err) => this.notification.error(apiErrorMessage(err)),
    });
  }
}
