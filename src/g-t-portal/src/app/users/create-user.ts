import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { errorCode, sharedMessage } from '../core/error-messages';
import { ROLE_LABELS, Role } from '../core/models';
import { NoticeService } from '../core/notice.service';
import { PageHeader } from '../shared/page-header';
import { UsersService } from './users.service';

@Component({
  selector: 'app-create-user',
  imports: [FormsModule, RouterLink, PageHeader],
  template: `
    <app-page-header title="Crear usuario" backLink="/users" backLabel="Usuarios" />
    <form class="card form" (ngSubmit)="submit()" novalidate>
      @if (formError()) {
        <p class="error-text" role="alert">{{ formError() }}</p>
      }
      <div class="field">
        <label for="fullName">Nombre completo</label>
        <input id="fullName" name="fullName" type="text" maxlength="100" autofocus [(ngModel)]="fullName"
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
        <label for="password">Contraseña</label>
        <input id="password" name="password" type="password" maxlength="72" autocomplete="new-password"
               [(ngModel)]="password" [disabled]="saving()" aria-describedby="password-error" />
        @if (passwordError()) { <p id="password-error" class="field-error">{{ passwordError() }}</p> }
      </div>
      <fieldset class="field radios">
        <legend>Rol</legend>
        @for (option of roleOptions; track option) {
          <label class="check">
            <input type="radio" name="role" [value]="option" [(ngModel)]="role" [disabled]="saving()" />
            {{ labels[option] }}
          </label>
        }
      </fieldset>
      <div class="form__actions">
        <a class="btn btn-secondary" routerLink="/users" [class.disabled]="saving()">Cancelar</a>
        <button type="submit" class="btn btn-primary" [disabled]="saving()">
          @if (saving()) { <span class="spinner" aria-hidden="true"></span> }
          Guardar
        </button>
      </div>
    </form>
  `,
})
export class CreateUser {
  private readonly service = inject(UsersService);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);

  protected readonly labels = ROLE_LABELS;
  protected readonly roleOptions: Role[] = ['ADMIN', 'TRAINER'];
  protected fullName = '';
  protected username = '';
  protected password = '';
  protected role: Role = 'TRAINER';
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly fullNameError = signal<string | null>(null);
  protected readonly usernameError = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);

  protected submit(): void {
    this.formError.set(null);
    this.fullNameError.set(this.fullName.trim() ? null : 'Ingresa el nombre completo.');
    this.usernameError.set(this.username.trim() ? null : 'Ingresa el nombre de usuario.');
    this.passwordError.set(
      this.password.length >= 8 && this.password.length <= 72
        ? null
        : 'La contraseña debe tener entre 8 y 72 caracteres.',
    );
    if (this.fullNameError() || this.usernameError() || this.passwordError()) {
      return;
    }
    this.saving.set(true);
    this.service
      .create({ fullName: this.fullName.trim(), username: this.username.trim(), password: this.password, role: this.role })
      .subscribe({
        next: () => {
          this.notice.show('Usuario creado.');
          void this.router.navigate(['/users']);
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.password = '';
          if (errorCode(error) === 'USERNAME_ALREADY_TAKEN') {
            this.usernameError.set('Ya existe un usuario con ese nombre. Elige otro.');
          } else {
            this.formError.set(sharedMessage(error));
          }
        },
      });
  }
}
