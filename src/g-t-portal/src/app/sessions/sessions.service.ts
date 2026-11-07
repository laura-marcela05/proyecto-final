import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { MuscleGroup, Session, SessionStatus } from '../core/models';

@Injectable({ providedIn: 'root' })
export class SessionsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listByStudent(studentId: string, status?: SessionStatus): Observable<Session[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Session[]>(`${this.api}/students/${studentId}/sessions`, { params });
  }

  checkIn(studentId: string): Observable<Session> {
    return this.http.post<Session>(`${this.api}/students/${studentId}/sessions`, null);
  }

  logMuscleGroup(sessionId: string, group: MuscleGroup): Observable<Session> {
    return this.http.put<Session>(`${this.api}/sessions/${sessionId}/muscle-groups/${group}`, null);
  }

  checkOut(sessionId: string): Observable<Session> {
    return this.http.patch<Session>(`${this.api}/sessions/${sessionId}`, { status: 'closed' });
  }

  remove(sessionId: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/sessions/${sessionId}`);
  }
}
