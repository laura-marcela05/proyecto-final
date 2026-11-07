import { HttpErrorResponse } from '@angular/common/http';

/** Shared text of design-system.md. */
export const SHARED = {
  expired: 'Tu acceso expiró. Ingresa de nuevo.',
  forbidden: 'No tienes permiso para esta acción.',
  generic: 'No se pudo completar la acción. Intenta de nuevo.',
};

/** The `error` code of the API's error body, when the response has one. */
export function errorCode(error: unknown): string | undefined {
  return error instanceof HttpErrorResponse ? (error.error?.error as string | undefined) : undefined;
}

/**
 * The shared message for a failure a screen has no text of its own for.
 * 401 and 403 return null: the interceptor already answered them for every screen.
 */
export function sharedMessage(error: unknown): string | null {
  if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
    return null;
  }
  return SHARED.generic;
}
