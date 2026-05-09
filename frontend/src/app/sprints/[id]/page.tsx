'use client';
import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { sprintsApi, analyticsApi, estimationApi, dashboardApi, backlogApi } from '@/lib/api';
import type { AISuggestionStatus, VelocityPrediction, BacklogItem } from '@/lib/types';
import { AISuggestionCard } from '@/components/ai/AISuggestionCard';
import { Card, CardHeader, CardBody, Button, StatCard, Badge, Spinner, PageHeader } from '@/components/ui';
import {
  Zap, Brain, CheckCircle2, XCircle, AlertTriangle, TrendingUp, Activity, BarChart3,
  Play, CheckCheck, Plus, X, Search, ListPlus, Trash2,
} from 'lucide-react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { clsx } from 'clsx';

const healthColors: Record<string, string> = {
  EXCELLENT: 'text-emerald-700', GOOD: 'text-green-700',
  FAIR: 'text-amber-700', POOR: 'text-orange-700', CRITICAL: 'text-red-700',
};
const healthBg: Record<string, string> = {
  EXCELLENT: 'border-emerald-200 bg-emerald-50/40',
  GOOD:      'border-green-200   bg-green-50/40',
  FAIR:      'border-amber-200   bg-amber-50/40',
  POOR:      'border-orange-200  bg-orange-50/40',
  CRITICAL:  'border-red-200     bg-red-50/40',
};

export default function SprintDetailPage() {
  const { id } = useParams<{ id: string }>();
  const sprintId = Number(id);
  const qc = useQueryClient();

  const { data: sprint }       = useQuery({ queryKey: ['sprint', sprintId],       queryFn: () => sprintsApi.get(sprintId) });
  const { data: health }       = useQuery({ queryKey: ['health', sprintId],       queryFn: () => analyticsApi.sprintHealth(sprintId) });
  const { data: readiness }    = useQuery({ queryKey: ['readiness', sprintId],    queryFn: () => analyticsApi.sprintReadiness(sprintId) });
  const { data: burndown }     = useQuery({ queryKey: ['burndown', sprintId],     queryFn: () => dashboardApi.burndown(sprintId) });
  const { data: riskItems }    = useQuery({ queryKey: ['risks', sprintId],        queryFn: () => dashboardApi.risks(sprintId) });
  const { data: backlogItems } = useQuery({ queryKey: ['sprintBacklog', sprintId], queryFn: () => backlogApi.listBySprint(sprintId) });

  // Project backlog (for the "Add items" picker)
  const projectId = sprint?.projectId;
  const { data: projectBacklog } = useQuery({
    queryKey: ['backlog', projectId],
    queryFn: () => backlogApi.listByProject(projectId!),
    enabled: !!projectId,
  });

  // Velocity AI Suggestion
  const [velStatus, setVelStatus] = useState<AISuggestionStatus>('idle');
  const [velResult, setVelResult] = useState<VelocityPrediction | null>(null);

  // Add-items picker state
  const [showPicker, setShowPicker] = useState(false);
  const [pickerSearch, setPickerSearch] = useState('');

  const startMut = useMutation({
    mutationFn: () => sprintsApi.start(sprintId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sprint', sprintId] }),
  });

  const completeMut = useMutation({
    mutationFn: () => sprintsApi.complete(sprintId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sprint', sprintId] }),
  });

  // Assign / unassign mutations — these are the bug fix
  const assignMut = useMutation({
    mutationFn: (itemId: number) => backlogApi.assignToSprint(itemId, sprintId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sprintBacklog', sprintId] });
      qc.invalidateQueries({ queryKey: ['backlog', projectId] });
      qc.invalidateQueries({ queryKey: ['readiness', sprintId] });
      qc.invalidateQueries({ queryKey: ['burndown', sprintId] });
    },
  });

  const unassignMut = useMutation({
    mutationFn: (itemId: number) => backlogApi.unassignFromSprint(itemId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sprintBacklog', sprintId] });
      qc.invalidateQueries({ queryKey: ['backlog', projectId] });
      qc.invalidateQueries({ queryKey: ['readiness', sprintId] });
      qc.invalidateQueries({ queryKey: ['burndown', sprintId] });
    },
  });

  const runVelocity = async () => {
    setVelStatus('loading');
    try {
      const result = await estimationApi.velocity({ sprintId });
      setVelResult(result);
      setVelStatus('ready');
    } catch {
      setVelStatus('idle');
    }
  };

  const acceptVelocity = async () => {
    if (velResult) {
      await sprintsApi.update(sprintId, { predictedVelocity: velResult.predictedVelocity });
      qc.invalidateQueries({ queryKey: ['sprint', sprintId] });
    }
    setVelStatus('accepted');
  };

  // Items in the project that are NOT in any sprint yet
  const availableItems = useMemo<BacklogItem[]>(() => {
    if (!projectBacklog) return [];
    return projectBacklog.filter(b => !b.sprintId);
  }, [projectBacklog]);

  const filteredAvailable = useMemo(() => {
    const q = pickerSearch.trim().toLowerCase();
    if (!q) return availableItems;
    return availableItems.filter(it =>
      it.title.toLowerCase().includes(q) ||
      it.userStory?.toLowerCase().includes(q),
    );
  }, [availableItems, pickerSearch]);

  if (!sprint) return <Spinner />;

  const burndownPie = (burndown?.statusBreakdown ?? []).map(s => ({
    name: s.status, value: s.storyPoints,
    fill: s.status === 'DONE' ? '#10b981'
      : s.status === 'IN_PROGRESS' ? '#7c3aed'
      : '#cbd5e1',
  }));

  const readinessChecks = readiness ? [
    { label: 'Velocity Predicted',  ok: readiness.velocityPredicted },
    { label: 'All Items Estimated', ok: readiness.allItemsEstimated },
    { label: 'No Ambiguous Items',  ok: readiness.noAmbiguousItems },
    { label: 'Capacity Feasible',   ok: readiness.capacityFeasible },
    { label: 'Team Available',      ok: readiness.teamAvailable },
    { label: 'No Burnout Risk',     ok: readiness.noBurnoutRisk },
  ] : [];

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <PageHeader
        title={sprint.name}
        subtitle={`${sprint.startDate ?? '?'} → ${sprint.endDate ?? '?'}`}
        actions={
          <div className="flex gap-2">
            {sprint.status === 'PLANNED' && (
              <Button variant="success" onClick={() => startMut.mutate()}>
                <Play className="w-4 h-4" /> Start Sprint
              </Button>
            )}
            {sprint.status === 'ACTIVE' && (
              <Button variant="secondary" onClick={() => completeMut.mutate()}>
                <CheckCheck className="w-4 h-4" /> Complete Sprint
              </Button>
            )}
          </div>
        }
      />

      {/* Stats Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Predicted Velocity"  value={sprint.predictedVelocity ?? '—'}                                                       unit="SP"  icon={Zap}        color="violet" />
        <StatCard label="Actual Velocity"     value={sprint.actualVelocity ?? '—'}                                                          unit="SP"  icon={TrendingUp} color="emerald" />
        <StatCard label="Completion Rate"     value={sprint.completionRate ? `${(sprint.completionRate * 100).toFixed(0)}%` : '—'}          icon={Activity}   color="blue" />
        <StatCard label="Team Availability"   value={sprint.developerAvailability ? `${(sprint.developerAvailability * 100).toFixed(0)}%` : '—'} icon={Brain} color="amber" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left column */}
        <div className="space-y-5 lg:col-span-2">

          {/* AI Velocity Prediction */}
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <Brain className="w-4 h-4 text-violet-600" />
                <h2 className="text-sm font-semibold text-slate-900">AI Velocity Prediction</h2>
              </div>
            </CardHeader>
            <CardBody>
              <AISuggestionCard
                title="Sprint Velocity"
                value={velResult?.predictedVelocity ?? 0}
                unit="SP"
                riskLevel={velResult?.riskLevel}
                meta={velResult ? [
                  { label: 'Stress Score',        value: velResult.stressScore.toFixed(1) },
                  { label: 'Effective Capacity',  value: velResult.effectiveCapacity.toFixed(1) },
                  { label: 'Velocity / Dev',      value: velResult.velocityPerDev.toFixed(1) },
                  { label: 'Recommended Max',     value: `${velResult.recommendedCommitment} SP` },
                ] : undefined}
                status={velStatus}
                onAccept={acceptVelocity}
                onReject={() => setVelStatus('rejected')}
                onRequest={runVelocity}
              />

              {/* Diagnostic panel — what the model actually saw. Helps debug bad
                  predictions when they don't match the Jupyter notebook. */}
              {velResult && (
                <div className="mt-4 pt-4 border-t border-slate-100">
                  <p className="text-xs font-semibold text-slate-700 uppercase tracking-wide mb-2.5">What the AI saw</p>
                  <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                    {[
                      { label: 'Domain',           value: velResult.usedDomain ?? '—' },
                      { label: 'Completed Sprints', value: velResult.usedCompletedSprintCount ?? 0 },
                      { label: 'Avg Velocity (3)', value: velResult.usedAvgVelocityLast3?.toFixed(1) ?? '—' },
                      { label: 'Avg Velocity (5)', value: velResult.usedAvgVelocityLast5?.toFixed(1) ?? '—' },
                      { label: 'Team Size',        value: velResult.usedTeamSize ?? '—' },
                      { label: 'Avg Experience',   value: velResult.usedAvgExperienceYears?.toFixed(1) ?? '—' },
                      { label: 'Sprint Duration',  value: `${velResult.usedSprintDurationDays ?? '—'} d` },
                      { label: 'Availability',     value: velResult.usedDeveloperAvailability != null ? `${(velResult.usedDeveloperAvailability * 100).toFixed(0)}%` : '—' },
                      { label: 'Completion Rate',  value: velResult.usedCompletionRate != null ? `${(velResult.usedCompletionRate * 100).toFixed(0)}%` : '—' },
                      { label: 'Planned SP',       value: velResult.usedPlannedStoryPoints ?? 0 },
                      { label: 'Carryover',        value: velResult.usedCarryoverTasks ?? 0 },
                      { label: 'Bugs',             value: velResult.usedNumBugs ?? 0 },
                    ].map(({ label, value }) => (
                      <div key={label} className="bg-slate-50 border border-slate-100 rounded-lg px-2.5 py-1.5">
                        <p className="text-xs text-slate-500">{label}</p>
                        <p className="text-sm font-semibold text-slate-900 truncate">{value}</p>
                      </div>
                    ))}
                  </div>
                  {(velResult.usedCompletedSprintCount ?? 0) === 0 && (
                    <div className="flex items-start gap-2 text-xs text-amber-800 bg-amber-50 rounded-lg px-3 py-2 border border-amber-200 mt-3">
                      <AlertTriangle className="w-3 h-3 mt-0.5 flex-shrink-0" />
                      No completed sprints yet — prediction relies on project defaults. Complete a sprint with `actualVelocity` set so future predictions improve.
                    </div>
                  )}
                </div>
              )}
            </CardBody>
          </Card>

          {/* Sprint Health */}
          {health && (
            <Card className={clsx('border', healthBg[health.healthBand] ?? 'border-slate-200')}>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Activity className="w-4 h-4 text-slate-500" />
                    <h2 className="text-sm font-semibold text-slate-900">Sprint Health</h2>
                  </div>
                  <span className={clsx('text-base font-semibold', healthColors[health.healthBand])}>
                    {health.healthBand} · {(health.healthScore * 100).toFixed(0)}%
                  </span>
                </div>
              </CardHeader>
              <CardBody>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-2.5 mb-4">
                  {[
                    { label: 'Completion Rate', value: `${(health.completionRate * 100).toFixed(0)}%` },
                    { label: 'Carryover Tasks', value: health.carryoverTasks },
                    { label: 'Bugs',            value: health.numBugs },
                    { label: 'Velocity Trend',  value: health.trendDirection },
                    { label: 'Overcommitted',   value: health.isOvercommitted ? 'Yes' : 'No' },
                    { label: 'Burnout Risk',    value: health.burnoutRisk ? 'Yes' : 'No' },
                  ].map(({ label, value }) => (
                    <div key={label} className="bg-white border border-slate-200 rounded-lg px-3 py-2">
                      <p className="text-xs text-slate-500">{label}</p>
                      <p className="text-sm font-semibold text-slate-900">{value}</p>
                    </div>
                  ))}
                </div>
                {health.warnings?.length > 0 && (
                  <div className="space-y-1.5">
                    {health.warnings.map((w, i) => (
                      <div key={i} className="flex items-start gap-2 text-xs text-amber-800 bg-amber-50 rounded-lg px-3 py-2 border border-amber-200">
                        <AlertTriangle className="w-3 h-3 mt-0.5 flex-shrink-0" /> {w}
                      </div>
                    ))}
                  </div>
                )}
                {health.recommendations?.length > 0 && (
                  <div className="space-y-1.5 mt-2">
                    {health.recommendations.map((r, i) => (
                      <div key={i} className="flex items-start gap-2 text-xs text-blue-800 bg-blue-50 rounded-lg px-3 py-2 border border-blue-200">
                        <TrendingUp className="w-3 h-3 mt-0.5 flex-shrink-0" /> {r}
                      </div>
                    ))}
                  </div>
                )}
              </CardBody>
            </Card>
          )}

          {/* Sprint Items + Add Items */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <BarChart3 className="w-4 h-4 text-slate-500" />
                  <h2 className="text-sm font-semibold text-slate-900">
                    Sprint Items ({backlogItems?.length ?? 0})
                  </h2>
                </div>
                {sprint.status !== 'COMPLETED' && (
                  <Button size="sm" onClick={() => setShowPicker(true)}>
                    <Plus className="w-3 h-3" /> Add Items
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardBody className="p-0">
              {!backlogItems?.length ? (
                <div className="px-5 py-8 text-center">
                  <p className="text-sm text-slate-500">No items assigned to this sprint yet.</p>
                  {sprint.status !== 'COMPLETED' && (
                    <Button size="sm" variant="secondary" className="mt-3" onClick={() => setShowPicker(true)}>
                      <ListPlus className="w-3 h-3" /> Add from Backlog
                    </Button>
                  )}
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {backlogItems.map(item => (
                    <div key={item.id} className="flex items-center gap-3 px-5 py-3 hover:bg-slate-50 transition-colors group">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-slate-900 truncate">{item.title}</p>
                        <div className="flex items-center gap-1.5 mt-1 flex-wrap">
                          <Badge variant={
                            item.status === 'DONE' ? 'emerald' :
                            item.status === 'IN_PROGRESS' ? 'violet' :
                            item.status === 'BLOCKED' ? 'red' :
                            'default'
                          }>
                            {item.status ?? 'TODO'}
                          </Badge>
                          {item.taskType && <Badge variant="default">{item.taskType}</Badge>}
                          {item.estimationRisk === 'HIGH' && <Badge variant="red">High Risk</Badge>}
                          {item.isAmbiguous && <Badge variant="amber">Ambiguous</Badge>}
                        </div>
                      </div>
                      <div className="flex items-center gap-3 flex-shrink-0">
                        <div className="text-right">
                          <span className="text-lg font-semibold text-violet-700">{item.storyPoints ?? '—'}</span>
                          <span className="text-xs text-slate-500 ml-1">SP</span>
                        </div>
                        {sprint.status !== 'COMPLETED' && (
                          <button
                            onClick={() => unassignMut.mutate(item.id)}
                            disabled={unassignMut.isPending}
                            title="Remove from sprint"
                            className="opacity-0 group-hover:opacity-100 text-slate-400 hover:text-red-500 transition-all p-1"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>

          {/* Risk Items */}
          {riskItems && riskItems.length > 0 && (
            <Card>
              <CardHeader>
                <div className="flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-red-500" />
                  <h2 className="text-sm font-semibold text-slate-900">High-Risk Items ({riskItems.length})</h2>
                </div>
              </CardHeader>
              <CardBody className="p-0">
                <div className="divide-y divide-slate-100">
                  {riskItems.map(item => (
                    <div key={item.id} className="flex items-center justify-between px-5 py-3">
                      <div className="min-w-0">
                        <p className="text-sm text-slate-900 truncate">{item.title}</p>
                        <div className="flex gap-1.5 mt-1">
                          {item.estimationRisk === 'HIGH' && <Badge variant="red">High Risk</Badge>}
                          {item.isAmbiguous && <Badge variant="amber">Ambiguous</Badge>}
                          {item.numComponents && item.numComponents >= 5 && <Badge variant="default">{item.numComponents} components</Badge>}
                        </div>
                      </div>
                      <span className="text-lg font-semibold text-violet-700">{item.storyPoints ?? '?'} <span className="text-xs text-slate-500 font-normal">SP</span></span>
                    </div>
                  ))}
                </div>
              </CardBody>
            </Card>
          )}
        </div>

        {/* Right column */}
        <div className="space-y-5">
          {/* Sprint Readiness */}
          {readiness && (
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-slate-500" />
                    <h2 className="text-sm font-semibold text-slate-900">Readiness Check</h2>
                  </div>
                  <Badge variant={readiness.isReady ? 'emerald' : 'red'}>
                    {readiness.isReady ? '✓ Ready' : '✗ Not Ready'}
                  </Badge>
                </div>
              </CardHeader>
              <CardBody>
                <div className="space-y-2">
                  {readinessChecks.map(({ label, ok }) => (
                    <div key={label} className="flex items-center gap-2.5">
                      {ok ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                      ) : (
                        <XCircle className="w-4 h-4 text-red-500 flex-shrink-0" />
                      )}
                      <span className={clsx('text-xs', ok ? 'text-slate-700' : 'text-slate-500')}>{label}</span>
                    </div>
                  ))}
                </div>
                {readiness.blockers?.length > 0 && (
                  <div className="mt-3 space-y-1">
                    {readiness.blockers.map((b, i) => (
                      <p key={i} className="text-xs text-red-700 bg-red-50 border border-red-200 rounded px-2 py-1">{b}</p>
                    ))}
                  </div>
                )}
                <div className="mt-4 pt-3 border-t border-slate-100 flex justify-between text-xs">
                  <span className="text-slate-500">Planned: <strong className="text-slate-900">{readiness.totalPlannedSP} SP</strong></span>
                  <span className="text-slate-500">Max: <strong className="text-slate-900">{readiness.recommendedMax} SP</strong></span>
                </div>
              </CardBody>
            </Card>
          )}

          {/* Burndown Pie */}
          {burndown && (
            <Card>
              <CardHeader>
                <div className="flex items-center gap-2">
                  <BarChart3 className="w-4 h-4 text-slate-500" />
                  <h2 className="text-sm font-semibold text-slate-900">Burndown</h2>
                </div>
              </CardHeader>
              <CardBody>
                <div className="text-center mb-3">
                  <p className="text-3xl font-semibold text-slate-900">{(burndown.completionRate * 100).toFixed(0)}%</p>
                  <p className="text-xs text-slate-500 mt-0.5">Story Points Completed</p>
                </div>
                {burndownPie.some(d => d.value > 0) ? (
                  <ResponsiveContainer width="100%" height={160}>
                    <PieChart>
                      <Pie data={burndownPie} cx="50%" cy="50%" innerRadius={45} outerRadius={70} paddingAngle={2} dataKey="value">
                        {burndownPie.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
                      </Pie>
                      <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: 8, fontSize: 12 }} />
                    </PieChart>
                  </ResponsiveContainer>
                ) : null}
                <div className="space-y-1.5 mt-2">
                  {[
                    { label: 'Completed', value: burndown.completedStoryPoints, color: 'bg-emerald-500' },
                    { label: 'Remaining', value: burndown.remainingStoryPoints, color: 'bg-slate-300' },
                  ].map(({ label, value, color }) => (
                    <div key={label} className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-1.5">
                        <span className={`w-2 h-2 rounded-full ${color}`} />
                        <span className="text-slate-500">{label}</span>
                      </div>
                      <span className="text-slate-900 font-medium">{value} SP</span>
                    </div>
                  ))}
                </div>
              </CardBody>
            </Card>
          )}
        </div>
      </div>

      {/* Add Items Modal */}
      <AddItemsModal
        open={showPicker}
        onClose={() => { setShowPicker(false); setPickerSearch(''); }}
        items={filteredAvailable}
        search={pickerSearch}
        onSearchChange={setPickerSearch}
        onAdd={(itemId) => assignMut.mutate(itemId)}
        adding={assignMut.isPending}
        addingId={assignMut.variables ?? null}
      />
    </div>
  );
}

// ─── Add Items Modal ──────────────────────────────────────────────────────────
function AddItemsModal({
  open, onClose, items, search, onSearchChange, onAdd, adding, addingId,
}: {
  open: boolean;
  onClose: () => void;
  items: BacklogItem[];
  search: string;
  onSearchChange: (v: string) => void;
  onAdd: (itemId: number) => void;
  adding: boolean;
  addingId: number | null;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm" onClick={onClose}>
      <div
        className="w-full max-w-2xl bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden flex flex-col max-h-[80vh]"
        onClick={e => e.stopPropagation()}
      >
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
          <div>
            <h3 className="text-base font-semibold text-slate-900">Add Items to Sprint</h3>
            <p className="text-xs text-slate-500 mt-0.5">Pick from your project backlog ({items.length} available)</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="px-5 py-3 border-b border-slate-100">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              autoFocus
              value={search}
              onChange={e => onSearchChange(e.target.value)}
              placeholder="Search by title or user story…"
              className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {items.length === 0 ? (
            <div className="px-5 py-12 text-center">
              <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center mx-auto mb-3">
                <BarChart3 className="w-6 h-6 text-slate-400" />
              </div>
              <p className="text-sm font-medium text-slate-700">
                {search ? 'No items match your search' : 'No unassigned items in the backlog'}
              </p>
              <p className="text-xs text-slate-500 mt-1.5">
                {search ? 'Try a different keyword.' : 'Create new stories from the Backlog page first.'}
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {items.map(item => (
                <div key={item.id} className="flex items-center gap-3 px-5 py-3 hover:bg-slate-50 transition-colors">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-900 truncate">{item.title}</p>
                    {item.userStory && (
                      <p className="text-xs text-slate-500 line-clamp-1 mt-0.5">{item.userStory}</p>
                    )}
                    <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
                      {item.taskType && <Badge variant="default">{item.taskType}</Badge>}
                      {item.priority && (
                        <Badge variant={
                          item.priority === 'CRITICAL' ? 'red' :
                          item.priority === 'HIGH' ? 'amber' :
                          item.priority === 'MEDIUM' ? 'blue' :
                          'default'
                        }>
                          {item.priority}
                        </Badge>
                      )}
                      {item.estimationRisk === 'HIGH' && <Badge variant="red">High Risk</Badge>}
                      {item.isAmbiguous && <Badge variant="amber">Ambiguous</Badge>}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <div className="text-right min-w-[42px]">
                      <span className="text-base font-semibold text-violet-700">{item.storyPoints ?? '—'}</span>
                      <span className="text-xs text-slate-500 ml-0.5">SP</span>
                    </div>
                    <Button
                      size="sm"
                      onClick={() => onAdd(item.id)}
                      disabled={adding && addingId === item.id}
                    >
                      {adding && addingId === item.id ? '…' : <><Plus className="w-3 h-3" /> Add</>}
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="px-5 py-3 border-t border-slate-200 bg-slate-50 flex justify-end">
          <Button variant="secondary" onClick={onClose}>Done</Button>
        </div>
      </div>
    </div>
  );
}
