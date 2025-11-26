import { usePortfolio } from '@/hooks/usePortfolio';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { formatCurrency } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';
import { DollarSign } from 'lucide-react';

export const DividendProgress = () => {
  const { t } = useTranslation();
  const { portfolio, loading } = usePortfolio();

  const weeklyGoal = 1600; // NT$1,600/week goal
  const currentIncome = portfolio?.weeklyDividendIncome || 0;
  const progress = Math.min((currentIncome / weeklyGoal) * 100, 100);

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.dividendProgress')}</CardTitle>
          <CardDescription>{t('dashboard.weeklyGoal')}: {formatCurrency(weeklyGoal)}</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center h-24">
            <p className="text-muted-foreground">{t('common.loading')}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <DollarSign className="h-5 w-5 text-green-600" />
          {t('dashboard.dividendProgress')}
        </CardTitle>
        <CardDescription>{t('dashboard.weeklyGoal')}: {formatCurrency(weeklyGoal)}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">{t('dashboard.currentIncome')}</span>
            <span className="font-semibold">{formatCurrency(currentIncome)}/week</span>
          </div>
          <Progress value={progress} className="h-3" />
          <div className="flex justify-between text-xs text-muted-foreground">
            <span>{formatCurrency(0)}</span>
            <span>{formatCurrency(weeklyGoal)}</span>
          </div>
        </div>

        <div className="rounded-lg bg-gradient-to-r from-green-500/10 to-emerald-500/10 border border-green-500/20 p-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-green-600 dark:text-green-400 mb-1">
              {progress.toFixed(0)}%
            </div>
            <p className="text-xs text-muted-foreground">
              {currentIncome >= weeklyGoal
                ? 'Goal achieved!'
                : `${formatCurrency(weeklyGoal - currentIncome)} to go`}
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
