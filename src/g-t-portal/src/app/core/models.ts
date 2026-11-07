export type Role = 'ADMIN' | 'TRAINER';
export type MuscleGroup = 'chest' | 'back' | 'legs' | 'arms' | 'shoulders' | 'core';
export type SessionStatus = 'open' | 'closed';

export const MUSCLE_GROUPS: readonly MuscleGroup[] = ['chest', 'back', 'legs', 'arms', 'shoulders', 'core'];

export const MUSCLE_LABELS: Record<MuscleGroup, string> = {
  chest: 'Pecho',
  back: 'Espalda',
  legs: 'Piernas',
  arms: 'Brazos',
  shoulders: 'Hombros',
  core: 'Core',
};

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrador',
  TRAINER: 'Entrenador',
};

export interface LoginResponse {
  token: string;
  role: Role;
  fullName: string;
}

export interface User {
  id: string;
  username: string;
  fullName: string;
  role: Role;
}

export interface UserCreate {
  fullName: string;
  username: string;
  password: string;
  role: Role;
}

export interface UserUpdate {
  fullName: string;
  username: string;
  password?: string;
}

export interface Student {
  id: string;
  name: string;
  document: string;
  trainerId: string;
  trainerName: string;
}

export interface StudentInput {
  name: string;
  document: string;
}

export interface Session {
  id: string;
  studentId: string;
  status: SessionStatus;
  checkInAt: string;
  checkOutAt: string | null;
  durationMinutes: number | null;
  muscleGroups: MuscleGroup[];
}
