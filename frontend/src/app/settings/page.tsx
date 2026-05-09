import { PageHeader, Card, CardBody } from '@/components/ui';
import { Settings as SettingsIcon } from 'lucide-react';

export default function SettingsPage() {
  return (
    <div className="p-8 max-w-3xl mx-auto">
      <PageHeader title="Settings" subtitle="Workspace and preferences" />
      <Card>
        <CardBody>
          <div className="flex items-center gap-3 py-6">
            <div className="w-10 h-10 rounded-lg bg-slate-100 flex items-center justify-center">
              <SettingsIcon className="w-5 h-5 text-slate-400" />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-700">Configuration coming soon</p>
              <p className="text-xs text-slate-500 mt-0.5">Workspace, appearance, and notification preferences will live here.</p>
            </div>
          </div>
        </CardBody>
      </Card>
    </div>
  );
}
