'use client';
import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { backlogApi, projectsApi, teamApi, sprintsApi, analyticsApi } from '@/lib/api';
import type { BacklogItem, EstimationOverrideRequest, StoryPointEstimate } from '@/lib/types';
import { Button } from '@/components/ui';
import { X, Sparkles, Brain, Info, Loader2, Calculator, Copy, Check } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const KNOWN_DOMAINS  = ['ecommerce', 'healthcare', 'finance', 'education', 'logistics'];
const TECH_STACKS    = ['nodejs', 'springboot', 'react', 'django', 'dotnet', 'flask', 'vue', 'angular', 'fastapi', 'rails'];
const TASK_TYPES     = ['feature', 'bug', 'enhancement', 'technical_debt', 'research'];

/** Concatenate title and stored userStory so TF-IDF gets all the keywords. */
function buildRichText(title?: string | null, userStory?: string | null): string {
  const t = (title ?? '').trim();
  const s = (userStory ?? '').trim();
  if (!t) return s;
  if (!s) return t;
  // Avoid duplicating if userStory already contains the title verbatim
  if (s.toLowerCase().includes(t.toLowerCase())) return s;
  return `${t}. ${s}`;
}

type Source = 'item' | 'project' | 'sprint' | 'auto' | 'override' | 'default';

/**
 * Comprehensive estimation modal — mirrors the Jupyter notebook's interactive
 * prompts. Pre-fills every input from item/project/auto-derived state, lets
 * the user review and override any of them, then runs the prediction.
 */
export function EstimationModal({
  open, onClose, item, onPredicted,
}: {
  open: boolean;
  onClose: () => void;
  item: BacklogItem | null;
  onPredicted?: (result: StoryPointEstimate) => void;
}) {
  const projectId = item?.projectId;

  // Pull context for auto-deriving fields
  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectsApi.get(projectId!),
    enabled: open && !!projectId,
  });
  const { data: backlog } = useQuery({
    queryKey: ['backlog', projectId],
    queryFn: () => backlogApi.listByProject(projectId!),
    enabled: open && !!projectId,
  });
  const { data: team } = useQuery({
    queryKey: ['team', projectId],
    queryFn: () => teamApi.listByProject(projectId!),
    enabled: open && !!projectId,
  });
  const { data: rolling } = useQuery({
    queryKey: ['rolling-velocity', projectId],
    queryFn: () => analyticsApi.rollingVelocity(projectId!),
    enabled: open && !!projectId,
  });

  // ── Auto-derived defaults (mirror what backend would compute) ────────────
  const derived = useMemo(() => {
    const now = Date.now();
    const taskAgeDays = item?.createdAt
      ? Math.max(0, Math.floor((now - new Date(item.createdAt).getTime()) / 86400000)) : 0;

    const similarTaskCount = backlog?.filter(b =>
      b.id !== item?.id && b.taskType === item?.taskType && b.status === 'DONE',
    ).length ?? 0;

    // Average experience years from team (or fall back to project default level)
    const expYears = team && team.length > 0
      ? team.filter(m => m.experienceYears != null)
            .reduce((sum, m, _i, arr) => sum + (m.experienceYears ?? 0) / Math.max(arr.length, 1), 0)
      : null;
    const inferredLevel: 'JUNIOR' | 'MID' | 'SENIOR' =
      expYears == null ? (project?.defaultDevExperienceLevel ?? 'MID')
      : expYears >= 6 ? 'SENIOR'
      : expYears >= 3 ? 'MID'
      : 'JUNIOR';

    const teamVelocityAvg =
      rolling && rolling.completedSprintCount > 0 && rolling.avgVelocityLast3 > 0
        ? Math.round(rolling.avgVelocityLast3)
        : (project?.defaultTeamVelocity ?? 20);

    // Backend doesn't expose recent completion rate directly; use project default
    const recentCompletionRate = project?.defaultCompletionRate ?? 0.85;

    return {
      domain:               project?.domain ?? '',
      techStack:            item?.techStack || project?.defaultTechStack || 'springboot',
      taskType:             (item?.taskType?.toLowerCase()) ?? 'feature',
      devExperienceLevel:   inferredLevel,
      teamSize:             team?.length || project?.teamSize || 5,
      sprintDurationDays:   project?.defaultSprintDurationDays ?? 14,
      teamVelocityAvg,
      recentCompletionRate,
      similarTaskCount,
      taskAgeDays,
      numComponents:        item?.numComponents ?? 1,
      externalApis:         item?.externalApis ?? 0,
      hasIntegration:       item?.hasIntegration ?? false,
      hasSecurity:          item?.hasSecurity ?? false,
      hasUiComplexity:      item?.hasUiComplexity ?? false,
      // Concatenate title + userStory for richer TF-IDF signal — same logic
      // the backend uses when no override is sent. The user can edit this.
      userStory:            buildRichText(item?.title, item?.userStory),
    };
  }, [project, backlog, team, rolling, item]);

  // ── Form state (initialised from derived; tracks per-field source) ───────
  const [form, setForm] = useState(derived);
  const [touched, setTouched] = useState<Set<keyof typeof derived>>(new Set());
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => { setForm(derived); setTouched(new Set()); }, [derived]);

  if (!open || !item) return null;

  const setField = <K extends keyof typeof derived>(key: K, value: (typeof derived)[K]) => {
    setForm(f => ({ ...f, [key]: value }));
    setTouched(t => new Set(t).add(key));
  };

  const sourceOf = (key: keyof typeof derived): Source => {
    if (touched.has(key)) return 'override';
    switch (key) {
      case 'numComponents': case 'externalApis':
      case 'hasIntegration': case 'hasSecurity': case 'hasUiComplexity':
        return item[key] != null ? 'item' : 'default';
      case 'userStory':
        // 'auto' if we built rich text (title + userStory), 'item' if just userStory copied straight through
        return item.title && item.userStory ? 'auto' : item.userStory ? 'item' : 'auto';
      case 'techStack':           return item.techStack ? 'item' : 'project';
      case 'taskAgeDays':         return 'auto';
      case 'similarTaskCount':    return 'auto';
      case 'teamSize':            return team && team.length > 0 ? 'auto' : 'project';
      case 'teamVelocityAvg':     return (rolling?.completedSprintCount ?? 0) > 0 ? 'auto' : 'project';
      case 'recentCompletionRate':return 'project';
      case 'devExperienceLevel':  return team && team.length > 0 ? 'auto' : 'project';
      case 'domain':              return project?.domain ? 'project' : 'default';
      case 'sprintDurationDays':  return 'project';
      case 'taskType':            return item.taskType ? 'item' : 'default';
      default: return 'default';
    }
  };

  const submit = async () => {
    setRunning(true);
    setError(null);
    try {
      const overrides: EstimationOverrideRequest = {};
      // Always send everything — the modal is a "explicit estimate" flow, like the notebook.
      // The backend treats any non-null override as authoritative.
      overrides.domain               = form.domain;
      overrides.techStack            = form.techStack;
      overrides.taskType             = form.taskType;
      overrides.devExperienceLevel   = form.devExperienceLevel;
      overrides.teamSize             = form.teamSize;
      overrides.sprintDurationDays   = form.sprintDurationDays;
      overrides.teamVelocityAvg      = form.teamVelocityAvg;
      overrides.recentCompletionRate = form.recentCompletionRate;
      overrides.similarTaskCount     = form.similarTaskCount;
      overrides.taskAgeDays          = form.taskAgeDays;
      overrides.numComponents        = form.numComponents;
      overrides.externalApis         = form.externalApis;
      overrides.hasIntegration       = form.hasIntegration;
      overrides.hasSecurity          = form.hasSecurity;
      overrides.hasUiComplexity      = form.hasUiComplexity;
      overrides.userStory            = form.userStory;

      const result = await backlogApi.estimate(item.id, overrides);
      onPredicted?.(result);
      onClose();
    } catch (e: any) {
      setError(e?.response?.data?.message ?? e?.message ?? 'Prediction failed');
    } finally {
      setRunning(false);
    }
  };

  const domainIsKnown = KNOWN_DOMAINS.includes(form.domain.toLowerCase());

  return (
    <AnimatePresence>
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm"
        onClick={onClose}
      >
        <motion.div
          initial={{ opacity: 0, scale: 0.97, y: 8 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.97, y: 8 }}
          transition={{ duration: 0.15 }}
          className="w-full max-w-3xl bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden flex flex-col max-h-[92vh]"
          onClick={e => e.stopPropagation()}
        >
          {/* Header */}
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between bg-gradient-to-r from-violet-50 to-indigo-50">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-lg bg-violet-600 flex items-center justify-center shadow-sm">
                <Brain className="w-5 h-5 text-white" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-slate-900">Story Point Prediction</h3>
                <p className="text-xs text-slate-600">Review every input before running the AI</p>
              </div>
            </div>
            <button onClick={onClose} className="text-slate-400 hover:text-slate-700 transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-5 space-y-5">
            {/* Story title (read-only context) */}
            <div className="bg-slate-50 border border-slate-200 rounded-lg px-3.5 py-2.5">
              <p className="text-xs text-slate-500 uppercase tracking-wide font-medium">Story</p>
              <p className="text-sm font-semibold text-slate-900 mt-0.5">{item.title}</p>
            </div>

            {/* Section: Story details */}
            <Section title="Story Details">
              <Field label="User Story Description" source={sourceOf('userStory')}>
                <textarea
                  rows={2}
                  value={form.userStory}
                  onChange={e => setField('userStory', e.target.value)}
                  placeholder="Describe what the user wants to do…"
                  className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500 resize-none"
                />
              </Field>
              <Grid cols={3}>
                <Field label="Task Type" source={sourceOf('taskType')}>
                  <select
                    value={form.taskType}
                    onChange={e => setField('taskType', e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500"
                  >
                    {TASK_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </Field>
                <Field label="Domain" source={sourceOf('domain')} hint={!domainIsKnown && form.domain ? `"${form.domain}" — zero-shot fallback` : `Known: ${KNOWN_DOMAINS.join(', ')}`}>
                  <input
                    list="known-domains"
                    value={form.domain}
                    onChange={e => setField('domain', e.target.value.toLowerCase())}
                    placeholder="ecommerce, healthcare, or any custom…"
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500"
                  />
                  <datalist id="known-domains">
                    {KNOWN_DOMAINS.map(d => <option key={d} value={d} />)}
                  </datalist>
                </Field>
                <Field label="Tech Stack" source={sourceOf('techStack')}>
                  <select
                    value={form.techStack}
                    onChange={e => setField('techStack', e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500"
                  >
                    {TECH_STACKS.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </Field>
              </Grid>
            </Section>

            {/* Section: Complexity features */}
            <Section title="Complexity Features">
              <Grid cols={2}>
                <Field label="# Components / Modules touched (1–7)" source={sourceOf('numComponents')}>
                  <NumberInput value={form.numComponents} min={1} max={7} onChange={v => setField('numComponents', v)} />
                </Field>
                <Field label="# External APIs / Integrations (0–5)" source={sourceOf('externalApis')}>
                  <NumberInput value={form.externalApis} min={0} max={5} onChange={v => setField('externalApis', v)} />
                </Field>
              </Grid>
              <Grid cols={3}>
                <CheckboxField label="Third-party integration" source={sourceOf('hasIntegration')}
                  checked={form.hasIntegration} onChange={v => setField('hasIntegration', v)} />
                <CheckboxField label="Security / auth / encryption" source={sourceOf('hasSecurity')}
                  checked={form.hasSecurity} onChange={v => setField('hasSecurity', v)} />
                <CheckboxField label="Significant UI complexity" source={sourceOf('hasUiComplexity')}
                  checked={form.hasUiComplexity} onChange={v => setField('hasUiComplexity', v)} />
              </Grid>
            </Section>

            {/* Section: Team & sprint context */}
            <Section title="Team & Sprint Context">
              <Grid cols={2}>
                <Field label="Developer Experience" source={sourceOf('devExperienceLevel')}>
                  <select
                    value={form.devExperienceLevel}
                    onChange={e => setField('devExperienceLevel', e.target.value as 'JUNIOR' | 'MID' | 'SENIOR')}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500"
                  >
                    <option value="JUNIOR">Junior</option>
                    <option value="MID">Mid</option>
                    <option value="SENIOR">Senior</option>
                  </select>
                </Field>
                <Field label="Team Size (3–9)" source={sourceOf('teamSize')}>
                  <NumberInput value={form.teamSize} min={3} max={9} onChange={v => setField('teamSize', v)} />
                </Field>
                <Field label="Sprint Duration (days)" source={sourceOf('sprintDurationDays')}>
                  <NumberInput value={form.sprintDurationDays} min={1} max={60} onChange={v => setField('sprintDurationDays', v)} />
                </Field>
                <Field label="Team Avg Velocity (SP/sprint, 3–35)" source={sourceOf('teamVelocityAvg')}>
                  <NumberInput value={form.teamVelocityAvg} min={3} max={60} onChange={v => setField('teamVelocityAvg', v)} />
                </Field>
                <Field label="Recent Completion Rate (0.5–1.0)" source={sourceOf('recentCompletionRate')}>
                  <NumberInput value={form.recentCompletionRate} step={0.05} min={0.5} max={1.0} float onChange={v => setField('recentCompletionRate', v)} />
                </Field>
                <Field label="Similar Past Tasks Done (0–30)"
                       source={sourceOf('similarTaskCount')}
                       hint={`Auto-counted from ${item.taskType ?? 'this'} items in this project`}>
                  <NumberInput value={form.similarTaskCount} min={0} max={30} onChange={v => setField('similarTaskCount', v)} />
                </Field>
                <Field label="Task Age in Backlog (days, 0–60)"
                       source={sourceOf('taskAgeDays')}
                       hint="Auto-calculated from item.createdAt">
                  <NumberInput value={form.taskAgeDays} min={0} max={365} onChange={v => setField('taskAgeDays', v)} />
                </Field>
              </Grid>
            </Section>

            {error && (
              <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {error}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-5 py-3 border-t border-slate-200 bg-slate-50 flex items-center justify-between gap-3 flex-wrap">
            <div className="flex items-center gap-3">
              <p className="text-xs text-slate-500 flex items-center gap-1.5">
                <Info className="w-3 h-3" /> POST <code className="bg-slate-200 px-1 rounded text-slate-700">/api/v1/backlog/{item.id}/estimate</code>
              </p>
              <button
                onClick={async () => {
                  const body = {
                    domain: form.domain,
                    techStack: form.techStack,
                    taskType: form.taskType,
                    devExperienceLevel: form.devExperienceLevel,
                    teamSize: form.teamSize,
                    sprintDurationDays: form.sprintDurationDays,
                    teamVelocityAvg: form.teamVelocityAvg,
                    recentCompletionRate: form.recentCompletionRate,
                    similarTaskCount: form.similarTaskCount,
                    taskAgeDays: form.taskAgeDays,
                    numComponents: form.numComponents,
                    externalApis: form.externalApis,
                    hasIntegration: form.hasIntegration,
                    hasSecurity: form.hasSecurity,
                    hasUiComplexity: form.hasUiComplexity,
                    userStory: form.userStory,
                  };
                  await navigator.clipboard.writeText(JSON.stringify(body, null, 2));
                  setCopied(true);
                  setTimeout(() => setCopied(false), 1500);
                }}
                className="text-xs text-violet-700 hover:text-violet-900 inline-flex items-center gap-1 font-medium"
              >
                {copied ? <><Check className="w-3 h-3" /> Copied!</> : <><Copy className="w-3 h-3" /> Copy as JSON</>}
              </button>
            </div>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={onClose} disabled={running}>Cancel</Button>
              <Button onClick={submit} disabled={running || !form.domain}>
                {running ? <><Loader2 className="w-4 h-4 animate-spin" /> Predicting…</>
                         : <><Sparkles className="w-4 h-4" /> Predict Story Points</>}
              </Button>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wider mb-2.5">{title}</h4>
      <div className="space-y-3">{children}</div>
    </section>
  );
}

function Grid({ cols, children }: { cols: 2 | 3; children: React.ReactNode }) {
  return (
    <div className={`grid gap-3 ${cols === 3 ? 'grid-cols-1 sm:grid-cols-3' : 'grid-cols-1 sm:grid-cols-2'}`}>
      {children}
    </div>
  );
}

function Field({ label, source, hint, children }: {
  label: string; source: Source; hint?: string; children: React.ReactNode;
}) {
  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between gap-2">
        <label className="text-xs font-medium text-slate-700">{label}</label>
        <SourceBadge source={source} />
      </div>
      {children}
      {hint && <p className="text-xs text-slate-500">{hint}</p>}
    </div>
  );
}

function CheckboxField({ label, source, checked, onChange }: {
  label: string; source: Source; checked: boolean; onChange: (v: boolean) => void;
}) {
  return (
    <label className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50 transition-colors">
      <input
        type="checkbox"
        checked={checked}
        onChange={e => onChange(e.target.checked)}
        className="w-4 h-4 rounded border-slate-300 text-violet-600 focus:ring-violet-500"
      />
      <span className="text-xs text-slate-700 flex-1">{label}</span>
      <SourceBadge source={source} />
    </label>
  );
}

function SourceBadge({ source }: { source: Source }) {
  const map: Record<Source, { label: string; cls: string; icon?: React.ElementType }> = {
    item:     { label: 'item',     cls: 'bg-slate-100 text-slate-600' },
    project:  { label: 'project',  cls: 'bg-blue-50 text-blue-700' },
    sprint:   { label: 'sprint',   cls: 'bg-violet-50 text-violet-700' },
    auto:     { label: 'auto',     cls: 'bg-emerald-50 text-emerald-700', icon: Calculator },
    override: { label: 'edited',   cls: 'bg-amber-50 text-amber-700' },
    default:  { label: 'default',  cls: 'bg-slate-100 text-slate-500' },
  };
  const { label, cls, icon: Icon } = map[source];
  return (
    <span className={`text-[10px] uppercase tracking-wider px-1.5 py-0.5 rounded font-medium inline-flex items-center gap-0.5 ${cls}`}>
      {Icon && <Icon className="w-2.5 h-2.5" />}{label}
    </span>
  );
}

function NumberInput({ value, min, max, step, float, onChange }: {
  value: number; min?: number; max?: number; step?: number; float?: boolean;
  onChange: (v: number) => void;
}) {
  return (
    <input
      type="number"
      value={value}
      min={min}
      max={max}
      step={step ?? (float ? 0.01 : 1)}
      onChange={e => {
        const n = float ? parseFloat(e.target.value) : parseInt(e.target.value, 10);
        if (!Number.isNaN(n)) onChange(n);
      }}
      className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500"
    />
  );
}
