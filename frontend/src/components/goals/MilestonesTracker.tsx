import { usePortfolio } from '@/hooks/usePortfolio';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { formatCurrency } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';
import { Trophy, CheckCircle2, Circle } from 'lucide-react';

export const MilestonesTracker = () => {
  const { t } = useTranslation();
  const { portfolio } = usePortfolio();

  const totalValue = portfolio?.totalValue || 0;

  const milestones = [
    { value: 1000, label: t('goals.milestone.first1k'), icon: '🎯' },
    { value: 5000, label: t('goals.milestone.first5k'), icon: '🚀' },
    { value: 10000, label: t('goals.milestone.first10k'), icon: '💎' },
    { value: 50000, label: t('goals.milestone.first50k'), icon: '🏆' },
    { value: 100000, label: t('goals.milestone.first100k'), icon: '👑' },
  ];

  const getCurrentMilestone = () => {
    for (let i = 0; i < milestones.length; i++) {
      if (totalValue < milestones[i].value) {
        return i;
      }
    }
    return milestones.length;
  };

  const currentMilestoneIndex = getCurrentMilestone();
  const nextMilestone = milestones[currentMilestoneIndex];
  const progress = nextMilestone
    ? Math.min((totalValue / nextMilestone.value) * 100, 100)
    : 100;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Trophy className="h-5 w-5 text-yellow-600" />
          {t('goals.milestones')}
        </CardTitle>
        <CardDescription>Track your investment journey</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {nextMilestone && (
          <div className="space-y-4">
            <div className="text-center">
              <div className="text-4xl mb-2">{nextMilestone.icon}</div>
              <p className="font-semibold">{nextMilestone.label}</p>
              <p className="text-sm text-muted-foreground">
                {formatCurrency(totalValue)} / {formatCurrency(nextMilestone.value)}
              </p>
            </div>

            <div className="space-y-2">
              <Progress value={progress} className="h-3" />
              <div className="flex justify-between text-xs text-muted-foreground">
                <span>{progress.toFixed(0)}% complete</span>
                <span>{formatCurrency(nextMilestone.value - totalValue)} to go</span>
              </div>
            </div>
          </div>
        )}

        <div className="space-y-3">
          {milestones.map((milestone, index) => {
            const isAchieved = totalValue >= milestone.value;
            const isCurrent = index === currentMilestoneIndex;

            return (
              <div
                key={milestone.value}
                className={`flex items-center gap-3 p-3 rounded-lg border ${
                  isAchieved
                    ? 'bg-green-50 dark:bg-green-900/10 border-green-200 dark:border-green-800'
                    : isCurrent
                    ? 'bg-blue-50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800'
                    : 'bg-muted/50'
                }`}
              >
                <div>
                  {isAchieved ? (
                    <CheckCircle2 className="h-5 w-5 text-green-600" />
                  ) : (
                    <Circle className={`h-5 w-5 ${isCurrent ? 'text-blue-600' : 'text-muted-foreground'}`} />
                  )}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-lg">{milestone.icon}</span>
                    <p className="font-medium text-sm">{milestone.label}</p>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {formatCurrency(milestone.value)}
                  </p>
                </div>
                {isAchieved && (
                  <div className="text-xs font-medium text-green-600 dark:text-green-400">
                    Achieved
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {currentMilestoneIndex >= milestones.length && (
          <div className="rounded-lg bg-gradient-to-r from-yellow-500/10 to-orange-500/10 border border-yellow-500/20 p-4 text-center">
            <p className="text-lg font-semibold text-yellow-600 dark:text-yellow-400">
              All Milestones Achieved!
            </p>
            <p className="text-sm text-muted-foreground mt-1">
              Congratulations on your investing journey!
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
