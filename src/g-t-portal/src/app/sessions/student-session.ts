import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';
import { errorCode, sharedMessage } from '../core/error-messages';
import { MUSCLE_GROUPS, MUSCLE_LABELS, MuscleGroup, Session, Student } from '../core/models';
import { PageHeader } from '../shared/page-header';
import { RequestError } from '../shared/request-error';
import { SkeletonRows } from '../shared/skeleton-rows';
import { StudentsService } from '../students/students.service';
import { SessionsService } from './sessions.service';

@Component({
  selector: 'app-student-session',
  imports: [RouterLink, DatePipe, PageHeader, RequestError, SkeletonRows],
  template: `
    <app-page-header [title]="student()?.name ?? 'Sesión'" backLink="/students" backLabel="Mis estudiantes" />
    @switch (state()) {
      @case ('loading') { <app-skeleton-rows /> }
      @case ('error') { <app-request-error message="No se pudo cargar el estudiante." (retry)="load()" /> }
      @case ('notfound') {
        <div class="card">
          <p class="error-text">Este estudiante no existe.</p>
          <a routerLink="/students">Volver a mis estudiantes</a>
        </div>
      }
      @default {
        <div class="card student-bar">
          <p>Doc. {{ student()?.document }}</p>
          <a class="btn btn-secondary" [routerLink]="['/students', studentId, 'history']">Ver historial</a>
        </div>
        @if (actionMessage()) {
          <p class="error-text" role="alert">{{ actionMessage() }}</p>
        }
        @if (session(); as current) {
          <section class="card">
            <p><strong>Sesión en curso</strong> — Entrada: {{ current.checkInAt | date: 'h:mm a' }}</p>
            <fieldset class="checklist" [disabled]="busy() !== null">
              <legend>Grupos musculares trabajados</legend>
              @for (group of groups; track group) {
                <label class="check">
                  <input type="checkbox" [checked]="isLogged(group)" [disabled]="isLogged(group) || busy() !== null"
                         (change)="log(current, group, $event)" />
                  {{ labels[group] }}
                </label>
              }
            </fieldset>
            @if (current.muscleGroups.length === 0) {
              <p class="muted">Marca al menos un grupo muscular para registrar la salida.</p>
            }
            <div class="form__actions">
              <button type="button" class="btn btn-primary" [disabled]="current.muscleGroups.length === 0 || busy() !== null"
                      (click)="checkOut(current)">
                @if (busy() === 'checkout') { <span class="spinner" aria-hidden="true"></span> }
                Registrar salida
              </button>
            </div>
          </section>
        } @else {
          <section class="card">
            @if (closedSummary()) {
              <p class="success-text" role="status">{{ closedSummary() }}</p>
            }
            <p>Sin sesión abierta.</p>
            <button type="button" class="btn btn-primary" [disabled]="busy() !== null" (click)="checkIn()">
              @if (busy() === 'checkin') { <span class="spinner" aria-hidden="true"></span> }
              Registrar entrada
            </button>
          </section>
        }
      }
    }
  `,
})
export class StudentSession implements OnInit {
  private readonly students = inject(StudentsService);
  private readonly sessions = inject(SessionsService);
  protected readonly studentId = inject(ActivatedRoute).snapshot.paramMap.get('studentId') ?? '';

  protected readonly groups = MUSCLE_GROUPS;
  protected readonly labels = MUSCLE_LABELS;
  protected readonly student = signal<Student | null>(null);
  protected readonly session = signal<Session | null>(null);
  protected readonly state = signal<'loading' | 'ready' | 'error' | 'notfound'>('loading');
  /** The action in flight, if any: only its control shows a spinner. */
  protected readonly busy = signal<'checkin' | 'log' | 'checkout' | null>(null);
  protected readonly actionMessage = signal<string | null>(null);
  protected readonly closedSummary = signal<string | null>(null);
  private readonly logged = computed(() => new Set<MuscleGroup>(this.session()?.muscleGroups ?? []));

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.actionMessage.set(null);
    forkJoin({
      student: this.students.get(this.studentId),
      open: this.sessions.listByStudent(this.studentId, 'open'),
    }).subscribe({
      next: ({ student, open }) => {
        this.student.set(student);
        this.session.set(open[0] ?? null);
        this.state.set('ready');
      },
      error: (error: unknown) => this.state.set(errorCode(error) === 'STUDENT_NOT_FOUND' ? 'notfound' : 'error'),
    });
  }

  protected isLogged(group: MuscleGroup): boolean {
    return this.logged().has(group);
  }

  protected checkIn(): void {
    this.run('checkin', this.sessions.checkIn(this.studentId), (session) => {
      this.closedSummary.set(null);
      this.session.set(session);
    });
  }

  /** A group is checked only once the API answers 200; on failure the box goes back to unchecked. */
  protected log(current: Session, group: MuscleGroup, event: Event): void {
    const box = event.target as HTMLInputElement;
    this.run(
      'log',
      this.sessions.logMuscleGroup(current.id, group),
      (session) => this.session.set(session),
      () => (box.checked = false),
    );
  }

  protected checkOut(current: Session): void {
    this.run('checkout', this.sessions.checkOut(current.id), (closed) => {
      const names = current.muscleGroups.map((g) => MUSCLE_LABELS[g]).join(', ');
      this.closedSummary.set(`Salida registrada: ${closed.durationMinutes} min · ${names}`);
      this.session.set(null);
    });
  }

  private run<T>(
    action: 'checkin' | 'log' | 'checkout',
    request: Observable<T>,
    onSuccess: (value: T) => void,
    onFailure?: () => void,
  ): void {
    this.busy.set(action);
    this.actionMessage.set(null);
    request.subscribe({
      next: (value) => {
        this.busy.set(null);
        onSuccess(value);
      },
      error: (error: unknown) => {
        this.busy.set(null);
        onFailure?.();
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.load();
          this.actionMessage.set('La sesión cambió mientras la usabas. Se actualizó la pantalla.');
        } else {
          this.actionMessage.set(sharedMessage(error));
        }
      },
    });
  }
}
