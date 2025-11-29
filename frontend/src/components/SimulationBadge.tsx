import { useTranslation } from 'react-i18next';

interface SimulationBadgeProps {
  mode: 'SIMULATION' | 'LIVE';
  goLiveDate?: string | null;
}

export function SimulationBadge({ mode, goLiveDate }: SimulationBadgeProps) {
  const { t, i18n } = useTranslation();
  const isZhTW = i18n.language === 'zh';

  if (mode === 'LIVE') {
    return (
      <div className="flex flex-col items-center gap-1">
        <span className="inline-flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-bold bg-green-500 text-white shadow-lg animate-pulse">
          <span className="w-2.5 h-2.5 bg-white rounded-full" />
          {isZhTW ? '🔴 真實交易模式' : '🔴 LIVE MODE – Real Money'}
        </span>
        {goLiveDate && (
          <span className="text-xs text-green-600 dark:text-green-400">
            {isZhTW ? `自 ${goLiveDate} 起生效` : `Active since ${goLiveDate}`}
          </span>
        )}
      </div>
    );
  }

  return (
    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-blue-500/20 text-blue-400 border border-blue-500/30">
      <span className="w-2 h-2 bg-blue-500 rounded-full" />
      {t('simulationMode')}
    </span>
  );
}
