import { useTranslation } from 'react-i18next';
import { WeeklyIncomeGoal } from './WeeklyIncomeGoal';
import { RebalanceCountdown } from './RebalanceCountdown';
import { MilestonesTracker } from './MilestonesTracker';

export const GoalsTab = () => {
  const { t } = useTranslation();

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">{t('goals.title')}</h2>
        <p className="text-muted-foreground">{t('goals.subtitle')}</p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <WeeklyIncomeGoal />
        <RebalanceCountdown />
      </div>

      <MilestonesTracker />
    </div>
  );
};
