import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { errorCode, sharedMessage } from '../core/error-messages';
import { NoticeService } from '../core/notice.service';
import { PageHeader } from '../shared/page-header';
import { StudentsService } from './students.service';

@Component({
  selector: 'app-register-student',
  imports: [FormsModule, RouterLink, PageHeader],
  template: `
    <app-page-header title="Registrar estudiante" backLink="/students" backLabel="Mis estudiantes" />
    <form class="card form" (ngSubmit)="submit()" novalidate>
      @if (formError()) {
        <p class="error-text" role="alert">{{ formError() }}</p>
      }
      <div class="field">
        <label for="name">Nombre</label>
        <input id="name" name="name" type="text" maxlength="100" autofocus [(ngModel)]="name"
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
  `,
})
export class RegisterStudent {
  private readonly service = inject(StudentsService);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);

  protected name = '';
  protected document = '';
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly nameError = signal<string | null>(null);
  protected readonly documentError = signal<string | null>(null);

  protected submit(): void {
    this.formError.set(null);
    this.nameError.set(this.name.trim() ? null : 'Ingresa el nombre del estudiante.');
    this.documentError.set(this.document.trim() ? null : 'Ingresa el número de documento.');
    if (this.nameError() || this.documentError()) {
      return;
    }
    this.saving.set(true);
    this.service.create({ name: this.name.trim(), document: this.document.trim() }).subscribe({
      next: (student) => {
        this.notice.show('Estudiante registrado.');
        void this.router.navigate(['/students', student.id]);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        if (errorCode(error) === 'DOCUMENT_ALREADY_REGISTERED') {
          this.documentError.set('Ya existe un estudiante con este número de documento. Revisa el número.');
        } else {
          this.formError.set(sharedMessage(error));
        }
      },
    });
  }
}
