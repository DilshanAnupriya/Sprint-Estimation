// ─── Core Domain Types ────────────────────────────────────────────────────────

export interface Project {
  id: number;
  name: string;
  description?: string;
  domain?: string;
  defaultTechStack?: string;
  teamSize?: number;
  defaultSprintDurationDays?: number;
  defaultTeamVelocity?: number;
  defaultCompletionRate?: number;
  defaultDevExperienceLevel?: 'JUNIOR' | 'MID' | 'SENIOR';
  // Counts populated from backend relationships
  teamMemberCount?: number;
  sprintCount?: number;
  backlogItemCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface Sprint {
  id: number;
  projectId: number;
  projectName?: string;          // populated from backend relationship
  backlogItemCount?: number;     // # items in this sprint
  totalPlannedSP?: number;       // sum of SP for items in this sprint
  name: string;
  techStack?: string;            // sprint-specific stack (overrides project default)
  status: 'PLANNED' | 'ACTIVE' | 'COMPLETED';
  startDate?: string;
  endDate?: string;
  durationDays?: number;
  predictedVelocity?: number;
  actualVelocity?: number;
  developerAvailability?: number;
  leaveDaysTotal?: number;
  completionRate?: number;
  carryoverTasks?: number;
  numBugsThisSprint?: number;
  avgVelocityLast3?: number;
  avgVelocityLast5?: number;
  healthScore?: number;
  velocityTrend?: number;
  effectiveCapacity?: number;
  isOvercommitted?: boolean;
  burnoutRisk?: boolean;
  velocityPerDev?: number;
  createdAt: string;
  updatedAt: string;
}

export interface BacklogItem {
  id: number;
  projectId: number;
  projectName?: string;          // populated from backend relationship
  sprintId?: number;
  sprintName?: string;           // populated from backend relationship (null if not in sprint)
  subTaskCount?: number;         // populated from backend relationship
  title: string;
  userStory?: string;
  taskType?: 'FEATURE' | 'BUG' | 'ENHANCEMENT' | 'TECHNICAL_DEBT' | 'RESEARCH';
  priority?: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  status?: 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE' | 'BLOCKED' | 'CANCELLED';
  techStack?: string;
  numComponents?: number;
  externalApis?: number;
  hasIntegration?: boolean;
  hasSecurity?: boolean;
  hasUiComplexity?: boolean;
  storyPoints?: number;
  mlPointEstimate?: number;
  mlLowerBound?: number;
  mlUpperBound?: number;
  estimationRisk?: string;
  complexityScore?: number;
  estimatedHours?: number;
  isAmbiguous?: boolean;
  ambiguityReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TeamMember {
  id: number;
  name: string;
  email?: string;
  role?: string;
  experienceLevel?: 'JUNIOR' | 'MID' | 'SENIOR';
  experienceYears?: number;
  // M:N — a developer can be on many projects (Jira-style)
  projectIds?: number[];
  projectNames?: string[];
}

// ─── AI / ML Response Types ───────────────────────────────────────────────────

export interface StoryPointEstimate {
  backlogItemId: number;
  pointEstimate: number;
  lowerBound: number;
  upperBound: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  rawPoint: number;
  rawP20: number;
  rawP80: number;
  usedFineTuned: boolean;
  domainUsed: string;

  // Diagnostic — actual values sent to the ML model (all 16 inputs)
  usedTechStack?: string;
  usedDomain?: string;
  usedTaskType?: string;
  usedDevExperienceLevel?: string;
  usedTeamSize?: number;
  usedSprintDurationDays?: number;
  usedTeamVelocityAvg?: number;
  usedRecentCompletionRate?: number;
  usedSimilarTaskCount?: number;
  usedTaskAgeDays?: number;
  usedHasIntegration?: boolean;
  usedHasSecurity?: boolean;
  usedHasUiComplexity?: boolean;
  usedNumComponents?: number;
  usedExternalApis?: number;
  usedUserStory?: string;
}

/**
 * Optional overrides for the SP estimate endpoint — mirrors the Jupyter
 * notebook's interactive prompts. Any null field auto-derives from the
 * backlog item / project / sprint state.
 */
export interface EstimationOverrideRequest {
  domain?: string;
  techStack?: string;
  taskType?: string;     // feature | bug | enhancement | technical_debt | research
  devExperienceLevel?: 'JUNIOR' | 'MID' | 'SENIOR';
  teamSize?: number;
  sprintDurationDays?: number;
  teamVelocityAvg?: number;
  recentCompletionRate?: number;
  similarTaskCount?: number;
  taskAgeDays?: number;
  numComponents?: number;
  externalApis?: number;
  hasIntegration?: boolean;
  hasSecurity?: boolean;
  hasUiComplexity?: boolean;
  userStory?: string;
}

export interface VelocityPrediction {
  sprintId?: number;
  predictedVelocity: number;
  rawPrediction: number;
  basePrediction: number;
  usedFineTuned: boolean;
  domainUsed: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  stressScore: number;
  velocityDelta: number;
  effectiveCapacity: number;
  velocityPerDev: number;
  recommendedCommitment: number;
  // Diagnostics — what the model actually saw
  usedAvgVelocityLast3?: number;
  usedAvgVelocityLast5?: number;
  usedTeamSize?: number;
  usedSprintDurationDays?: number;
  usedCompletionRate?: number;
  usedCarryoverTasks?: number;
  usedDeveloperAvailability?: number;
  usedLeaveDays?: number;
  usedPlannedStoryPoints?: number;
  usedNumBugs?: number;
  usedAvgExperienceYears?: number;
  usedDomain?: string;
  usedCompletedSprintCount?: number;
}

export interface SprintPlanResult {
  sprintId?: number;
  projectId?: number;
  velocityPrediction: VelocityPrediction;
  storyPointEstimates: StoryPointEstimate[];
  estimatedItemCount: number;
  totalPlannedSP: number;
  unestimatedItems: number;
  recommendedMaxSP: number;
  isFeasible: boolean;
  surplusSP: number;
  warnings: string[];
}

// ─── Analytics Types ──────────────────────────────────────────────────────────

export interface SprintHealth {
  sprintId: number;
  sprintName: string;
  completionRate: number;
  carryoverTasks: number;
  developerAvailability: number;
  numBugs: number;
  leaveDays: number;
  avgVelocityLast3: number;
  avgVelocityLast5: number;
  healthScore: number;
  healthBand: 'CRITICAL' | 'POOR' | 'FAIR' | 'GOOD' | 'EXCELLENT';
  velocityTrend: number;
  trendDirection: 'IMPROVING' | 'STABLE' | 'DECLINING';
  velocityPerDev: number;
  effectiveCapacity: number;
  isOvercommitted: boolean;
  burnoutRisk: boolean;
  recommendedCommitment: number;
  warnings: string[];
  recommendations: string[];
}

export interface SprintReadiness {
  sprintId: number;
  isReady: boolean;
  velocityPredicted: boolean;
  allItemsEstimated: boolean;
  noAmbiguousItems: boolean;
  capacityFeasible: boolean;
  teamAvailable: boolean;
  noBurnoutRisk: boolean;
  totalPlannedSP: number;
  recommendedMax: number;
  unestimatedCount: number;
  blockers: string[];
  warnings: string[];
}

export interface ComplexityScore {
  backlogItemId: number;
  title: string;
  numComponents: number;
  externalApis: number;
  hasIntegration: boolean;
  hasSecurity: boolean;
  hasUiComplexity: boolean;
  wordCount: number;
  complexityScore: number;
  complexityBand: 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';
  isAmbiguous: boolean;
  ambiguityReason?: string;
  recommendation: string;
}

export interface BurndownResponse {
  sprintId: number;
  sprintName: string;
  totalItems: number;
  totalStoryPoints: number;
  completedItems: number;
  completedStoryPoints: number;
  inProgressItems: number;
  todoItems: number;
  remainingStoryPoints: number;
  completionRate: number;
  statusBreakdown: { status: string; itemCount: number; storyPoints: number }[];
}

export interface VelocityHistoryPoint {
  sprintId: number;
  sprintName: string;
  predictedVelocity?: number;
  actualVelocity?: number;
  completionRate?: number;
  healthScore?: number;
  startDate?: string;
  endDate?: string;
}

export interface ProjectSummary {
  projectId: number;
  projectName: string;
  domain?: string;
  defaultTechStack?: string;
  teamSize?: number;
  totalSprints: number;
  completedSprints: number;
  activeSprints: number;
  plannedSprints: number;
  totalBacklogItems: number;
  completedItems: number;
  inProgressItems: number;
  todoItems: number;
  totalStoryPoints: number;
  completedStoryPoints: number;
  unestimatedItems: number;
  highRiskItems: number;
  ambiguousItems: number;
  avgActualVelocity: number;
  avgVelocityLast3: number;
  overallCompletionRate: number;
}

export interface RollingVelocity {
  avgVelocityLast3: number;
  avgVelocityLast5: number;
  completedSprintCount: number;
}

// ─── Request Types ────────────────────────────────────────────────────────────

export interface ProjectRequest {
  name: string;
  description?: string;
  domain?: string;
  defaultTechStack?: string;
  teamSize?: number;
  defaultSprintDurationDays?: number;
  defaultTeamVelocity?: number;
  defaultCompletionRate?: number;
  defaultDevExperienceLevel?: 'JUNIOR' | 'MID' | 'SENIOR';
}

export interface SprintRequest {
  name: string;
  techStack?: string;            // sprint-specific stack
  startDate?: string;
  endDate?: string;
  durationDays?: number;
  predictedVelocity?: number;
  developerAvailability?: number;
  leaveDaysTotal?: number;
}

export interface BacklogItemRequest {
  title: string;
  userStory?: string;
  taskType?: BacklogItem['taskType'];
  priority?: BacklogItem['priority'];
  status?: BacklogItem['status'];
  techStack?: string;
  numComponents?: number;
  externalApis?: number;
  hasIntegration?: boolean;
  hasSecurity?: boolean;
  hasUiComplexity?: boolean;
  storyPoints?: number;
}

export interface VelocityPredictionRequest {
  sprintId?: number;
  avgVelocityLast3?: number;
  avgVelocityLast5?: number;
  teamSize?: number;
  sprintDurationDays?: number;
  completionRate?: number;
  carryoverTasks?: number;
  developerAvailability?: number;
  leaveDays?: number;
  plannedStoryPoints?: number;
  numBugs?: number;
  avgExperienceYears?: number;
  domain?: string;
}

// ─── UI Helper Types ──────────────────────────────────────────────────────────

export type AISuggestionStatus = 'idle' | 'loading' | 'ready' | 'accepted' | 'rejected';
