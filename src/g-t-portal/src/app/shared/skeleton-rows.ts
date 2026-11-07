import { Component } from '@angular/core';

/** Five placeholder rows while a list loads; the header stays usable. */
@Component({
  selector: 'app-skeleton-rows',
  template: `
    <div class="card" aria-busy="true" aria-label="Cargando">
      @for (row of rows; track row) {
        <div class="skeleton"></div>
      }
    </div>
  `,
})
export class SkeletonRows {
  protected readonly rows = [1, 2, 3, 4, 5];
}
