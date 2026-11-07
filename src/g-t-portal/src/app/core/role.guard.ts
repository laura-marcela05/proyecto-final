import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';

/** A screen of the other role sends the user back to Student list. */
export const roleGuard = (allowed: Role): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.role() === allowed ? true : router.createUrlTree(['/students']);
};
