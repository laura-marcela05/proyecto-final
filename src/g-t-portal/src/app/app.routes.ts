import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { roleGuard } from './core/role.guard';

/** Every route and the guard that protects it. Only /login is open without a token. */
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login').then((m) => m.Login) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'students' },
      { path: 'students', loadComponent: () => import('./students/student-list').then((m) => m.StudentList) },
      {
        path: 'students/new',
        canActivate: [roleGuard('TRAINER')],
        loadComponent: () => import('./students/register-student').then((m) => m.RegisterStudent),
      },
      {
        path: 'students/:studentId/edit',
        canActivate: [roleGuard('TRAINER')],
        loadComponent: () => import('./students/edit-student').then((m) => m.EditStudent),
      },
      {
        path: 'students/:studentId/history',
        loadComponent: () => import('./sessions/session-history').then((m) => m.SessionHistory),
      },
      {
        path: 'students/:studentId',
        canActivate: [roleGuard('TRAINER')],
        loadComponent: () => import('./sessions/student-session').then((m) => m.StudentSession),
      },
      {
        path: 'users',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./users/user-list').then((m) => m.UserList),
      },
      {
        path: 'users/new',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./users/create-user').then((m) => m.CreateUser),
      },
      {
        path: 'users/:userId/edit',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./users/edit-user').then((m) => m.EditUser),
      },
    ],
  },
  { path: '**', redirectTo: 'students' },
];
