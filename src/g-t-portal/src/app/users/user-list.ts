import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { errorCode, sharedMessage } from '../core/error-messages';
import { ROLE_LABELS, User } from '../core/models';
import { NoticeService } from '../core/notice.service';
import { ConfirmDialog } from '../shared/confirm-dialog';
import { PageHeader } from '../shared/page-header';
import { RequestError } from '../shared/request-error';
import { SkeletonRows } from '../shared/skeleton-rows';
import { UsersService } from './users.service';

@Component({
  selector: 'app-user-list',
  imports: [RouterLink, PageHeader, RequestError, SkeletonRows, ConfirmDialog],
  template: `
    <app-page-header title="Usuarios" />
    <div class="toolbar">
      <a class="btn btn-primary" routerLink="/users/new">Crear usuario</a>
    </div>
    @switch (state()) {
      @case ('loading') { <app-skeleton-rows /> }
      @case ('error') { <app-request-error message="No se pudieron cargar los usuarios." (retry)="load()" /> }
      @default {
        <table class="card table">
          <thead>
            <tr>
              <th scope="col">Nombre completo</th>
              <th scope="col">Usuario</th>
              <th scope="col">Rol</th>
              <th scope="col"><span class="visually-hidden">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            @for (user of users(); track user.id) {
              <tr>
                <td>{{ user.fullName }}</td>
                <td>{{ user.username }}</td>
                <td>{{ roles[user.role] }}</td>
                <td class="actions">
                  <a class="btn btn-secondary" [routerLink]="['/users', user.id, 'edit']">Editar</a>
                  @if (user.id !== ownId) {
                    <button type="button" class="btn btn-danger" (click)="pendingDelete.set(user)">Eliminar</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    }
    @if (pendingDelete(); as user) {
      <app-confirm-dialog [subject]="'a ' + user.fullName" (confirmed)="remove(user)"
                          (cancelled)="pendingDelete.set(null)" />
    }
  `,
})
export class UserList implements OnInit {
  private readonly service = inject(UsersService);
  private readonly notice = inject(NoticeService);
  protected readonly ownId = inject(AuthService).userId();

  protected readonly roles = ROLE_LABELS;
  protected readonly users = signal<User[]>([]);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly pendingDelete = signal<User | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.service.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected remove(user: User): void {
    this.pendingDelete.set(null);
    this.service.remove(user.id).subscribe({
      next: () => {
        this.users.update((list) => list.filter((u) => u.id !== user.id));
        this.notice.show('Usuario eliminado.');
      },
      error: (error: unknown) => {
        const messages: Record<string, string> = {
          USER_HAS_STUDENTS: 'Este entrenador tiene estudiantes. No se puede eliminar.',
          CANNOT_DELETE_OWN_USER: 'No puedes eliminar tu propio usuario.',
        };
        const message = messages[errorCode(error) ?? ''] ?? sharedMessage(error);
        if (message) {
          this.notice.show(message);
        }
      },
    });
  }
}
