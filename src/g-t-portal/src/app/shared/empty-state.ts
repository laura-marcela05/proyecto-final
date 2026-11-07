import { Component, input, output } from '@angular/core';

/** A message plus the one action that fills the list, when the role has one. */
@Component({
  selector: 'app-empty-state',
  template: `
    <div class="card empty-state">
      <p>{{ message() }}</p>
      @if (actionLabel()) {
        <button type="button" class="btn btn-primary" (click)="action.emit()">{{ actionLabel() }}</button>
      }
    </div>
  `,
})
export class EmptyState {
  readonly message = input.required<string>();
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
