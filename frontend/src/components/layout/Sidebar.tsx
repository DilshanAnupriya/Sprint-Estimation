'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard, FolderKanban, Zap, BarChart3, Users,
  Settings, Brain, Sparkles,
} from 'lucide-react';
import { clsx } from 'clsx';

const navItems = [
  { href: '/',         label: 'Dashboard', icon: LayoutDashboard },
  { href: '/projects', label: 'Projects',  icon: FolderKanban },
  { href: '/sprints',  label: 'Sprints',   icon: Zap },
  { href: '/backlog',  label: 'Backlog',   icon: BarChart3 },
  { href: '/team',     label: 'Team',      icon: Users },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-64 flex-shrink-0 bg-white border-r border-slate-200 flex flex-col">
      {/* Logo */}
      <div className="px-5 py-5 border-b border-slate-200">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center shadow-sm">
            <Brain className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-sm font-semibold text-slate-900 tracking-tight">SprintAI</h1>
            <p className="text-xs text-slate-500">Agile Estimation</p>
          </div>
        </div>
      </div>

      {/* AI Status */}
      <div className="mx-4 mt-4 px-3 py-2 rounded-lg bg-violet-50 border border-violet-100">
        <div className="flex items-center gap-2">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-violet-400 opacity-60" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-violet-500" />
          </span>
          <span className="text-xs text-violet-700 font-medium">AI Models Active</span>
          <Sparkles className="w-3 h-3 text-violet-500 ml-auto" />
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-0.5">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = href === '/' ? pathname === '/' : pathname.startsWith(href);
          return (
            <Link key={href} href={href}
              className={clsx(
                'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-150',
                active
                  ? 'bg-violet-50 text-violet-700'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
              )}
            >
              <Icon className={clsx('w-4 h-4 flex-shrink-0', active ? 'text-violet-600' : 'text-slate-400')} />
              <span className="flex-1">{label}</span>
              {active && <span className="w-1 h-1 rounded-full bg-violet-600" />}
            </Link>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="p-3 border-t border-slate-200">
        <Link href="/settings"
          className={clsx(
            'flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors',
            pathname.startsWith('/settings')
              ? 'bg-violet-50 text-violet-700'
              : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50',
          )}
        >
          <Settings className={clsx('w-4 h-4', pathname.startsWith('/settings') ? 'text-violet-600' : 'text-slate-400')} />
          <span>Settings</span>
        </Link>
        <p className="text-xs text-slate-400 mt-3 px-3">
          v1.0.0
        </p>
      </div>
    </aside>
  );
}
