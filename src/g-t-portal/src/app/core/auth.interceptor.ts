import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { SHARED } from './error-messages';
import { NoticeService } from './notice.service';

/** Adds the bearer token, and answers 401 and 403 the same way on every screen (design-system.md). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notice = inject(NoticeService);

  const isApi = req.url.startsWith(environment.apiUrl);
  const token = auth.token();
  const request = isApi && token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const isLogin = req.url.endsWith('/auth/login');
      if (isApi && !isLogin && error.status === 401) {
        auth.logout();
        notice.show(SHARED.expired);
        void router.navigate(['/login']);
      } else if (isApi && !isLogin && error.status === 403) {
        notice.show(SHARED.forbidden);
      }
      return throwError(() => error);
    }),
  );
};
