import {
  Directive,
  Input,
  TemplateRef,
  ViewContainerRef,
  effect,
  inject,
  signal,
} from '@angular/core';
import { AuthService } from './auth.service';

@Directive({
  selector: '[hasPermission]',
  standalone: true,
})
export class HasPermissionDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly authService = inject(AuthService);

  private readonly requiredPermissions = signal<string[]>([]);
  private isDisplayed = false;

  constructor() {
    effect(() => {
      const required = this.requiredPermissions();
      const hasAccess = this.checkPermissions(required);

      if (hasAccess && !this.isDisplayed) {
        this.viewContainer.createEmbeddedView(this.templateRef);
        this.isDisplayed = true;
      } else if (!hasAccess && this.isDisplayed) {
        this.viewContainer.clear();
        this.isDisplayed = false;
      }
    });
  }

  @Input()
  set hasPermission(value: string | string[] | null | undefined) {
    if (!value) {
      this.requiredPermissions.set([]);
    } else if (Array.isArray(value)) {
      this.requiredPermissions.set(value.filter(Boolean));
    } else {
      this.requiredPermissions.set([value]);
    }
  }

  private checkPermissions(required: string[]): boolean {
    if (required.length === 0) {
      return true;
    }
    return this.authService.hasAnyPermission(required);
  }
}
