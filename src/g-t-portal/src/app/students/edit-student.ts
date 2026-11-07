import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorCode, sharedMessage } from '../core/error-messages';
import { NoticeService } from '../core/notice.service';
import { PageHeader } from '../shared/page-header';
import { RequestError } from '../shared/request-error';
import { SkeletonRows } from '../shared/skeleton-rows';
import { StudentsService } from './students.service';

@Component({
  selector: 'app-edit-student',
  imports: [FormsModule, RouterLink, PageHeader, RequestError, SkeletonRows],
  template: `
    <app-page-header title="Editar estudiante" backLink="/students" backLabel="Mis estudiantes" />
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
        <form class="card form" (ngSubmit)="submit()" novalidate>
          @if (formError()) {
            <p class="error-text" role="alert">{{ formError() }}</p>
          }
          <div class="field">
            <label for="name">Nombre</label>
            <input id="name" name="name" type="text" maxlength="100" [(ngModel)]="name"
                   [disabled]="saving()" aria-describedby="name-error" />
            @if (nameError()) { <p id="name-error" class="field-error">{{ nameError() }}</p> }
          </div>
          <div class="field">
            <label for="document">Número de documento</label>
            <input id="document" name="document" type="text" maxlength="20" [(ngModel)]="document"
                   [disabled]="saving()" aria-describedby="document-error" />
            @if (documentError()) { <p id="document-error" class="field-error">{{ documentError() }}</p> }
          </div>
          <div class="form__actions">
            <a class="btn btn-secondary" routerLink="/students" [class.disabled]="saving()">Cancelar</a>
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
export class EditStudent implements OnInit {
  private readonly service = inject(StudentsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  private readonly studentId = this.route.snapshot.paramMap.get('studentId') ?? '';

  protected name = '';
  protected document = '';
  protected readonly state = signal<'loading' | 'ready' | 'error' | 'notfound'>('loading');
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly nameError = signal<string | null>(null);
  protected readonly documentError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.service.get(this.studentId).subscribe({
      next: (student) => {
        this.name = student.name;
        this.document = student.document;
        this.state.set('ready');
      },
      error: (error: unknown) => this.state.set(errorCode(error) === 'STUDENT_NOT_FOUND' ? 'notfound' : 'error'),
    });
  }

  protected submit(): void {
    this.formError.set(null);
    this.nameError.set(this.name.trim() ? null : 'Ingresa el nombre del estudiante.');
    this.documentError.set(this.document.trim() ? null : 'Ingresa el número de documento.');
    if (this.nameError() || this.documentError()) {
      return;
    }
    this.saving.set(true);
    this.service.update(this.studentId, { name: this.name.trim(), document: this.document.trim() }).subscribe({
      next: () => {
        this.notice.show('Estudiante actualizado.');
        void this.router.navigate(['/students']);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const code = errorCode(error);
        if (code === 'DOCUMENT_ALREADY_REGISTERED') {
          this.documentError.set('Ya existe un estudiante con este número de documento. Revisa el número.');
        } else if (code === 'STUDENT_NOT_FOUND') {
          this.state.set('notfound');
        } else {
          this.formError.set(sharedMessage(error));
        }
      },
    });
  }
}
