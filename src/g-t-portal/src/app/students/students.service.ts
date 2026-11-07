import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Student, StudentInput } from '../core/models';

@Injectable({ providedIn: 'root' })
export class StudentsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/students`;

  list(): Observable<Student[]> {
    return this.http.get<Student[]>(this.url);
  }

  get(studentId: string): Observable<Student> {
    return this.http.get<Student>(`${this.url}/${studentId}`);
  }

  create(input: StudentInput): Observable<Student> {
    return this.http.post<Student>(this.url, input);
  }

  update(studentId: string, input: StudentInput): Observable<Student> {
    return this.http.put<Student>(`${this.url}/${studentId}`, input);
  }

  remove(studentId: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/${studentId}`);
  }
}
