import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse, Role } from './models';

const STORAGE_KEY = 'gym-tracker.session';

/** The token, the role and the full name, kept in localStorage until the token expires or the user leaves. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<LoginResponse | null>(this.read());

  readonly role = computed<Role | null>(() => this.session()?.role ?? null);
  readonly fullName = computed(() => this.session()?.fullName ?? '');
  readonly token = computed(() => this.session()?.token ?? null);

  login(username: string, password: string): Observable<void> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password }).pipe(
      tap((response) => this.store(response)),
      map(() => undefined),
    );
  }

  /** There is no logout endpoint: leaving is discarding the stored session. */
  logout(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Storage unavailable: the in-memory session below is still cleared.
    }
    this.session.set(null);
  }

  isLoggedIn(): boolean {
    const current = this.session();
    if (!current) {
      return false;
    }
    if (this.isExpired(current.token)) {
      this.logout();
      return false;
    }
    return true;
  }

  /** The user id: the `sub` claim of the token. */
  userId(): string | null {
    const token = this.token();
    return token ? (this.payload(token)?.['sub'] as string | undefined) ?? null : null;
  }

  private store(response: LoginResponse): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    } catch {
      // Storage unavailable: the session lives only until the page is reloaded.
    }
    this.session.set(response);
  }

  private read(): LoginResponse | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as LoginResponse) : null;
    } catch {
      return null;
    }
  }

  private isExpired(token: string): boolean {
    const exp = this.payload(token)?.['exp'];
    return typeof exp !== 'number' || exp * 1000 <= Date.now();
  }

  private payload(token: string): Record<string, unknown> | null {
    try {
      const body = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(body)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}
