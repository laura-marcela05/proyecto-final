import { Injectable, signal } from '@angular/core';

/** A message shown once at the top of the next screen: "Usuario creado.", "Tu acceso expiró...", and so on. */
@Injectable({ providedIn: 'root' })
export class NoticeService {
  readonly message = signal<string | null>(null);
  private timer: ReturnType<typeof setTimeout> | undefined;

  show(text: string): void {
    this.message.set(text);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.message.set(null), 6000);
  }

  dismiss(): void {
    clearTimeout(this.timer);
    this.message.set(null);
  }
}
