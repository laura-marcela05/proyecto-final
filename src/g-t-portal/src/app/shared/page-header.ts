import { Component, computed, inject, input } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ROLE_LABELS } from '../core/models';

/** Title, back link, the user's name and role, "Cerrar sesión" and the navigation bar. */
@Component({
  selector: 'app-page-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="page-header">
      <div class="page-header__top">
        @if (backLink()) {
          <a class="back-link" [routerLink]="backLink()">&lt; {{ backLabel() }}</a>
        } @else {
          <h1>{{ title() }}</h1>
        }
        <div class="page-header__user">
          <span>{{ auth.fullName() }}</span>
          <span class="muted"> · {{ roleLabel() }}</span>
          <button type="button" class="btn btn-secondary" (click)="logout()">Cerrar sesión</button>
        </div>
      </div>
      <nav aria-label="Principal" class="nav-bar">
        <a routerLink="/students" routerLinkActive="nav-active">Estudiantes</a>
        @if (auth.role() === 'ADMIN') {
          <a routerLink="/users" routerLinkActive="nav-active">Usuarios</a>
        }
      </nav>
      @if (backLink()) {
        <h1>{{ title() }}</h1>
      }
    </header>
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly backLink = input<string | null>(null);
  readonly backLabel = input<string>('');

  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly roleLabel = computed(() => {
    const role = this.auth.role();
    return role ? ROLE_LABELS[role] : '';
  });

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
