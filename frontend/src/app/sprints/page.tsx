'use client';
import { Suspense, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'next/navigation';
import { sprintsApi, projectsApi } from '@/lib/api';
import type { SprintRequest } from '@/lib/types';
import { Card, CardHeader, CardBody, Button, Input, Badge, Spinner, EmptyState, PageHeader, Select } from '@/components/ui';
import { Plus, Zap, Play, CheckCircle2, ArrowRight, Calendar, X } from 'lucide-react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';

const statusColor: Record<string, string> = { PLANNED: 'amber', ACTIVE: 'emerald', COMPLETED: 'blue' };

export default function SprintsPage() {
  return (
    <Suspense fallback={<Spinner />}>
      <SprintsPageInner />
    </Suspense>
  );
}

function SprintsPageInner() {
  const params = useSearchParams();
  const projectIdParam = params.get('project');
  const qc = useQueryClient();

  const { data: projects } = useQuery({ queryKey: ['projects'], queryFn: projectsApi.list });
  const [selectedProject, setSelectedProject] = useState<number | null>(projectIdParam ? Number(projectIdParam) : null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<SprintRequest>({ name: '', techStack: '', startDate: '', endDate: '', developerAvailability: 0.85, leaveDaysTotal: 0 });

  const TECH_STACKS = ['springboot', 'django', 'nodejs', 'rails', 'laravel', 'nextjs', 'fastapi', 'react', 'vue', 'angular', 'dotnet'];

  const projectId = selectedProject ?? (projects?.[0]?.id ?? null);
  const { data: sprints, isLoading } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: () => sprintsApi.listByProject(projectId!),
    enabled: !!projectId,
  });

  const createMut = useMutation({
    mutationFn: (req: SprintRequest) => sprintsApi.create(projectId!, req),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['sprints', projectId] }); setShowForm(false); },
  });

  const startMut = useMutation({
    mutationFn: (id: number) => sprintsApi.start(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sprints', projectId] }),
  });

  const completeMut = useMutation({
    mutationFn: (id: number) => sprintsApi.complete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sprints', projectId] }),
  });

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <PageHeader
        title="Sprints"
        subtitle="Plan, start and complete sprints with AI velocity predictions"
        actions={
          <Button onClick={() => setShowForm(true)} disabled={!projectId}>
            <Plus className="w-4 h-4" /> New Sprint
          </Button>
        }
      />

      {/* Project Selector */}
      <div className="flex items-center gap-3 mb-6">
        <label className="text-xs font-medium text-slate-600 uppercase tracking-wide">Project</label>
        <select
          value={selectedProject ?? ''}
          onChange={e => setSelectedProject(Number(e.target.value))}
          className="px-3 py-1.5 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500 shadow-sm"
        >
          <option value="">Select a project…</option>
          {projects?.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
      </div>

      {/* Create Form */}
      <AnimatePresence>
        {showForm && (
          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} className="mb-6 overflow-hidden">
            <Card className="border-violet-200">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-900">Create Sprint</h3>
                  <button onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </CardHeader>
              <CardBody>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="md:col-span-2">
                    <Input label="Sprint Name *" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="Sprint 1 — Core Features" />
                  </div>
                  <div>
                    <Select
                      label="Tech Stack"
                      value={form.techStack ?? ''}
                      onChange={e => setForm(f => ({ ...f, techStack: e.target.value }))}
                    >
                      <option value="">Use project default</option>
                      {TECH_STACKS.map(t => <option key={t} value={t}>{t}</option>)}
                    </Select>
                    <p className="text-xs text-slate-500 mt-1.5">Leave blank to inherit from project</p>
                  </div>
                  <Input label="Start Date" type="date" value={form.startDate} onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))} />
                  <Input label="End Date" type="date" value={form.endDate} onChange={e => setForm(f => ({ ...f, endDate: e.target.value }))} />
                  <Input label="Developer Availability (0–1)" type="number" step="0.05" min={0} max={1} value={form.developerAvailability} onChange={e => setForm(f => ({ ...f, developerAvailability: +e.target.value }))} />
                  <Input label="Leave Days Total" type="number" min={0} value={form.leaveDaysTotal} onChange={e => setForm(f => ({ ...f, leaveDaysTotal: +e.target.value }))} />
                </div>
                <div className="flex gap-2 mt-5">
                  <Button onClick={() => createMut.mutate(form)} disabled={!form.name || createMut.isPending}>
                    {createMut.isPending ? 'Creating…' : 'Create Sprint'}
                  </Button>
                  <Button variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button>
                </div>
              </CardBody>
            </Card>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Sprints */}
      {!projectId ? (
        <EmptyState icon={Zap} title="Select a project" description="Choose a project to view its sprints" />
      ) : isLoading ? (
        <Spinner />
      ) : !sprints?.length ? (
        <EmptyState icon={Zap} title="No sprints yet" action={<Button size="sm" onClick={() => setShowForm(true)}><Plus className="w-3 h-3" /> Create First Sprint</Button>} />
      ) : (
        <div className="space-y-3">
          {sprints.map(s => (
            <motion.div key={s.id} layout>
              <Card className="card-hover">
                <CardBody>
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex items-start gap-3 min-w-0 flex-1">
                      <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0 mt-1.5 ${
                        s.status === 'ACTIVE'    ? 'bg-emerald-500 animate-pulse' :
                        s.status === 'COMPLETED' ? 'bg-blue-500' : 'bg-amber-500'
                      }`} />
                      <div className="min-w-0">
                        <h3 className="text-sm font-semibold text-slate-900 truncate">{s.name}</h3>
                        <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                          <Badge variant={statusColor[s.status] as any}>{s.status}</Badge>
                          {s.techStack && <Badge variant="violet">{s.techStack}</Badge>}
                          {s.startDate && (
                            <span className="text-xs text-slate-500 flex items-center gap-1">
                              <Calendar className="w-3 h-3" /> {s.startDate} → {s.endDate ?? '?'}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0">
                      {s.status === 'PLANNED' && (
                        <Button size="sm" variant="success" onClick={() => startMut.mutate(s.id)}>
                          <Play className="w-3 h-3" /> Start
                        </Button>
                      )}
                      {s.status === 'ACTIVE' && (
                        <Button size="sm" variant="secondary" onClick={() => completeMut.mutate(s.id)}>
                          <CheckCircle2 className="w-3 h-3" /> Complete
                        </Button>
                      )}
                      <Link href={`/sprints/${s.id}`}>
                        <Button size="sm" variant="ghost">
                          Details <ArrowRight className="w-3 h-3" />
                        </Button>
                      </Link>
                    </div>
                  </div>

                  {/* Velocity + scope row */}
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-4">
                    {[
                      { label: 'Items',     value: s.backlogItemCount ?? 0, unit: '' },
                      { label: 'Planned',   value: s.totalPlannedSP ?? 0,   unit: 'SP' },
                      { label: 'Predicted', value: s.predictedVelocity ?? '—', unit: 'SP' },
                      { label: 'Actual',    value: s.actualVelocity ?? '—',    unit: 'SP' },
                    ].map(({ label, value, unit }) => (
                      <div key={label} className="bg-slate-50 border border-slate-100 rounded-lg px-3 py-2.5 text-center">
                        <p className="text-xs text-slate-500">{label}</p>
                        <p className="text-base font-semibold text-slate-900 mt-0.5">
                          {value} {unit && <span className="text-xs text-slate-500">{unit}</span>}
                        </p>
                      </div>
                    ))}
                  </div>
                </CardBody>
              </Card>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
