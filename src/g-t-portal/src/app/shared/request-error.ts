import { Component, input, output } from '@angular/core';

/** A failed load: the message and "Reintentar", which repeats the request. */
@Component({
  selector: 'app-request-error',
  template: `
    <div class="card request-error" role="alert">
      <p class="error-text">{{ message() }}</p>
      <button type="button" class="btn btn-secondary" (click)="retry.emit()">Reintentar</button>
    </div>
  `,
})
export class RequestError {
  readonly message = input.required<string>();
  readonly retry = output<void>();
}
