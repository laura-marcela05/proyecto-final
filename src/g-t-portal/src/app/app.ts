import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NoticeService } from './core/notice.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `
    @if (notice.message(); as text) {
      <div class="notice" role="status" (click)="notice.dismiss()">{{ text }}</div>
    }
    <main class="container">
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly notice = inject(NoticeService);
}
