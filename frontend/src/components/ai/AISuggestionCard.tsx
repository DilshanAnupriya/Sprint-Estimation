'use client';
import { motion, AnimatePresence } from 'framer-motion';
import { Sparkles, Check, X, AlertTriangle, Loader2 } from 'lucide-react';
import { clsx } from 'clsx';
import type { AISuggestionStatus } from '@/lib/types';

interface AISuggestionCardProps {
  title: string;
  value: number | string;
  unit?: string;
  lowerBound?: number;
  upperBound?: number;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
  confidence?: number;
  meta?: { label: string; value: string | number }[];
  warnings?: string[];
  status: AISuggestionStatus;
  onAccept: () => void;
  onReject: () => void;
  onRequest?: () => void;
}

const riskColors = {
  LOW:    'text-emerald-700 bg-emerald-50 border border-emerald-200',
  MEDIUM: 'text-amber-700   bg-amber-50   border border-amber-200',
  HIGH:   'text-red-700     bg-red-50     border border-red-200',
};
const riskLabels = { LOW: 'Low Risk', MEDIUM: 'Medium Risk', HIGH: 'High Risk' };

export function AISuggestionCard({
  title, value, unit, lowerBound, upperBound, riskLevel,
  meta, warnings, status, onAccept, onReject, onRequest,
}: AISuggestionCardProps) {

  if (status === 'idle' && onRequest) {
    return (
      <motion.button
        onClick={onRequest}
        whileHover={{ scale: 1.01 }}
        whileTap={{ scale: 0.99 }}
        className="w-full flex items-center gap-3 px-4 py-3 rounded-xl border border-dashed border-violet-300 bg-violet-50/50 text-violet-700 hover:border-violet-400 hover:bg-violet-50 transition-colors duration-150 text-sm font-medium"
      >
        <Sparkles className="w-4 h-4" />
        <span>Ask AI to {title}</span>
      </motion.button>
    );
  }

  if (status === 'loading') {
    return (
      <div className="rounded-xl border border-violet-200 bg-violet-50/50 p-4">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-violet-100 flex items-center justify-center">
            <Loader2 className="w-5 h-5 text-violet-600 animate-spin" />
          </div>
          <div>
            <p className="text-sm font-medium text-violet-700">AI is thinking…</p>
            <p className="text-xs text-slate-500 mt-0.5">Running ML inference</p>
          </div>
        </div>
        <div className="mt-3 space-y-2">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-1.5 rounded-full bg-violet-200 ai-pulse" style={{ width: `${70 + i * 10}%` }} />
          ))}
        </div>
      </div>
    );
  }

  if (status === 'accepted') {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 flex items-center gap-3"
      >
        <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center">
          <Check className="w-4 h-4 text-emerald-700" />
        </div>
        <div>
          <p className="text-sm font-semibold text-emerald-800">AI suggestion accepted</p>
          <p className="text-xs text-emerald-700/80">
            {value} {unit} applied
          </p>
        </div>
      </motion.div>
    );
  }

  if (status === 'rejected') {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 flex items-center gap-3"
      >
        <div className="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center">
          <X className="w-4 h-4 text-slate-500" />
        </div>
        <div>
          <p className="text-sm font-medium text-slate-700">AI suggestion rejected</p>
          <p className="text-xs text-slate-500">Enter your own value manually</p>
        </div>
      </motion.div>
    );
  }

  // status === 'ready'
  return (
    <AnimatePresence>
      <motion.div
        key="suggestion"
        initial={{ opacity: 0, y: 10, scale: 0.97 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: -10 }}
        transition={{ duration: 0.2 }}
        className="rounded-xl border border-violet-200 bg-white ai-glow overflow-hidden"
      >
        {/* Header */}
        <div className="px-4 py-2.5 bg-gradient-to-r from-violet-50 to-indigo-50 border-b border-violet-100 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-violet-600" />
          <span className="text-xs font-semibold text-violet-700 uppercase tracking-wider">AI Suggestion</span>
          <span className="ml-1 text-xs text-slate-500">— {title}</span>
        </div>

        {/* Body */}
        <div className="p-4">
          {/* Main value */}
          <div className="flex items-end gap-3 mb-3 flex-wrap">
            <span className="text-4xl font-semibold gradient-text leading-none">{value}</span>
            {unit && <span className="text-base text-slate-500 mb-1">{unit}</span>}
            {lowerBound !== undefined && upperBound !== undefined && (
              <span className="text-sm text-slate-500 mb-1">
                [{lowerBound}–{upperBound}]
              </span>
            )}
            {riskLevel && (
              <span className={clsx('ml-auto text-xs px-2 py-1 rounded-full font-medium', riskColors[riskLevel])}>
                {riskLabels[riskLevel]}
              </span>
            )}
          </div>

          {/* Meta info */}
          {meta && meta.length > 0 && (
            <div className="grid grid-cols-2 gap-2 mb-3">
              {meta.map(m => (
                <div key={m.label} className="bg-slate-50 border border-slate-200 rounded-lg px-3 py-2">
                  <p className="text-xs text-slate-500">{m.label}</p>
                  <p className="text-sm font-semibold text-slate-900 mt-0.5">{m.value}</p>
                </div>
              ))}
            </div>
          )}

          {/* Warnings */}
          {warnings && warnings.length > 0 && (
            <div className="mb-3 space-y-1.5">
              {warnings.map((w, i) => (
                <div key={i} className="flex items-start gap-2 text-xs text-amber-800 bg-amber-50 rounded-lg px-3 py-2 border border-amber-200">
                  <AlertTriangle className="w-3 h-3 mt-0.5 flex-shrink-0" />
                  <span>{w}</span>
                </div>
              ))}
            </div>
          )}

          {/* Accept / Reject buttons */}
          <div className="flex gap-2 mt-3">
            <motion.button
              onClick={onAccept}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-semibold transition-colors"
            >
              <Check className="w-4 h-4" />
              Accept
            </motion.button>
            <motion.button
              onClick={onReject}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 text-sm font-semibold transition-colors"
            >
              <X className="w-4 h-4" />
              Reject
            </motion.button>
          </div>
        </div>
      </motion.div>
    </AnimatePresence>
  );
}
