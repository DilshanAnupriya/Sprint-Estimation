'use client';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectsApi } from '@/lib/api';
import type { ProjectRequest } from '@/lib/types';
import { Card, CardHeader, CardBody, Button, Input, Select, Textarea, Spinner, EmptyState, Badge, PageHeader } from '@/components/ui';
import { Plus, FolderKanban, Users, Calendar, Trash2, ArrowRight, X, Brain, Zap } from 'lucide-react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';

const DOMAINS = ['finance', 'ecommerce', 'healthcare', 'education', 'logistics', 'general'];
const TECH_STACKS = ['springboot', 'django', 'nodejs', 'rails', 'laravel', 'nextjs', 'fastapi', 'react'];

export default function ProjectsPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<ProjectRequest>({
    name: '', description: '', domain: 'general', defaultTechStack: 'springboot',
    teamSize: 5, defaultSprintDurationDays: 14,
    defaultTeamVelocity: 25, defaultCompletionRate: 0.85, defaultDevExperienceLevel: 'MID',
  });

  const { data: projects, isLoading } = useQuery({ queryKey: ['projects'], queryFn: projectsApi.list });

  const createMut = useMutation({
    mutationFn: (req: ProjectRequest) => projectsApi.create(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['projects'] });
      setShowForm(false);
      setForm({
        name: '', description: '', domain: 'general', defaultTechStack: 'springboot',
        teamSize: 5, defaultSprintDurationDays: 14,
        defaultTeamVelocity: 25, defaultCompletionRate: 0.85, defaultDevExperienceLevel: 'MID',
      });
    },
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => projectsApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['projects'] }),
  });

  if (isLoading) return <Spinner />;

  return (
    <div className="p-8 max-w-6xl mx-auto">
      <PageHeader
        title="Projects"
        subtitle={`${projects?.length ?? 0} active workspace${projects?.length !== 1 ? 's' : ''}`}
        actions={
          <Button onClick={() => setShowForm(true)}>
            <Plus className="w-4 h-4" /> New Project
          </Button>
        }
      />

      {/* Create Form */}
      <AnimatePresence>
        {showForm && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="mb-6 overflow-hidden"
          >
            <Card className="border-violet-200">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-semibold text-slate-900">Create New Project</h3>
                  <button onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </CardHeader>
              <CardBody>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="md:col-span-2">
                    <Input label="Project Name *" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="e.g. E-Commerce Platform" />
                  </div>
                  <div className="md:col-span-2">
                    <Textarea label="Description" value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} rows={2} placeholder="What is this project about?" />
                  </div>
                  <Select label="Domain" value={form.domain} onChange={e => setForm(f => ({ ...f, domain: e.target.value }))}>
                    {DOMAINS.map(d => <option key={d} value={d}>{d}</option>)}
                  </Select>
                  <Select label="Default Tech Stack" value={form.defaultTechStack} onChange={e => setForm(f => ({ ...f, defaultTechStack: e.target.value }))}>
                    {TECH_STACKS.map(t => <option key={t} value={t}>{t}</option>)}
                  </Select>
                  <Input label="Team Size" type="number" min={1} max={50} value={form.teamSize} onChange={e => setForm(f => ({ ...f, teamSize: +e.target.value }))} />
                  <Input label="Sprint Duration (days)" type="number" min={7} max={60} value={form.defaultSprintDurationDays} onChange={e => setForm(f => ({ ...f, defaultSprintDurationDays: +e.target.value }))} />
                </div>

                {/* AI Estimation Context */}
                <div className="mt-6 p-4 rounded-xl border border-violet-200 bg-violet-50/40">
                  <div className="flex items-center gap-2 mb-3">
                    <Brain className="w-4 h-4 text-violet-600" />
                    <span className="text-xs font-semibold text-violet-700 uppercase tracking-wide">AI Estimation Context</span>
                    <span className="text-xs text-slate-500 font-normal normal-case">— used until you complete sprints</span>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div>
                      <Input
                        label="Team Velocity (SP/sprint)"
                        type="number" min={1} max={100}
                        value={form.defaultTeamVelocity ?? ''}
                        onChange={e => setForm(f => ({ ...f, defaultTeamVelocity: +e.target.value }))}
                        placeholder="e.g. 28"
                      />
                      <p className="text-xs text-slate-500 mt-1.5">SP your team typically delivers</p>
                    </div>
                    <div>
                      <Input
                        label="Completion Rate (0.5–1.0)"
                        type="number" min={0.5} max={1.0} step={0.05}
                        value={form.defaultCompletionRate ?? ''}
                        onChange={e => setForm(f => ({ ...f, defaultCompletionRate: +e.target.value }))}
                        placeholder="e.g. 0.85"
                      />
                      <p className="text-xs text-slate-500 mt-1.5">% of committed SP done</p>
                    </div>
                    <div>
                      <Select
                        label="Default Experience"
                        value={form.defaultDevExperienceLevel ?? 'MID'}
                        onChange={e => setForm(f => ({ ...f, defaultDevExperienceLevel: e.target.value as 'JUNIOR' | 'MID' | 'SENIOR' }))}
                      >
                        <option value="JUNIOR">Junior</option>
                        <option value="MID">Mid</option>
                        <option value="SENIOR">Senior</option>
                      </Select>
                      <p className="text-xs text-slate-500 mt-1.5">Avg team level fallback</p>
                    </div>
                  </div>
                  <div className="flex items-start gap-2 mt-3 text-xs text-violet-700 bg-white border border-violet-100 rounded-lg px-3 py-2">
                    <Zap className="w-3 h-3 mt-0.5 flex-shrink-0" />
                    Once you complete sprints, the AI switches to your real sprint data automatically.
                  </div>
                </div>

                <div className="flex gap-2 mt-5">
                  <Button onClick={() => createMut.mutate(form)} disabled={!form.name || createMut.isPending}>
                    {createMut.isPending ? 'Creating…' : 'Create Project'}
                  </Button>
                  <Button variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button>
                </div>
              </CardBody>
            </Card>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Projects Grid */}
      {!projects?.length ? (
        <EmptyState
          icon={FolderKanban}
          title="No projects yet"
          description="Create your first project to start estimating story points with AI"
          action={<Button onClick={() => setShowForm(true)}><Plus className="w-4 h-4" /> Create First Project</Button>}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {projects.map(p => (
            <motion.div key={p.id} layout initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
              <Card className="card-hover">
                <CardBody>
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-10 h-10 rounded-xl bg-violet-50 flex items-center justify-center flex-shrink-0">
                        <FolderKanban className="w-5 h-5 text-violet-600" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="text-sm font-semibold text-slate-900 truncate">{p.name}</h3>
                        <p className="text-xs text-slate-500 line-clamp-1">{p.description ?? 'No description'}</p>
                      </div>
                    </div>
                    <button onClick={() => deleteMut.mutate(p.id)} className="text-slate-300 hover:text-red-500 transition-colors p-1">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                  <div className="flex flex-wrap gap-1.5 mb-4">
                    {p.domain && <Badge variant="violet">{p.domain}</Badge>}
                    {p.defaultTechStack && <Badge variant="blue">{p.defaultTechStack}</Badge>}
                    {p.defaultSprintDurationDays && (
                      <Badge variant="default">
                        <Calendar className="w-3 h-3" />{p.defaultSprintDurationDays}d
                      </Badge>
                    )}
                  </div>
                  {/* Live counts from backend relationships */}
                  <div className="grid grid-cols-3 gap-2 mb-4">
                    {[
                      { label: 'Members',  value: p.teamMemberCount ?? 0,  icon: Users },
                      { label: 'Sprints',  value: p.sprintCount ?? 0 },
                      { label: 'Stories',  value: p.backlogItemCount ?? 0 },
                    ].map(({ label, value, icon: Icon }) => (
                      <div key={label} className="bg-slate-50 border border-slate-100 rounded-lg px-2.5 py-2 text-center">
                        <p className="text-xs text-slate-500 flex items-center justify-center gap-1">
                          {Icon && <Icon className="w-3 h-3" />}{label}
                        </p>
                        <p className="text-base font-semibold text-slate-900 mt-0.5">{value}</p>
                      </div>
                    ))}
                  </div>
                  <Link href={`/projects/${p.id}`}>
                    <Button variant="secondary" className="w-full justify-between">
                      Open Project <ArrowRight className="w-4 h-4" />
                    </Button>
                  </Link>
                </CardBody>
              </Card>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
