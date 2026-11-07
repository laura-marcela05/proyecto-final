import { Component, input, output } from '@angular/core';

/** "¿Eliminar …? Esta acción no se puede deshacer." Nothing is sent until "Eliminar". */
@Component({
  selector: 'app-confirm-dialog',
  template: `
    <div class="dialog-backdrop">
      <div class="dialog card" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <p id="confirm-title">¿Eliminar {{ subject() }}? Esta acción no se puede deshacer.</p>
        <div class="dialog__actions">
          <button type="button" class="btn btn-secondary" (click)="cancelled.emit()">Cancelar</button>
          <button type="button" class="btn btn-danger" (click)="confirmed.emit()">Eliminar</button>
        </div>
      </div>
    </div>
  `,
})
export class ConfirmDialog {
  readonly subject = input.required<string>();
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();
}
