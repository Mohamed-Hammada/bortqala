import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { HasPermissionDirective } from './has-permission.directive';
import { AuthService } from './auth.service';

@Component({
  template: `
    <div id="single-perm" *hasPermission="'finance:journal:post'">Post Journal</div>
    <div id="multi-perm" *hasPermission="['contracting:claim:create', 'contracting:claim:approve']">Claim Action</div>
    <div id="empty-perm" *hasPermission="[]">Always Show</div>
  `,
  standalone: true,
  imports: [HasPermissionDirective],
})
class TestHostComponent {}

describe('HasPermissionDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let allowedPerms: Set<string>;

  beforeEach(() => {
    allowedPerms = new Set<string>();
    const mockAuth = {
      hasAnyPermission: (perms: string[]) => perms.some((p) => allowedPerms.has(p)),
      hasPermission: (perm: string) => allowedPerms.has(perm),
    };

    TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [
        { provide: AuthService, useValue: mockAuth },
      ],
    });
  });

  it('renders elements when user has required permission', () => {
    allowedPerms.add('finance:journal:post');

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const singleEl = fixture.debugElement.query(By.css('#single-perm'));
    expect(singleEl).not.toBeNull();
    expect(singleEl.nativeElement.textContent).toContain('Post Journal');
  });

  it('hides elements when user lacks required permission', () => {
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const singleEl = fixture.debugElement.query(By.css('#single-perm'));
    expect(singleEl).toBeNull();
  });

  it('renders multi-permission element when at least one permission matches', () => {
    allowedPerms.add('contracting:claim:create');

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const multiEl = fixture.debugElement.query(By.css('#multi-perm'));
    expect(multiEl).not.toBeNull();
  });
});
