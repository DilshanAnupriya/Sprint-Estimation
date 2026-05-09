'use client';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'next/navigation';
import { Suspense, useMemo, useState } from 'react';
import { teamApi, projectsApi } from '@/lib/api';
import type { TeamMember } from '@/lib/types';
import { Card, CardBody, CardHeader, Badge, Button, Input, Select, Spinner, EmptyState, PageHeader } from '@/components/ui';
import { Users, Plus, Trash2, X, UserPlus, FolderKanban, Search } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { clsx } from 'clsx';

const expColors: Record<string, 'blue' | 'violet' | 'emerald' | 'default'> = { JUNIOR: 'blue', MID: 'violet', SENIOR: 'emerald' };

export default function TeamPage() {
  return (
    <Suspense fallback={<Spinner />}>
      <TeamPageInner />
    </Suspense>
  );
}

function TeamPageInner() {
  const params = useSearchParams();
  const projectIdParam = params.get('project');
  const qc = useQueryClient();

  const { data: projects } = useQuery({ queryKey: ['projects'], queryFn: projectsApi.list });
  const [selectedProject, setSelectedProject] = useState<number | null>(projectIdParam ? Number(projectIdParam) : null);

  // Tabs: 'project' shows devs assigned to selected project; 'all' shows the global pool
  const [tab, setTab] = useState<'project' | 'all'>('project');

  const projectId = selectedProject ?? (projects?.[0]?.id ?? null);

  const { data: projectMembers, isLoading: loadingProject } = useQuery({
    queryKey: ['team', projectId],
    queryFn: () => teamApi.listByProject(projectId!),
    enabled: !!projectId,
  });

  const { data: allDevelopers, isLoading: loadingAll } = useQuery({
    queryKey: ['developers'],
    queryFn: teamApi.listAll,
  });

  // ── Mutations ────────────────────────────────────────────────────────────
  const createMut = useMutation({
    mutationFn: (req: Partial<TeamMember>) => teamApi.create(projectId!, req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['team', projectId] });
      qc.invalidateQueries({ queryKey: ['developers'] });
      qc.invalidateQueries({ queryKey: ['projects'] });
      setShowCreateForm(false);
    },
  });

  const assignMut = useMutation({
    mutationFn: (memberId: number) => teamApi.assign(projectId!, memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['team', projectId] });
      qc.invalidateQueries({ queryKey: ['developers'] });
      qc.invalidateQueries({ queryKey: ['projects'] });
    },
  });

  const unassignMut = useMutation({
    mutationFn: (memberId: number) => teamApi.unassign(projectId!, memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['team', projectId] });
      qc.invalidateQueries({ queryKey: ['developers'] });
      qc.invalidateQueries({ queryKey: ['projects'] });
    },
  });

  const removeMut = useMutation({
    mutationFn: (memberId: number) => teamApi.remove(memberId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['team', projectId] });
      qc.invalidateQueries({ queryKey: ['developers'] });
      qc.invalidateQueries({ queryKey: ['projects'] });
    },
  });

  // ── Forms ────────────────────────────────────────────────────────────────
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showPicker, setShowPicker] = useState(false);
  const [pickerSearch, setPickerSearch] = useState('');
  const [form, setForm] = useState({
    name: '', email: '', role: 'Developer',
    experienceLevel: 'MID' as 'JUNIOR' | 'MID' | 'SENIOR', experienceYears: 3,
  });

  // Devs in the global pool that aren't on this project yet
  const availableDevelopers = useMemo<TeamMember[]>(() => {
    if (!allDevelopers || !projectId) return [];
    return allDevelopers.filter(dev => !dev.projectIds?.includes(projectId));
  }, [allDevelopers, projectId]);

  const filteredAvailable = useMemo(() => {
    const q = pickerSearch.trim().toLowerCase();
    if (!q) return availableDevelopers;
    return availableDevelopers.filter(d =>
      d.name.toLowerCase().includes(q) || d.email?.toLowerCase().includes(q)
    );
  }, [availableDevelopers, pickerSearch]);

  // Show whichever list the active tab points at
  const visibleDevs = tab === 'project' ? (projectMembers ?? []) : (allDevelopers ?? []);
  const isLoading = tab === 'project' ? loadingProject : loadingAll;

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <PageHeader
        title="Team"
        subtitle="Global developer pool · Jira-style — one developer can work on many projects"
        actions={
          <div className="flex gap-2">
            {tab === 'project' && projectId && (
              <>
                <Button variant="secondary" onClick={() => setShowPicker(true)} disabled={!availableDevelopers.length}>
                  <UserPlus className="w-4 h-4" /> Assign Existing
                </Button>
                <Button onClick={() => setShowCreateForm(true)}>
                  <Plus className="w-4 h-4" /> New Developer
                </Button>
              </>
            )}
          </div>
        }
      />

      {/* Project selector + tab toggle */}
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
          {([
            { key: 'project', label: `On Project (${projectMembers?.length ?? 0})` },
            { key: 'all',     label: `Global Pool (${allDevelopers?.length ?? 0})` },
          ] as const).map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setTab(key)}
              className={clsx(
                'px-3 py-1 rounded-md text-xs font-medium transition-colors',
                tab === key ? 'bg-violet-600 text-white shadow-sm' : 'text-slate-600 hover:text-slate-900',
              )}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Create form */}
      <AnimatePresence>
        {showCreateForm && (
          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} className="mb-6 overflow-hidden">
            <Card className="border-violet-200">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-900">Add New Developer</h3>
                  <button onClick={() => setShowCreateForm(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </CardHeader>
              <CardBody>
                <p className="text-xs text-slate-500 mb-4">
                  Creates a global developer and adds them to this project. If the email matches an existing
                  developer, they're reused — same person, multiple projects.
                </p>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Input label="Name *" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="Full name" />
                  <Input label="Email" type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} placeholder="dev@example.com" />
                  <Input label="Role" value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))} placeholder="Developer, QA, Designer…" />
                  <Select label="Experience Level" value={form.experienceLevel} onChange={e => setForm(f => ({ ...f, experienceLevel: e.target.value as 'JUNIOR' | 'MID' | 'SENIOR' }))}>
                    <option value="JUNIOR">Junior</option>
                    <option value="MID">Mid</option>
                    <option value="SENIOR">Senior</option>
                  </Select>
                  <Input label="Experience (years)" type="number" min={0} max={40} value={form.experienceYears} onChange={e => setForm(f => ({ ...f, experienceYears: +e.target.value }))} />
                </div>
                <div className="flex gap-2 mt-5">
                  <Button onClick={() => createMut.mutate(form)} disabled={!form.name || createMut.isPending}>
                    {createMut.isPending ? 'Adding…' : 'Add Developer'}
                  </Button>
                  <Button variant="secondary" onClick={() => setShowCreateForm(false)}>Cancel</Button>
                </div>
              </CardBody>
            </Card>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Team list */}
      {tab === 'project' && !projectId ? (
        <EmptyState icon={Users} title="Select a project" description="Pick a project to see its developers" />
      ) : isLoading ? (
        <Spinner />
      ) : !visibleDevs.length ? (
        <EmptyState
          icon={Users}
          title={tab === 'project' ? 'No developers on this project' : 'No developers in the global pool'}
          description={tab === 'project' ? 'Add a new developer or assign an existing one from the global pool.' : 'Create a developer from any project — they\'ll appear here.'}
          action={
            tab === 'project' && projectId ? (
              <div className="flex gap-2">
                {availableDevelopers.length > 0 && (
                  <Button size="sm" variant="secondary" onClick={() => setShowPicker(true)}>
                    <UserPlus className="w-3 h-3" /> Assign Existing
                  </Button>
                )}
                <Button size="sm" onClick={() => setShowCreateForm(true)}>
                  <Plus className="w-3 h-3" /> New Developer
                </Button>
              </div>
            ) : undefined
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {visibleDevs.map(m => (
            <motion.div key={m.id} layout>
              <Card className="card-hover">
                <CardBody>
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-violet-500 to-indigo-500 flex items-center justify-center text-sm font-semibold text-white flex-shrink-0">
                        {m.name.charAt(0).toUpperCase()}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-900 truncate">{m.name}</p>
                        <p className="text-xs text-slate-500 truncate">{m.role ?? 'Developer'} · {m.email ?? '—'}</p>
                        <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
                          <Badge variant={expColors[m.experienceLevel ?? 'MID']}>{m.experienceLevel}</Badge>
                          <span className="text-xs text-slate-500">{m.experienceYears ?? 0}y</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-1 flex-shrink-0">
                      {tab === 'project' && projectId && (
                        <button
                          onClick={() => unassignMut.mutate(m.id)}
                          title="Remove from project"
                          className="text-slate-400 hover:text-amber-600 transition-colors p-1.5 rounded hover:bg-amber-50"
                        >
                          <UserPlus className="w-4 h-4 rotate-180" />
                        </button>
                      )}
                      <button
                        onClick={() => {
                          if (confirm(`Permanently delete ${m.name} from the global developer pool?`)) {
                            removeMut.mutate(m.id);
                          }
                        }}
                        title="Delete from global pool"
                        className="text-slate-300 hover:text-red-500 transition-colors p-1.5 rounded hover:bg-red-50"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                  {/* Project list — Jira-style */}
                  {m.projectNames && m.projectNames.length > 0 && (
                    <div className="mt-3 pt-3 border-t border-slate-100">
                      <p className="text-xs text-slate-500 mb-1.5 flex items-center gap-1">
                        <FolderKanban className="w-3 h-3" /> On {m.projectNames.length} project{m.projectNames.length !== 1 ? 's' : ''}
                      </p>
                      <div className="flex flex-wrap gap-1">
                        {m.projectNames.map((pn, i) => (
                          <Badge key={i} variant="default">{pn}</Badge>
                        ))}
                      </div>
                    </div>
                  )}
                </CardBody>
              </Card>
            </motion.div>
          ))}
        </div>
      )}

      {/* Pick existing developer modal */}
      {showPicker && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm" onClick={() => setShowPicker(false)}>
          <div
            className="w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden flex flex-col max-h-[80vh]"
            onClick={e => e.stopPropagation()}
          >
            <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
              <div>
                <h3 className="text-base font-semibold text-slate-900">Assign Developer</h3>
                <p className="text-xs text-slate-500 mt-0.5">From the global pool ({availableDevelopers.length} available)</p>
              </div>
              <button onClick={() => setShowPicker(false)} className="text-slate-400 hover:text-slate-700 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="px-5 py-3 border-b border-slate-100">
              <div className="relative">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  autoFocus
                  value={pickerSearch}
                  onChange={e => setPickerSearch(e.target.value)}
                  placeholder="Search by name or email…"
                  className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500"
                />
              </div>
            </div>
            <div className="flex-1 overflow-y-auto">
              {filteredAvailable.length === 0 ? (
                <div className="px-5 py-12 text-center">
                  <p className="text-sm text-slate-500">
                    {pickerSearch ? 'No developers match your search' : 'All global developers are already on this project'}
                  </p>
                </div>
              ) : (
                <div className="divide-y divide-slate-100">
                  {filteredAvailable.map(d => (
                    <div key={d.id} className="flex items-center gap-3 px-5 py-3 hover:bg-slate-50">
                      <div className="w-9 h-9 rounded-full bg-slate-200 flex items-center justify-center text-sm font-semibold text-slate-700 flex-shrink-0">
                        {d.name.charAt(0).toUpperCase()}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-slate-900 truncate">{d.name}</p>
                        <p className="text-xs text-slate-500 truncate">{d.email ?? '—'} · {d.experienceLevel} · {d.projectIds?.length ?? 0} projects</p>
                      </div>
                      <Button
                        size="sm"
                        onClick={() => assignMut.mutate(d.id)}
                        disabled={assignMut.isPending}
                      >
                        <Plus className="w-3 h-3" /> Assign
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="px-5 py-3 border-t border-slate-200 bg-slate-50 flex justify-end">
              <Button variant="secondary" onClick={() => setShowPicker(false)}>Done</Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
