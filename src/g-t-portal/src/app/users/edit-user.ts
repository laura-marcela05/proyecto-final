import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorCode, sharedMessage } from '../core/error-messages';
import { ROLE_LABELS, Role } from '../core/models';
import { NoticeService } from '../core/notice.service';
import { PageHeader } from '../shared/page-header';
import { RequestError } from '../shared/request-error';
import { SkeletonRows } from '../shared/skeleton-rows';
import { UsersService } from './users.service';

@Component({
  selector: 'app-edit-user',
  imports: [FormsModule, RouterLink, PageHeader, RequestError, SkeletonRows],
  template: `
    <app-page-header [title]="title()" backLink="/users" backLabel="Usuarios" />
    @switch (state()) {
      @case ('loading') { <app-skeleton-rows /> }
      @case ('error') { <app-request-error message="No se pudo cargar el usuario." (retry)="load()" /> }
      @case ('notfound') {
        <div class="card">
          <p class="error-text">Este usuario no existe.</p>
          <a routerLink="/users">Volver a usuarios</a>
        </div>
      }
      @default {
        <form class="card form" (ngSubmit)="submit()" novalidate>
          @if (formError()) {
            <p class="error-text" role="alert">{{ formError() }}</p>
          }
          <div class="field">
            <label for="fullName">Nombre completo</label>
            <input id="fullName" name="fullName" type="text" maxlength="100" [(ngModel)]="fullName"
                   [disabled]="saving()" aria-describedby="fullName-error" />
            @if (fullNameError()) { <p id="fullName-error" class="field-error">{{ fullNameError() }}</p> }
          </div>
          <div class="field">
            <label for="username">Usuario</label>
            <input id="username" name="username" type="text" maxlength="50" [(ngModel)]="username"
                   [disabled]="saving()" aria-describedby="username-error" />
            @if (usernameError()) { <p id="username-error" class="field-error">{{ usernameError() }}</p> }
          </div>
          <div class="field">
            <label for="password">Nueva contraseña (opcional)</label>
            <input id="password" name="password" type="password" maxlength="72" autocomplete="new-password"
                   [(ngModel)]="password" [disabled]="saving()" aria-describedby="password-hint password-error" />
            <p id="password-hint" class="hint">Déjala vacía para conservar la actual.</p>
            @if (passwordError()) { <p id="password-error" class="field-error">{{ passwordError() }}</p> }
          </div>
          <div class="form__actions">
            <a class="btn btn-secondary" routerLink="/users" [class.disabled]="saving()">Cancelar</a>
            <button type="submit" class="btn btn-primary" [disabled]="saving()">
              @if (saving()) { <span class="spinner" aria-hidden="true"></span> }
              Guardar
            </button>
          </div>
        </form>
      }
    }
  `,
})
export class EditUser implements OnInit {
  private readonly service = inject(UsersService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  private readonly userId = this.route.snapshot.paramMap.get('userId') ?? '';

  protected fullName = '';
  protected username = '';
  protected password = '';
  protected readonly role = signal<Role | null>(null);
  protected readonly state = signal<'loading' | 'ready' | 'error' | 'notfound'>('loading');
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly fullNameError = signal<string | null>(null);
  protected readonly usernameError = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);
  /** The role shows in the title only, as text: it cannot change (BR-03). */
  protected readonly title = computed(() => {
    const role = this.role();
    return role ? `Editar usuario · ${ROLE_LABELS[role]}` : 'Editar usuario';
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.service.get(this.userId).subscribe({
      next: (user) => {
        this.fullName = user.fullName;
        this.username = user.username;
        this.role.set(user.role);
        this.state.set('ready');
      },
      error: (error: unknown) => this.state.set(errorCode(error) === 'USER_NOT_FOUND' ? 'notfound' : 'error'),
    });
  }

  protected submit(): void {
    this.formError.set(null);
    this.fullNameError.set(this.fullName.trim() ? null : 'Ingresa el nombre completo.');
    this.usernameError.set(this.username.trim() ? null : 'Ingresa el nombre de usuario.');
    const typed = this.password.length > 0;
    this.passwordError.set(
      !typed || (this.password.length >= 8 && this.password.length <= 72)
        ? null
        : 'La contraseña debe tener entre 8 y 72 caracteres.',
    );
    if (this.fullNameError() || this.usernameError() || this.passwordError()) {
      return;
    }
    this.saving.set(true);
    const input = { fullName: this.fullName.trim(), username: this.username.trim(), ...(typed ? { password: this.password } : {}) };
    this.service.update(this.userId, input).subscribe({
      next: () => {
        this.notice.show('Usuario actualizado.');
        void this.router.navigate(['/users']);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.password = '';
        const code = errorCode(error);
        if (code === 'USERNAME_ALREADY_TAKEN') {
          this.usernameError.set('Ya existe un usuario con ese nombre. Elige otro.');
        } else if (code === 'USER_NOT_FOUND') {
          this.state.set('notfound');
        } else {
          this.formError.set(sharedMessage(error));
        }
      },
    });
  }
}
