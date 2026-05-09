'use client';
import { clsx } from 'clsx';

// ─── Badge ────────────────────────────────────────────────────────────────────
const badgeVariants = {
  default: 'bg-slate-100 text-slate-700 border border-slate-200',
  violet:  'bg-violet-50 text-violet-700 border border-violet-200',
  emerald: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  amber:   'bg-amber-50 text-amber-700 border border-amber-200',
  red:     'bg-red-50 text-red-700 border border-red-200',
  blue:    'bg-blue-50 text-blue-700 border border-blue-200',
};

export function Badge({ children, variant = 'default', className }: {
  children: React.ReactNode;
  variant?: keyof typeof badgeVariants;
  className?: string;
}) {
  return (
    <span className={clsx(
      'inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium',
      badgeVariants[variant],
      className,
    )}>
      {children}
    </span>
  );
}

// ─── Card ─────────────────────────────────────────────────────────────────────
export function Card({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={clsx(
      'bg-white border border-slate-200 rounded-xl shadow-sm',
      className,
    )}>
      {children}
    </div>
  );
}

export function CardHeader({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={clsx('px-5 py-3.5 border-b border-slate-200', className)}>{children}</div>;
}

export function CardBody({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={clsx('px-5 py-4', className)}>{children}</div>;
}

// ─── Button ───────────────────────────────────────────────────────────────────
const btnVariants = {
  primary:   'bg-violet-600 hover:bg-violet-700 text-white shadow-sm',
  secondary: 'bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 shadow-sm',
  danger:    'bg-red-600 hover:bg-red-700 text-white shadow-sm',
  ghost:     'hover:bg-slate-100 text-slate-600 hover:text-slate-900',
  success:   'bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm',
};

export function Button({
  children, variant = 'primary', size = 'md',
  className, onClick, disabled, type = 'button',
}: {
  children: React.ReactNode;
  variant?: keyof typeof btnVariants;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  onClick?: () => void;
  disabled?: boolean;
  type?: 'button' | 'submit' | 'reset';
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={clsx(
        'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors duration-150 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:ring-offset-2 focus:ring-offset-slate-50',
        size === 'sm' && 'px-3 py-1.5 text-xs',
        size === 'md' && 'px-4 py-2 text-sm',
        size === 'lg' && 'px-5 py-2.5 text-base',
        btnVariants[variant],
        className,
      )}
    >
      {children}
    </button>
  );
}

// ─── Input ────────────────────────────────────────────────────────────────────
export function Input({ label, error, className, ...props }: {
  label?: string;
  error?: string;
} & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div className="space-y-1">
      {label && <label className="block text-xs font-medium text-slate-600">{label}</label>}
      <input
        {...props}
        className={clsx(
          'w-full px-3 py-2 bg-white border rounded-lg text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500 transition-colors',
          error ? 'border-red-300' : 'border-slate-200',
          className,
        )}
      />
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  );
}

// ─── Select ───────────────────────────────────────────────────────────────────
export function Select({ label, className, children, ...props }: {
  label?: string;
} & React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className="space-y-1">
      {label && <label className="block text-xs font-medium text-slate-600">{label}</label>}
      <select
        {...props}
        className={clsx(
          'w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500 transition-colors',
          className,
        )}
      >
        {children}
      </select>
    </div>
  );
}

// ─── Textarea ─────────────────────────────────────────────────────────────────
export function Textarea({ label, className, ...props }: {
  label?: string;
} & React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <div className="space-y-1">
      {label && <label className="block text-xs font-medium text-slate-600">{label}</label>}
      <textarea
        {...props}
        className={clsx(
          'w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-violet-500/30 focus:border-violet-500 transition-colors resize-none',
          className,
        )}
      />
    </div>
  );
}

// ─── Stat Card ────────────────────────────────────────────────────────────────
export function StatCard({ label, value, sub, unit, icon: Icon, color = 'violet' }: {
  label: string;
  value: string | number;
  sub?: string;
  unit?: string;
  icon?: React.ElementType;
  color?: 'violet' | 'emerald' | 'amber' | 'blue' | 'red';
}) {
  const iconBg = {
    violet:  'bg-violet-50 text-violet-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber:   'bg-amber-50 text-amber-600',
    blue:    'bg-blue-50 text-blue-600',
    red:     'bg-red-50 text-red-600',
  };
  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 card-hover">
      <div className="flex items-start justify-between">
        <div className="min-w-0">
          <p className="text-xs text-slate-500 font-medium uppercase tracking-wide">{label}</p>
          <p className="text-2xl font-semibold text-slate-900 mt-1.5 leading-none">
            {value}{unit && <span className="text-base font-normal text-slate-500 ml-1">{unit}</span>}
          </p>
          {sub && <p className="text-xs text-slate-500 mt-1.5">{sub}</p>}
        </div>
        {Icon && (
          <div className={clsx('w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0', iconBg[color])}>
            <Icon className="w-4 h-4" />
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Page Header ──────────────────────────────────────────────────────────────
export function PageHeader({ title, subtitle, actions }: {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}) {
  return (
    <div className="flex items-start justify-between mb-6 gap-4">
      <div className="min-w-0">
        <h1 className="text-2xl font-semibold text-slate-900 tracking-tight">{title}</h1>
        {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
      </div>
      {actions && <div className="flex items-center gap-2 flex-shrink-0">{actions}</div>}
    </div>
  );
}

// ─── Empty State ──────────────────────────────────────────────────────────────
export function EmptyState({ icon: Icon, title, description, action }: {
  icon: React.ElementType;
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
      <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center mb-4">
        <Icon className="w-6 h-6 text-slate-400" />
      </div>
      <p className="text-sm font-semibold text-slate-700">{title}</p>
      {description && <p className="text-xs text-slate-500 mt-1.5 max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

// ─── Spinner ──────────────────────────────────────────────────────────────────
export function Spinner({ className }: { className?: string }) {
  return (
    <div className={clsx('flex items-center justify-center py-12', className)}>
      <div className="w-7 h-7 border-2 border-violet-600 border-t-transparent rounded-full animate-spin" />
    </div>
  );
}
