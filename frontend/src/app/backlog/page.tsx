'use client';
import { Suspense, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'next/navigation';
import { backlogApi, projectsApi, sprintsApi } from '@/lib/api';
import type { BacklogItemRequest, AISuggestionStatus, StoryPointEstimate, Sprint, BacklogItem } from '@/lib/types';
import { AISuggestionCard } from '@/components/ai/AISuggestionCard';
import { EstimationModal } from '@/components/ai/EstimationModal';
import { Card, CardHeader, CardBody, Button, Input, Select, Textarea, Badge, Spinner, EmptyState, PageHeader } from '@/components/ui';
import {
  Plus, BarChart3, Sparkles, Trash2, CheckCircle2, AlertTriangle, X, ChevronDown, ChevronUp, Brain, MoveRight, Zap,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { clsx } from 'clsx';

const TASK_TYPES = ['FEATURE', 'BUG', 'ENHANCEMENT', 'TECHNICAL_DEBT', 'RESEARCH'];
const PRIORITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

const priorityColor: Record<string, 'red' | 'amber' | 'blue' | 'default'> = {
  CRITICAL: 'red', HIGH: 'amber', MEDIUM: 'blue', LOW: 'default',
};
const statusColor: Record<string, 'default' | 'violet' | 'amber' | 'emerald' | 'red'> = {
  TODO: 'default', IN_PROGRESS: 'violet', IN_REVIEW: 'amber', DONE: 'emerald', BLOCKED: 'red',
};
const riskColor: Record<string, 'emerald' | 'amber' | 'red'> = {
  LOW: 'emerald', MEDIUM: 'amber', HIGH: 'red',
};

function BacklogItemRow({ item, sprints, onEstimate, onDelete, onMarkDone, onAssignSprint, onUnassignSprint }: {
  item: any;
  sprints?: Sprint[];
  onEstimate: () => void;
  onDelete: () => void;
  onMarkDone: () => void;
  onAssignSprint: (sprintId: number) => void;
  onUnassignSprint: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const hasEstimate = item.mlPointEstimate != null;
  const sprintsAvailable = (sprints ?? []).filter(s => s.status !== 'COMPLETED');

  return (
    <motion.div layout className="border border-slate-200 rounded-xl overflow-hidden bg-white shadow-sm">
      <div
        className="flex items-start gap-3 p-4 cursor-pointer hover:bg-slate-50 transition-colors"
        onClick={() => setExpanded(e => !e)}
      >
        {/* Priority bar */}
        <div className={clsx('w-1 self-stretch rounded-full flex-shrink-0', {
          'bg-red-500':    item.priority === 'CRITICAL',
          'bg-amber-500':  item.priority === 'HIGH',
          'bg-blue-500':   item.priority === 'MEDIUM',
          'bg-slate-300':  item.priority === 'LOW' || !item.priority,
        })} />

        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div className="flex-1 min-w-0">
              <h3 className="text-sm font-semibold text-slate-900 truncate">{item.title}</h3>
              {item.userStory && (
                <p className="text-xs text-slate-500 mt-0.5 line-clamp-2">{item.userStory}</p>
              )}
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
              {/* SP Badge */}
              {item.storyPoints != null ? (
                <div className="flex items-center gap-1.5">
                  <span className="text-lg font-semibold text-violet-700">{item.storyPoints}</span>
                  <span className="text-xs text-slate-500">SP</span>
                  {hasEstimate && <Brain className="w-3 h-3 text-violet-500" />}
                </div>
              ) : (
                <button
                  onClick={e => { e.stopPropagation(); onEstimate(); }}
                  className="flex items-center gap-1 text-xs px-2 py-1 rounded-md border border-violet-200 bg-violet-50 text-violet-700 hover:bg-violet-100 transition-colors"
                >
                  <Sparkles className="w-3 h-3" /> Estimate
                </button>
              )}
              {/* Status */}
              <Badge variant={statusColor[item.status ?? 'TODO']}>
                {item.status ?? 'TODO'}
              </Badge>
              {/* Expand */}
              <button onClick={e => { e.stopPropagation(); setExpanded(e2 => !e2); }} className="text-slate-400 hover:text-slate-600">
                {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div className="flex flex-wrap gap-1.5 mt-2">
            {item.taskType && <Badge variant="default">{item.taskType}</Badge>}
            {item.priority && <Badge variant={priorityColor[item.priority]}>{item.priority}</Badge>}
            {item.sprintId && (
              <Badge variant="blue">
                <Zap className="w-2.5 h-2.5" />
                {item.sprintName ?? `Sprint #${item.sprintId}`}
              </Badge>
            )}
            {item.subTaskCount != null && item.subTaskCount > 0 && (
              <Badge variant="default">
                {item.subTaskCount} subtask{item.subTaskCount !== 1 ? 's' : ''}
              </Badge>
            )}
            {item.isAmbiguous && (
              <Badge variant="amber">
                <AlertTriangle className="w-2.5 h-2.5" />Ambiguous
              </Badge>
            )}
            {item.estimationRisk && <Badge variant={riskColor[item.estimationRisk]}>{item.estimationRisk} risk</Badge>}
          </div>
        </div>
      </div>

      {/* Expanded detail */}
      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden border-t border-slate-100"
          >
            <div className="p-4 bg-slate-50/40">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5 mb-4">
                {[
                  { label: 'Components',   value: item.numComponents ?? '—' },
                  { label: 'External APIs', value: item.externalApis ?? '—' },
                  { label: 'Complexity',   value: item.complexityScore?.toFixed(2) ?? '—' },
                  { label: 'Est. Hours',   value: item.estimatedHours?.toFixed(1) ?? '—' },
                ].map(({ label, value }) => (
                  <div key={label} className="bg-white border border-slate-200 rounded-lg px-3 py-2">
                    <p className="text-xs text-slate-500">{label}</p>
                    <p className="text-sm font-semibold text-slate-900">{value}</p>
                  </div>
                ))}
              </div>

              {/* ML Estimate details */}
              {hasEstimate && (
                <div className="bg-violet-50/50 border border-violet-200 rounded-lg p-3 mb-3">
                  <div className="flex items-center gap-2 mb-2">
                    <Brain className="w-3.5 h-3.5 text-violet-600" />
                    <span className="text-xs font-semibold text-violet-700 uppercase tracking-wide">AI Estimate Details</span>
                  </div>
                  <div className="grid grid-cols-3 gap-2">
                    <div className="text-center">
                      <p className="text-xs text-slate-500">Lower (P20)</p>
                      <p className="text-sm font-semibold text-slate-900">{item.mlLowerBound}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-xs text-slate-500">Estimate</p>
                      <p className="text-lg font-semibold text-violet-700">{item.mlPointEstimate}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-xs text-slate-500">Upper (P80)</p>
                      <p className="text-sm font-semibold text-slate-900">{item.mlUpperBound}</p>
                    </div>
                  </div>
                </div>
              )}

              {item.ambiguityReason && (
                <div className="flex items-start gap-2 text-xs text-amber-800 bg-amber-50 rounded-lg px-3 py-2 border border-amber-200 mb-3">
                  <AlertTriangle className="w-3 h-3 mt-0.5 flex-shrink-0" />
                  {item.ambiguityReason}
                </div>
              )}

              <div className="flex flex-wrap gap-2">
                {item.status !== 'DONE' && (
                  <Button size="sm" variant="success" onClick={onMarkDone}>
                    <CheckCircle2 className="w-3 h-3" /> Mark Done
                  </Button>
                )}
                <Button size="sm" variant="secondary" onClick={onEstimate}>
                  <Sparkles className="w-3 h-3" /> Re-estimate
                </Button>
                {/* Assign / Unassign Sprint */}
                {item.sprintId ? (
                  <Button size="sm" variant="ghost" onClick={onUnassignSprint}>
                    <MoveRight className="w-3 h-3" /> Remove from Sprint
                  </Button>
                ) : sprintsAvailable.length > 0 ? (
                  <div className="relative">
                    <select
                      value=""
                      onChange={e => e.target.value && onAssignSprint(Number(e.target.value))}
                      className="text-xs px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-slate-700 hover:bg-slate-50 cursor-pointer focus:outline-none focus:ring-2 focus:ring-violet-500/30"
                    >
                      <option value="">Move to Sprint…</option>
                      {sprintsAvailable.map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.status})</option>
                      ))}
                    </select>
                  </div>
                ) : null}
                <Button size="sm" variant="danger" onClick={onDelete}>
                  <Trash2 className="w-3 h-3" />
                </Button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

export default function BacklogPage() {
  return (
    <Suspense fallback={<Spinner />}>
      <BacklogPageInner />
    </Suspense>
  );
}

function BacklogPageInner() {
  const params = useSearchParams();
  const projectIdParam = params.get('project');
  const qc = useQueryClient();

  const { data: projects } = useQuery({ queryKey: ['projects'], queryFn: projectsApi.list });
  const [selectedProject, setSelectedProject] = useState<number | null>(projectIdParam ? Number(projectIdParam) : null);
  const [showForm, setShowForm] = useState(false);
  const [filter, setFilter] = useState<'all' | 'unestimated' | 'high-risk'>('all');

  const [aiStatus, setAiStatus] = useState<Record<number, AISuggestionStatus>>({});
  const [aiResult, setAiResult] = useState<Record<number, StoryPointEstimate>>({});
  const [estimateItem, setEstimateItem] = useState<BacklogItem | null>(null);

  const [form, setForm] = useState<BacklogItemRequest>({
    title: '', userStory: '', taskType: 'FEATURE', priority: 'MEDIUM',
    techStack: '', numComponents: 1, externalApis: 0,
    hasIntegration: false, hasSecurity: false, hasUiComplexity: false,
  });

  const projectId = selectedProject ?? (projects?.[0]?.id ?? null);
  const { data: backlog, isLoading } = useQuery({
    queryKey: ['backlog', projectId],
    queryFn: () => backlogApi.listByProject(projectId!),
    enabled: !!projectId,
  });

  const { data: sprints } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: () => sprintsApi.listByProject(projectId!),
    enabled: !!projectId,
  });

  const createMut = useMutation({
    mutationFn: (req: BacklogItemRequest) => backlogApi.create(projectId!, req),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['backlog', projectId] }); setShowForm(false); },
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => backlogApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['backlog', projectId] }),
  });

  const markDoneMut = useMutation({
    mutationFn: (id: number) => backlogApi.markDone(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['backlog', projectId] }),
  });

  const assignMut = useMutation({
    mutationFn: ({ itemId, sprintId }: { itemId: number; sprintId: number }) => backlogApi.assignToSprint(itemId, sprintId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['backlog', projectId] }),
  });

  const unassignMut = useMutation({
    mutationFn: (itemId: number) => backlogApi.unassignFromSprint(itemId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['backlog', projectId] }),
  });

  /** Open the estimation modal for the given item. The modal collects every
   *  ML input (mirroring the Jupyter notebook flow) and calls the API itself. */
  const openEstimate = (item: BacklogItem) => {
    setEstimateItem(item);
    setAiStatus(s => ({ ...s, [item.id]: 'loading' }));
  };

  const handlePredicted = (itemId: number, result: StoryPointEstimate) => {
    setAiResult(r => ({ ...r, [itemId]: result }));
    setAiStatus(s => ({ ...s, [itemId]: 'ready' }));
    qc.invalidateQueries({ queryKey: ['backlog', projectId] });
  };

  const handleModalClose = () => {
    if (estimateItem && aiStatus[estimateItem.id] === 'loading') {
      // User closed without predicting — reset to idle
      setAiStatus(s => ({ ...s, [estimateItem.id]: 'idle' }));
    }
    setEstimateItem(null);
  };

  const handleAccept = (itemId: number) => {
    setAiStatus(s => ({ ...s, [itemId]: 'accepted' }));
    qc.invalidateQueries({ queryKey: ['backlog', projectId] });
  };

  const handleReject = (itemId: number) => {
    setAiStatus(s => ({ ...s, [itemId]: 'rejected' }));
  };

  const filteredBacklog = (backlog ?? []).filter(item => {
    if (filter === 'unestimated') return item.storyPoints == null;
    if (filter === 'high-risk')   return item.estimationRisk === 'HIGH' || item.isAmbiguous;
    return true;
  });

  const unestimated = backlog?.filter(b => !b.storyPoints).length ?? 0;
  const highRisk = backlog?.filter(b => b.estimationRisk === 'HIGH' || b.isAmbiguous).length ?? 0;

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <PageHeader
        title="Product Backlog"
        subtitle="User stories with AI-powered story point estimation"
        actions={
          <Button onClick={() => setShowForm(true)} disabled={!projectId}>
            <Plus className="w-4 h-4" /> Add Story
          </Button>
        }
      />

      {/* Project Selector + Filters */}
      <div className="flex items-center gap-3 mb-6 flex-wrap">
        <label className="text-xs font-medium text-slate-600 uppercase tracking-wide">Project</label>
        <select
          value={selectedProject ?? ''}
          onChange={e => setSelectedProject(Number(e.target.value))}
          className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500 shadow-sm"
        >
          <option value="">Select a project…</option>
          {projects?.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
        <div className="flex gap-1 ml-auto bg-white border border-slate-200 rounded-lg p-0.5 shadow-sm">
          {(['all', 'unestimated', 'high-risk'] as const).map(f => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={clsx(
                'px-3 py-1 rounded-md text-xs font-medium transition-colors',
                filter === f
                  ? 'bg-violet-600 text-white shadow-sm'
                  : 'text-slate-600 hover:text-slate-900',
              )}
            >
              {f === 'all'           ? `All (${backlog?.length ?? 0})`
              : f === 'unestimated'  ? `Unestimated (${unestimated})`
              :                        `High Risk (${highRisk})`}
            </button>
          ))}
        </div>
      </div>

      {/* Create Form */}
      <AnimatePresence>
        {showForm && (
          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} className="mb-4 overflow-hidden">
            <Card className="border-violet-200">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-900">New Backlog Item</h3>
                  <button onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </CardHeader>
              <CardBody>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="md:col-span-2">
                    <Input label="Title *" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="As a user, I want to…" />
                  </div>
                  <div className="md:col-span-2">
                    <Textarea label="User Story (longer = better AI estimate)" value={form.userStory} onChange={e => setForm(f => ({ ...f, userStory: e.target.value }))} rows={3} placeholder="Describe what the user wants to do and why…" />
                  </div>
                  <Select label="Task Type" value={form.taskType} onChange={e => setForm(f => ({ ...f, taskType: e.target.value as any }))}>
                    {TASK_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                  </Select>
                  <Select label="Priority" value={form.priority} onChange={e => setForm(f => ({ ...f, priority: e.target.value as any }))}>
                    {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
                  </Select>
                  <Input label="Tech Stack" value={form.techStack} onChange={e => setForm(f => ({ ...f, techStack: e.target.value }))} placeholder="springboot, react…" />
                  <Input label="Num Components" type="number" min={1} value={form.numComponents} onChange={e => setForm(f => ({ ...f, numComponents: +e.target.value }))} />
                  <Input label="External APIs" type="number" min={0} value={form.externalApis} onChange={e => setForm(f => ({ ...f, externalApis: +e.target.value }))} />
                  <div className="space-y-2">
                    <label className="block text-xs font-medium text-slate-600">Complexity Flags</label>
                    <div className="space-y-1.5">
                      {(['hasIntegration', 'hasSecurity', 'hasUiComplexity'] as const).map(flag => (
                        <label key={flag} className="flex items-center gap-2 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={!!form[flag]}
                            onChange={e => setForm(f => ({ ...f, [flag]: e.target.checked }))}
                            className="w-3.5 h-3.5 rounded border-slate-300 text-violet-600 focus:ring-violet-500"
                          />
                          <span className="text-xs text-slate-700">{flag.replace('has', 'Has ')}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                </div>
                <div className="flex gap-2 mt-5">
                  <Button onClick={() => createMut.mutate(form)} disabled={!form.title || createMut.isPending}>
                    {createMut.isPending ? 'Adding…' : 'Add Item'}
                  </Button>
                  <Button variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button>
                </div>
              </CardBody>
            </Card>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Backlog Items */}
      {!projectId ? (
        <EmptyState icon={BarChart3} title="Select a project" description="Choose a project above to view its backlog" />
      ) : isLoading ? (
        <Spinner />
      ) : !filteredBacklog.length ? (
        <EmptyState
          icon={BarChart3}
          title={filter === 'all' ? 'No backlog items' : `No ${filter} items`}
          description={filter === 'all' ? 'Add user stories to start estimating' : undefined}
          action={filter === 'all' ? <Button size="sm" onClick={() => setShowForm(true)}><Plus className="w-3 h-3" /> Add First Story</Button> : undefined}
        />
      ) : (
        <div className="space-y-3">
          {filteredBacklog.map(item => (
            <div key={item.id}>
              <BacklogItemRow
                item={item}
                sprints={sprints}
                onEstimate={() => openEstimate(item)}
                onDelete={() => deleteMut.mutate(item.id)}
                onMarkDone={() => markDoneMut.mutate(item.id)}
                onAssignSprint={(sprintId) => assignMut.mutate({ itemId: item.id, sprintId })}
                onUnassignSprint={() => unassignMut.mutate(item.id)}
              />
              {/* AI Suggestion Panel */}
              <AnimatePresence>
                {(aiStatus[item.id] && aiStatus[item.id] !== 'idle') && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="mt-2 ml-4 overflow-hidden"
                  >
                    <AISuggestionCard
                      title="Story Point Estimate"
                      value={aiResult[item.id]?.pointEstimate ?? '…'}
                      unit="SP"
                      lowerBound={aiResult[item.id]?.lowerBound}
                      upperBound={aiResult[item.id]?.upperBound}
                      riskLevel={aiResult[item.id]?.riskLevel}
                      meta={aiResult[item.id] ? [
                        { label: 'Domain',     value: `${aiResult[item.id].domainUsed}${aiResult[item.id].usedFineTuned ? ' · fine-tuned' : ' · base'}` },
                        { label: 'Experience', value: aiResult[item.id].usedDevExperienceLevel ?? '—' },
                        { label: 'Velocity',   value: `${aiResult[item.id].usedTeamVelocityAvg ?? '—'} SP` },
                        { label: 'Components', value: aiResult[item.id].usedNumComponents ?? '—' },
                        { label: 'External APIs', value: aiResult[item.id].usedExternalApis ?? '—' },
                        { label: 'Flags',      value: [
                            aiResult[item.id].usedHasIntegration ? 'Integration' : null,
                            aiResult[item.id].usedHasSecurity ? 'Security' : null,
                            aiResult[item.id].usedHasUiComplexity ? 'UI' : null,
                          ].filter(Boolean).join(', ') || 'none' },
                      ] : undefined}
                      status={aiStatus[item.id] ?? 'idle'}
                      onAccept={() => handleAccept(item.id)}
                      onReject={() => handleReject(item.id)}
                    />
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          ))}
        </div>
      )}

      {/* Estimation modal — collects every ML input (mirrors Jupyter notebook) */}
      <EstimationModal
        open={!!estimateItem}
        onClose={handleModalClose}
        item={estimateItem}
        onPredicted={(result) => {
          if (estimateItem) handlePredicted(estimateItem.id, result);
        }}
      />
    </div>
  );
}
