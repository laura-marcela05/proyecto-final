import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, tap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { errorCode, sharedMessage } from '../core/error-messages';
import { MUSCLE_LABELS, Session, Student } from '../core/models';
import { NoticeService } from '../core/notice.service';
import { ConfirmDialog } from '../shared/confirm-dialog';
import { EmptyState } from '../shared/empty-state';
import { PageHeader } from '../shared/page-header';
import { RequestError } from '../shared/request-error';
import { SkeletonRows } from '../shared/skeleton-rows';
import { StudentsService } from '../students/students.service';
import { SessionsService } from './sessions.service';

@Component({
  selector: 'app-session-history',
  imports: [RouterLink, DatePipe, PageHeader, EmptyState, RequestError, SkeletonRows, ConfirmDialog],
  template: `
    <app-page-header title="Historial de sesiones" [backLink]="backLink()" [backLabel]="student()?.name ?? ''" />
    @switch (state()) {
      @case ('loading') { <app-skeleton-rows /> }
      @case ('error') { <app-request-error message="No se pudo cargar el historial." (retry)="load()" /> }
      @case ('notfound') {
        <div class="card">
          <p class="error-text">Este estudiante no existe.</p>
          <a routerLink="/students">{{ isAdmin() ? 'Volver a estudiantes' : 'Volver a mis estudiantes' }}</a>
        </div>
      }
      @default {
        @if (sessions().length === 0) {
          <app-empty-state message="Este estudiante aún no tiene sesiones registradas."
                           [actionLabel]="isAdmin() ? null : 'Ir a registrar entrada'" (action)="goToSession()" />
        } @else {
          <table class="card table">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Entrada</th>
                <th scope="col">Salida</th>
                <th scope="col">Duración</th>
                <th scope="col">Grupos musculares</th>
                @if (isAdmin()) { <th scope="col"><span class="visually-hidden">Acciones</span></th> }
              </tr>
            </thead>
            <tbody>
              @for (session of sessions(); track session.id) {
                <tr>
                  <td>{{ session.checkInAt | date: 'dd/MM/yyyy' }}</td>
                  <td>{{ session.checkInAt | date: 'h:mm a' }}</td>
                  <td>{{ session.checkOutAt ? (session.checkOutAt | date: 'h:mm a') : 'En curso' }}</td>
                  <td>{{ session.durationMinutes !== null ? session.durationMinutes + ' min' : '—' }}</td>
                  <td>{{ groupNames(session) }}</td>
                  @if (isAdmin()) {
                    <td class="actions">
                      <button type="button" class="btn btn-danger" (click)="pendingDelete.set(session)">Eliminar</button>
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        }
      }
    }
    @if (pendingDelete(); as session) {
      <app-confirm-dialog subject="esta sesión" (confirmed)="remove(session)" (cancelled)="pendingDelete.set(null)" />
    }
  `,
})
export class SessionHistory implements OnInit {
  private readonly students = inject(StudentsService);
  private readonly service = inject(SessionsService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  private readonly studentId = inject(ActivatedRoute).snapshot.paramMap.get('studentId') ?? '';

  protected readonly student = signal<Student | null>(null);
  protected readonly sessions = signal<Session[]>([]);
  protected readonly state = signal<'loading' | 'ready' | 'error' | 'notfound'>('loading');
  protected readonly pendingDelete = signal<Session | null>(null);
  protected readonly isAdmin = computed(() => this.auth.role() === 'ADMIN');
  /** TRAINER goes back to Student session; ADMIN to Student list. */
  protected readonly backLink = computed(() => (this.isAdmin() ? '/students' : `/students/${this.studentId}`));

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    forkJoin({
      // The header shows the student's name as soon as it arrives, while the table still loads.
      student: this.students.get(this.studentId).pipe(tap((student) => this.student.set(student))),
      sessions: this.service.listByStudent(this.studentId),
    })
      .subscribe({
        next: ({ student, sessions }) => {
          this.student.set(student);
          this.sessions.set(sessions);
          this.state.set('ready');
        },
        error: (error: unknown) => this.state.set(errorCode(error) === 'STUDENT_NOT_FOUND' ? 'notfound' : 'error'),
      });
  }

  protected groupNames(session: Session): string {
    return session.muscleGroups.map((group) => MUSCLE_LABELS[group]).join(', ');
  }

  protected goToSession(): void {
    void this.router.navigate(['/students', this.studentId]);
  }

  protected remove(session: Session): void {
    this.pendingDelete.set(null);
    this.service.remove(session.id).subscribe({
      next: () => {
        this.sessions.update((list) => list.filter((s) => s.id !== session.id));
        this.notice.show('Sesión eliminada.');
      },
      error: (error: unknown) => {
        if (errorCode(error) === 'SESSION_NOT_FOUND') {
          this.notice.show('Esta sesión ya no existe.');
          this.load();
          return;
        }
        const message = sharedMessage(error);
        if (message) {
          this.notice.show(message);
        }
      },
    });
  }
}
