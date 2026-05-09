import axios from 'axios';
import type {
  Project, Sprint, BacklogItem, TeamMember,
  StoryPointEstimate, VelocityPrediction, SprintPlanResult,
  SprintHealth, SprintReadiness, ComplexityScore,
  BurndownResponse, VelocityHistoryPoint, ProjectSummary, RollingVelocity,
  ProjectRequest, SprintRequest, BacklogItemRequest, VelocityPredictionRequest,
  EstimationOverrideRequest,
} from './types';

const api = axios.create({ baseURL: '/api/v1', headers: { 'Content-Type': 'application/json' } });

// ─── Projects ─────────────────────────────────────────────────────────────────
export const projectsApi = {
  list: () => api.get<Project[]>('/projects').then(r => r.data),
  get: (id: number) => api.get<Project>(`/projects/${id}`).then(r => r.data),
  create: (req: ProjectRequest) => api.post<Project>('/projects', req).then(r => r.data),
  update: (id: number, req: Partial<ProjectRequest>) => api.put<Project>(`/projects/${id}`, req).then(r => r.data),
  delete: (id: number) => api.delete(`/projects/${id}`),
};

// ─── Sprints ──────────────────────────────────────────────────────────────────
export const sprintsApi = {
  listByProject: (projectId: number) => api.get<Sprint[]>(`/projects/${projectId}/sprints`).then(r => r.data),
  get: (id: number) => api.get<Sprint>(`/sprints/${id}`).then(r => r.data),
  create: (projectId: number, req: SprintRequest) => api.post<Sprint>(`/projects/${projectId}/sprints`, req).then(r => r.data),
  update: (id: number, req: Partial<SprintRequest>) => api.put<Sprint>(`/sprints/${id}`, req).then(r => r.data),
  start: (id: number) => api.post<Sprint>(`/sprints/${id}/start`).then(r => r.data),
  complete: (id: number, data?: object) => api.post<Sprint>(`/sprints/${id}/complete`, data ?? {}).then(r => r.data),
  updateMetrics: (id: number, data: object) => api.patch<Sprint>(`/sprints/${id}/metrics`, data).then(r => r.data),
  delete: (id: number) => api.delete(`/sprints/${id}`),
};

// ─── Backlog Items ────────────────────────────────────────────────────────────
export const backlogApi = {
  listByProject: (projectId: number) => api.get<BacklogItem[]>(`/projects/${projectId}/backlog`).then(r => r.data),
  listBySprint: (sprintId: number) => api.get<BacklogItem[]>(`/sprints/${sprintId}/backlog`).then(r => r.data),
  get: (id: number) => api.get<BacklogItem>(`/backlog/${id}`).then(r => r.data),
  create: (projectId: number, req: BacklogItemRequest) => api.post<BacklogItem>(`/projects/${projectId}/backlog`, req).then(r => r.data),
  update: (id: number, req: Partial<BacklogItemRequest>) => api.put<BacklogItem>(`/backlog/${id}`, req).then(r => r.data),
  delete: (id: number) => api.delete(`/backlog/${id}`),
  assignToSprint: (itemId: number, sprintId: number) => api.post<BacklogItem>(`/backlog/${itemId}/assign-sprint/${sprintId}`).then(r => r.data),
  unassignFromSprint: (itemId: number) => api.post<BacklogItem>(`/backlog/${itemId}/unassign-sprint`).then(r => r.data),
  markDone: (itemId: number) => api.post<BacklogItem>(`/backlog/${itemId}/mark-done`).then(r => r.data),
  estimate: (itemId: number, overrides?: EstimationOverrideRequest) =>
    api.post<StoryPointEstimate>(`/backlog/${itemId}/estimate`, overrides ?? {}).then(r => r.data),
};

// ─── Team Members (Jira-style M:N — developer pool ↔ projects) ────────────────
export const teamApi = {
  listAll:       () => api.get<TeamMember[]>(`/developers`).then(r => r.data),
  listByProject: (projectId: number) => api.get<TeamMember[]>(`/projects/${projectId}/team`).then(r => r.data),
  create:        (projectId: number, req: Partial<TeamMember>) =>
                   api.post<TeamMember>(`/projects/${projectId}/team`, req).then(r => r.data),
  assign:        (projectId: number, memberId: number) =>
                   api.post<TeamMember>(`/projects/${projectId}/team/${memberId}`).then(r => r.data),
  unassign:      (projectId: number, memberId: number) =>
                   api.delete(`/projects/${projectId}/team/${memberId}`),
  remove:        (memberId: number) => api.delete(`/team/${memberId}`),
};

// ─── Estimation (ML) ──────────────────────────────────────────────────────────
export const estimationApi = {
  storyPoints: (itemId: number, overrides?: EstimationOverrideRequest) =>
    api.post<StoryPointEstimate>(`/estimation/story-points/${itemId}`, overrides ?? {}).then(r => r.data),
  velocity: (req: VelocityPredictionRequest) => api.post<VelocityPrediction>('/estimation/velocity', req).then(r => r.data),
  sprintPlan: (req: object) => api.post<SprintPlanResult>('/estimation/sprint-plan', req).then(r => r.data),
};

// ─── Analytics ────────────────────────────────────────────────────────────────
export const analyticsApi = {
  complexity: (itemId: number) => api.get<ComplexityScore>(`/analytics/backlog/${itemId}/complexity`).then(r => r.data),
  sprintHealth: (sprintId: number) => api.get<SprintHealth>(`/analytics/sprints/${sprintId}/health`).then(r => r.data),
  sprintReadiness: (sprintId: number) => api.get<SprintReadiness>(`/analytics/sprints/${sprintId}/readiness`).then(r => r.data),
  rollingVelocity: (projectId: number) => api.get<RollingVelocity>(`/analytics/projects/${projectId}/rolling-velocity`).then(r => r.data),
  velocityTrend: (projectId: number) => api.get(`/analytics/projects/${projectId}/velocity-trend`).then(r => r.data),
  backlogAnalysis: (projectId: number, predictedVelocity?: number) =>
    api.get(`/analytics/projects/${projectId}/backlog-analysis`, { params: { predictedVelocity } }).then(r => r.data),
};

// ─── Dashboard ────────────────────────────────────────────────────────────────
export const dashboardApi = {
  burndown: (sprintId: number) => api.get<BurndownResponse>(`/dashboard/sprints/${sprintId}/burndown`).then(r => r.data),
  velocityHistory: (projectId: number) => api.get<VelocityHistoryPoint[]>(`/dashboard/projects/${projectId}/velocity-history`).then(r => r.data),
  risks: (sprintId: number) => api.get<BacklogItem[]>(`/dashboard/sprints/${sprintId}/risks`).then(r => r.data),
  projectSummary: (projectId: number) => api.get<ProjectSummary>(`/dashboard/projects/${projectId}/summary`).then(r => r.data),
};
