import type { Role } from './auth/claims';

export interface PortalModule {
  readonly id: 'hr' | 'facilities' | 'projects' | 'governance';
  readonly path: string;
  readonly roles: readonly Role[];
  readonly phase: number;
}

export const MODULES: readonly PortalModule[] = [
  { id: 'hr', path: '/hr', roles: ['hr-officer'], phase: 2 },
  { id: 'facilities', path: '/facilities', roles: ['facilities-officer'], phase: 2 },
  { id: 'projects', path: '/projects', roles: ['projects-officer'], phase: 3 },
  { id: 'governance', path: '/governance', roles: ['governance-officer'], phase: 1 },
];
