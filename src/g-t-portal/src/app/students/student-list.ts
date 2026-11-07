import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { errorCode, sharedMessage } from '../core/error-messages';
import { Student } from '../core/models';
import { NoticeService } from '../core/notice.service';
import { ConfirmDialog } from '../shared/confirm-dialog';
import { EmptyState } from '../shared/empty-state';
import { PageHeader } from '../shared/page-header';
import { RequestError } from '../shared/request-error';
import { SkeletonRows } from '../shared/skeleton-rows';
import { StudentsService } from './students.service';

@Component({
  selector: 'app-student-list',
  imports: [RouterLink, PageHeader, EmptyState, RequestError, SkeletonRows, ConfirmDialog],
  template: `
    <app-page-header [title]="title()" />
    @if (isTrainer() && state() === 'ready' && students().length > 0) {
      <div class="toolbar">
        <a class="btn btn-primary" routerLink="/students/new">Registrar estudiante</a>
      </div>
    }
    @switch (state()) {
      @case ('loading') { <app-skeleton-rows /> }
      @case ('error') {
        <app-request-error message="No se pudieron cargar los estudiantes." (retry)="load()" />
      }
      @default {
        @if (students().length === 0) {
          <app-empty-state
            [message]="isTrainer() ? 'Aún no tienes estudiantes registrados.' : 'Aún no hay estudiantes registrados.'"
            [actionLabel]="isTrainer() ? 'Registrar estudiante' : null"
            (action)="register()" />
        } @else {
          <table class="card table">
            <thead>
              <tr>
                <th scope="col">Nombre</th>
                <th scope="col">Documento</th>
                @if (!isTrainer()) { <th scope="col">Entrenador</th> }
                <th scope="col"><span class="visually-hidden">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (student of students(); track student.id) {
                <tr class="row-link" (click)="open(student)">
                  <td><a [routerLink]="target(student)" (click)="$event.stopPropagation()">{{ student.name }}</a></td>
                  <td>Doc. {{ student.document }}</td>
                  @if (!isTrainer()) { <td>{{ student.trainerName }}</td> }
                  <td class="actions">
                    @if (isTrainer()) {
                      <a class="btn btn-secondary" [routerLink]="['/students', student.id, 'edit']"
                         (click)="$event.stopPropagation()">Editar</a>
                    } @else {
                      <button type="button" class="btn btn-danger"
                              (click)="$event.stopPropagation(); pendingDelete.set(student)">Eliminar</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      }
    }
    @if (pendingDelete(); as student) {
      <app-confirm-dialog [subject]="'a ' + student.name" (confirmed)="remove(student)"
                          (cancelled)="pendingDelete.set(null)" />
    }
  `,
})
export class StudentList implements OnInit {
  private readonly service = inject(StudentsService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);

  protected readonly students = signal<Student[]>([]);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly pendingDelete = signal<Student | null>(null);
  protected readonly isTrainer = computed(() => this.auth.role() === 'TRAINER');
  protected readonly title = computed(() => (this.isTrainer() ? 'Mis estudiantes' : 'Estudiantes'));

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.service.list().subscribe({
      next: (students) => {
        this.students.set(students);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  /** TRAINER opens Student session; ADMIN opens Session history. */
  protected target(student: Student): string[] {
    return this.isTrainer() ? ['/students', student.id] : ['/students', student.id, 'history'];
  }

  protected open(student: Student): void {
    void this.router.navigate(this.target(student));
  }

  protected register(): void {
    void this.router.navigate(['/students/new']);
  }

  protected remove(student: Student): void {
    this.pendingDelete.set(null);
    this.service.remove(student.id).subscribe({
      next: () => {
        this.students.update((list) => list.filter((s) => s.id !== student.id));
        this.notice.show('Estudiante eliminado.');
      },
      error: (error: unknown) => {
        const message =
          errorCode(error) === 'STUDENT_HAS_SESSIONS'
            ? 'Este estudiante tiene sesiones. Elimínalas primero desde su historial.'
            : sharedMessage(error);
        if (message) {
          this.notice.show(message);
        }
      },
    });
  }
}
