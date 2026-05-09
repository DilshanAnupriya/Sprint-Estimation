'use client';
import { useQuery } from '@tanstack/react-query';
import { projectsApi } from '@/lib/api';
import { Spinner, StatCard, Card, CardHeader, CardBody, EmptyState, Badge } from '@/components/ui';
import { FolderKanban, Zap, BarChart3, Brain, TrendingUp, AlertTriangle, Plus, ArrowRight, Sparkles } from 'lucide-react';
import Link from 'next/link';

export default function DashboardPage() {
  const { data: projects, isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.list,
  });

  if (isLoading) return <Spinner />;

  const totalProjects = projects?.length ?? 0;

  return (
    <div className="p-8 max-w-7xl mx-auto">
      {/* Hero Header */}
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900 tracking-tight">Welcome back</h1>
            <p className="text-sm text-slate-500 mt-1">AI-powered Agile estimation — story points and velocity, on demand.</p>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <StatCard label="Projects"      value={totalProjects} icon={FolderKanban} color="violet"  sub="Active workspaces" />
        <StatCard label="AI Models"     value="2 Active"      icon={Brain}        color="blue"    sub="SP + Velocity" />
        <StatCard label="Accuracy"      value="≈ 85%"         icon={TrendingUp}   color="emerald" sub="Story-point precision" />
        <StatCard label="Risk Detection" value="On"           icon={AlertTriangle} color="amber"   sub="Ambiguity & overcommit" />
      </div>

      {/* Projects List + Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold text-slate-900">Recent Projects</h2>
                <Link href="/projects" className="text-xs text-violet-600 hover:text-violet-700 font-medium inline-flex items-center gap-1">
                  View all <ArrowRight className="w-3 h-3" />
                </Link>
              </div>
            </CardHeader>
            <CardBody className="p-0">
              {!projects?.length ? (
                <EmptyState
                  icon={FolderKanban}
                  title="No projects yet"
                  description="Create your first project to start AI-powered estimation"
                  action={
                    <Link href="/projects" className="text-xs text-violet-600 hover:text-violet-700 flex items-center gap-1 font-medium">
                      <Plus className="w-3 h-3" /> Create Project
                    </Link>
                  }
                />
              ) : (
                <div className="divide-y divide-slate-100">
                  {projects.slice(0, 6).map(p => (
                    <Link key={p.id} href={`/projects/${p.id}`}
                      className="flex items-center justify-between px-5 py-3.5 hover:bg-slate-50 transition-colors group"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-9 h-9 rounded-lg bg-violet-50 flex items-center justify-center flex-shrink-0">
                          <FolderKanban className="w-4 h-4 text-violet-600" />
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-slate-900 truncate">{p.name}</p>
                          <p className="text-xs text-slate-500">
                            {p.domain ?? 'General'} · {p.teamMemberCount ?? p.teamSize ?? 0} members · {p.sprintCount ?? 0} sprints · {p.backlogItemCount ?? 0} stories
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        {p.defaultTechStack && <Badge variant="violet">{p.defaultTechStack}</Badge>}
                        <ArrowRight className="w-4 h-4 text-slate-300 group-hover:text-slate-500" />
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>
        </div>

        {/* Quick Actions */}
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <h2 className="text-sm font-semibold text-slate-900">Quick Actions</h2>
            </CardHeader>
            <CardBody className="space-y-1">
              {[
                { href: '/projects?new=1', icon: FolderKanban, label: 'New Project', desc: 'Start a workspace' },
                { href: '/backlog?new=1',  icon: BarChart3,    label: 'Add Story',   desc: 'Create a backlog item' },
                { href: '/sprints?new=1',  icon: Zap,          label: 'Plan Sprint', desc: 'Set up a sprint' },
              ].map(({ href, icon: Icon, label, desc }) => (
                <Link key={href} href={href}
                  className="flex items-center gap-3 p-2.5 rounded-lg hover:bg-slate-50 transition-colors group"
                >
                  <div className="w-9 h-9 rounded-lg bg-slate-100 group-hover:bg-violet-50 flex items-center justify-center transition-colors">
                    <Icon className="w-4 h-4 text-slate-500 group-hover:text-violet-600 transition-colors" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-900">{label}</p>
                    <p className="text-xs text-slate-500">{desc}</p>
                  </div>
                </Link>
              ))}
            </CardBody>
          </Card>

          {/* AI Tips */}
          <Card className="border-violet-200 bg-gradient-to-br from-violet-50 to-white">
            <CardBody>
              <div className="flex items-center gap-2 mb-3">
                <Sparkles className="w-4 h-4 text-violet-600" />
                <span className="text-xs font-semibold text-violet-700 uppercase tracking-wide">AI Tips</span>
              </div>
              <ul className="space-y-2 text-xs text-slate-600">
                <li className="flex items-start gap-2">
                  <span className="text-violet-500 mt-0.5">•</span>
                  Write detailed user stories for sharper SP estimates
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-violet-500 mt-0.5">•</span>
                  Accept velocity suggestions after 2+ completed sprints
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-violet-500 mt-0.5">•</span>
                  Flag ambiguous items before sprint planning
                </li>
              </ul>
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  );
}
