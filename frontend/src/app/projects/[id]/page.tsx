'use client';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'next/navigation';
import { projectsApi, sprintsApi, backlogApi, dashboardApi } from '@/lib/api';
import { Card, CardHeader, CardBody, StatCard, Badge, Button, Spinner, PageHeader, EmptyState } from '@/components/ui';
import { Zap, BarChart3, TrendingUp, AlertTriangle, CheckCircle2, ArrowRight, Plus } from 'lucide-react';
import Link from 'next/link';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line } from 'recharts';

const statusColor: Record<string, string> = {
  PLANNED: 'amber', ACTIVE: 'emerald', COMPLETED: 'blue',
};

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);

  const { data: project } = useQuery({ queryKey: ['project', projectId], queryFn: () => projectsApi.get(projectId) });
  const { data: sprints, isLoading: sprintsLoading } = useQuery({ queryKey: ['sprints', projectId], queryFn: () => sprintsApi.listByProject(projectId) });
  const { data: backlog } = useQuery({ queryKey: ['backlog', projectId], queryFn: () => backlogApi.listByProject(projectId) });
  const { data: summary } = useQuery({ queryKey: ['summary', projectId], queryFn: () => dashboardApi.projectSummary(projectId) });
  const { data: velHistory } = useQuery({ queryKey: ['velHistory', projectId], queryFn: () => dashboardApi.velocityHistory(projectId) });

  if (!project || sprintsLoading) return <Spinner />;

  const completedSprints = sprints?.filter(s => s.status === 'COMPLETED') ?? [];
  const activeSprint = sprints?.find(s => s.status === 'ACTIVE');
  const totalSP = backlog?.reduce((a, b) => a + (b.storyPoints ?? 0), 0) ?? 0;
  const doneSP = backlog?.filter(b => b.status === 'DONE').reduce((a, b) => a + (b.storyPoints ?? 0), 0) ?? 0;

  const velocityChartData = (velHistory ?? []).map(v => ({
    name: v.sprintName?.replace('Sprint ', 'S') ?? '—',
    predicted: v.predictedVelocity ?? 0,
    actual: v.actualVelocity ?? 0,
  }));

  const spStatusData = [
    { name: 'Done',        value: backlog?.filter(b => b.status === 'DONE').length ?? 0,        fill: '#10b981' },
    { name: 'In Progress', value: backlog?.filter(b => b.status === 'IN_PROGRESS').length ?? 0, fill: '#7c3aed' },
    { name: 'Todo',        value: backlog?.filter(b => b.status === 'TODO').length ?? 0,        fill: '#cbd5e1' },
  ];

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <PageHeader
        title={project.name}
        subtitle={project.description ?? `${project.domain ?? 'General'} · ${project.teamSize ?? '?'} developers`}
        actions={
          <div className="flex gap-2">
            <Link href={`/backlog?project=${projectId}`}>
              <Button variant="secondary" size="sm"><BarChart3 className="w-3 h-3" /> Backlog</Button>
            </Link>
            <Link href={`/sprints?project=${projectId}`}>
              <Button size="sm"><Zap className="w-3 h-3" /> Sprints</Button>
            </Link>
          </div>
        }
      />

      {/* Stats Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Sprints"        value={sprints?.length ?? 0}                        icon={Zap}           color="violet"  sub={`${completedSprints.length} completed`} />
        <StatCard label="Backlog Items"  value={backlog?.length ?? 0}                        icon={BarChart3}     color="blue"    sub={`${backlog?.filter(b => !b.storyPoints).length ?? 0} unestimated`} />
        <StatCard label="Story Points"   value={totalSP}                                     icon={TrendingUp}    color="emerald" sub={`${doneSP} completed`} />
        <StatCard label="High Risk"      value={summary?.highRiskItems ?? 0}                 icon={AlertTriangle} color="red"     sub="Need attention" />
      </div>

      {/* Active Sprint Banner */}
      {activeSprint && (
        <div className="mb-6 rounded-xl border border-emerald-200 bg-gradient-to-r from-emerald-50 to-white p-4 flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-3">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-60" />
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500" />
            </span>
            <div>
              <p className="text-sm font-semibold text-emerald-800">Active Sprint: {activeSprint.name}</p>
              <p className="text-xs text-emerald-700/80">
                {activeSprint.endDate ? `Ends ${activeSprint.endDate}` : 'No end date'} ·
                Predicted: {activeSprint.predictedVelocity ?? '?'} SP
              </p>
            </div>
          </div>
          <Link href={`/sprints/${activeSprint.id}`}>
            <Button size="sm" variant="success">View Sprint <ArrowRight className="w-3 h-3" /></Button>
          </Link>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Velocity Chart */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <h2 className="text-sm font-semibold text-slate-900">Velocity History</h2>
            </CardHeader>
            <CardBody>
              {velocityChartData.length > 0 ? (
                <ResponsiveContainer width="100%" height={220}>
                  <LineChart data={velocityChartData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="name" tick={{ fill: '#64748b', fontSize: 11 }} />
                    <YAxis tick={{ fill: '#64748b', fontSize: 11 }} />
                    <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: 8, fontSize: 12 }} />
                    <Line type="monotone" dataKey="predicted" stroke="#7c3aed" strokeWidth={2} dot={{ fill: '#7c3aed', r: 3 }} name="Predicted" />
                    <Line type="monotone" dataKey="actual"    stroke="#10b981" strokeWidth={2} dot={{ fill: '#10b981', r: 3 }} name="Actual" />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState icon={TrendingUp} title="No velocity data yet" description="Complete sprints to see velocity trends" />
              )}
            </CardBody>
          </Card>
        </div>

        {/* Backlog Status */}
        <Card>
          <CardHeader>
            <h2 className="text-sm font-semibold text-slate-900">Backlog Status</h2>
          </CardHeader>
          <CardBody>
            {spStatusData.some(d => d.value > 0) ? (
              <>
                <ResponsiveContainer width="100%" height={150}>
                  <BarChart data={spStatusData} layout="vertical">
                    <XAxis type="number" tick={{ fill: '#64748b', fontSize: 11 }} />
                    <YAxis type="category" dataKey="name" tick={{ fill: '#64748b', fontSize: 11 }} width={80} />
                    <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e2e8f0', borderRadius: 8, fontSize: 12 }} />
                    <Bar dataKey="value" radius={4} />
                  </BarChart>
                </ResponsiveContainer>
                <div className="mt-3 space-y-1.5">
                  {[
                    { label: 'Unestimated', value: summary?.unestimatedItems ?? 0, color: 'text-amber-700' },
                    { label: 'Ambiguous',   value: summary?.ambiguousItems ?? 0,   color: 'text-red-700' },
                    { label: 'High Risk',   value: summary?.highRiskItems ?? 0,    color: 'text-red-700' },
                  ].map(({ label, value, color }) => (
                    <div key={label} className="flex justify-between text-xs">
                      <span className="text-slate-500">{label}</span>
                      <span className={`font-medium ${color}`}>{value}</span>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <EmptyState icon={BarChart3} title="No backlog items" description="Add stories to get started" />
            )}
          </CardBody>
        </Card>
      </div>

      {/* Sprints List */}
      <Card className="mt-6">
        <CardHeader>
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900">Sprints</h2>
            <Link href={`/sprints?project=${projectId}`}>
              <Button size="sm" variant="ghost"><Plus className="w-3 h-3" /> New Sprint</Button>
            </Link>
          </div>
        </CardHeader>
        <CardBody className="p-0">
          {!sprints?.length ? (
            <EmptyState icon={Zap} title="No sprints yet" description="Create your first sprint to start planning" />
          ) : (
            <div className="divide-y divide-slate-100">
              {sprints.map(s => (
                <Link key={s.id} href={`/sprints/${s.id}`}
                  className="flex items-center justify-between px-5 py-3 hover:bg-slate-50 transition-colors group"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <Badge variant={statusColor[s.status] as any}>{s.status}</Badge>
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-slate-900 truncate">{s.name}</p>
                      <p className="text-xs text-slate-500">
                        {s.startDate ?? '—'} → {s.endDate ?? '?'} · Predicted: {s.predictedVelocity ?? '?'} SP
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    {s.status === 'COMPLETED' && s.actualVelocity && (
                      <span className="text-xs text-emerald-700 flex items-center gap-1 font-medium">
                        <CheckCircle2 className="w-3 h-3" /> {s.actualVelocity} SP
                      </span>
                    )}
                    <ArrowRight className="w-4 h-4 text-slate-300 group-hover:text-slate-500" />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
