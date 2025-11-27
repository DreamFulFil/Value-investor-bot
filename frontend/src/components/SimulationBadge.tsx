import { useTranslation } from 'react-i18next';

interface SimulationBadgeProps {
  mode: 'SIMULATION' | 'LIVE';
}

export function SimulationBadge({ mode }: SimulationBadgeProps) {
  const { t } = useTranslation();

  if (mode === 'LIVE') {
    return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-400 border border-red-500/30">
        <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
        {t('liveTrading')}
      </span>
    );
  }

  return (
    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-green-500/20 text-green-400 border border-green-500/30">
      <span className="w-2 h-2 bg-green-500 rounded-full" />
      {t('simulationMode')}
    </span>
  );
}
