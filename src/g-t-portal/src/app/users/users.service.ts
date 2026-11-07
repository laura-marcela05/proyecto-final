import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User, UserCreate, UserUpdate } from '../core/models';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/users`;

  list(): Observable<User[]> {
    return this.http.get<User[]>(this.url);
  }

  get(userId: string): Observable<User> {
    return this.http.get<User>(`${this.url}/${userId}`);
  }

  create(input: UserCreate): Observable<User> {
    return this.http.post<User>(this.url, input);
  }

  update(userId: string, input: UserUpdate): Observable<User> {
    return this.http.put<User>(`${this.url}/${userId}`, input);
  }

  remove(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${userId}`);
  }
}
