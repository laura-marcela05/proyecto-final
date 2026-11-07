import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { SHARED, errorCode } from '../core/error-messages';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <section class="card login-card">
      <h1 class="login-title">Gym Tracker</h1>
      @if (formError()) {
        <p class="error-text" role="alert">{{ formError() }}</p>
      }
      <form (ngSubmit)="submit()" novalidate>
        <div class="field">
          <label for="username">Usuario</label>
          <input id="username" name="username" type="text" maxlength="50" autofocus autocomplete="username"
                 [(ngModel)]="username" [disabled]="loading()" [attr.aria-invalid]="usernameError() ? 'true' : null"
                 aria-describedby="username-error" />
          @if (usernameError()) {
            <p id="username-error" class="field-error">{{ usernameError() }}</p>
          }
        </div>
        <div class="field">
          <label for="password">Contraseña</label>
          <input id="password" name="password" type="password" maxlength="72" autocomplete="current-password"
                 [(ngModel)]="password" [disabled]="loading()" [attr.aria-invalid]="passwordError() ? 'true' : null"
                 aria-describedby="password-error" />
          @if (passwordError()) {
            <p id="password-error" class="field-error">{{ passwordError() }}</p>
          }
        </div>
        <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
          @if (loading()) { <span class="spinner" aria-hidden="true"></span> }
          Ingresar
        </button>
      </form>
    </section>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected username = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly usernameError = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);

  protected submit(): void {
    this.formError.set(null);
    this.usernameError.set(this.username.trim() ? null : 'Ingresa tu usuario.');
    this.passwordError.set(this.password ? null : 'Ingresa tu contraseña.');
    if (this.usernameError() || this.passwordError()) {
      return;
    }
    this.loading.set(true);
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: () => void this.router.navigate(['/students']),
      error: (error: unknown) => {
        this.loading.set(false);
        this.password = '';
        this.formError.set(
          errorCode(error) === 'INVALID_CREDENTIALS' ? 'Usuario o contraseña incorrectos.' : SHARED.generic,
        );
      },
    });
  }
}
