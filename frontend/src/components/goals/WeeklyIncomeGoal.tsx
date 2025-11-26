import { usePortfolio } from '@/hooks/usePortfolio';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useTranslation } from 'react-i18next';
import { DollarSign, TrendingUp } from 'lucide-react';
import { RadialBarChart, RadialBar, ResponsiveContainer, PolarAngleAxis } from 'recharts';

// Format as NT$ currency
const formatNTD = (value: number) => `NT$${value.toLocaleString()}`;

export const WeeklyIncomeGoal = () => {
  const { t } = useTranslation();
  const { portfolio } = usePortfolio();

  const weeklyGoal = 1600; // NT$1,600 per week target
  const currentIncome = portfolio?.weeklyDividendIncome || 0;
  const progress = Math.min((currentIncome / weeklyGoal) * 100, 100);
  const remaining = Math.max(weeklyGoal - currentIncome, 0);

  // Data for circular progress ring
  const chartData = [
    {
      name: 'Progress',
      value: progress,
      fill: progress >= 100 ? '#10b981' : '#3b82f6',
    },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <DollarSign className="h-6 w-6 text-green-600" />
          {t('goals.weeklyIncome')}
        </CardTitle>
        <CardDescription>
          {t('goals.target')}: {formatNTD(weeklyGoal)}/week
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Circular Progress Ring */}
        <div className="relative flex justify-center items-center">
          <div className="w-48 h-48">
            <ResponsiveContainer width="100%" height="100%">
              <RadialBarChart
                cx="50%"
                cy="50%"
                innerRadius="70%"
                outerRadius="100%"
                barSize={12}
                data={chartData}
                startAngle={90}
                endAngle={-270}
              >
                <PolarAngleAxis
                  type="number"
                  domain={[0, 100]}
                  angleAxisId={0}
                  tick={false}
                />
                <RadialBar
                  background={{ fill: '#e5e7eb' }}
                  dataKey="value"
                  cornerRadius={10}
                  fill="#3b82f6"
                />
              </RadialBarChart>
            </ResponsiveContainer>
          </div>
          {/* Center text */}
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <div className="text-2xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent">
              {formatNTD(currentIncome)}
            </div>
            <p className="text-xs text-muted-foreground">/week</p>
            <p className="text-lg font-semibold mt-1">{progress.toFixed(0)}%</p>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded-lg border p-4 bg-muted/50">
            <div className="flex items-center gap-2 mb-2">
              <TrendingUp className="h-4 w-4 text-green-600" />
              <p className="text-xs font-medium text-muted-foreground">Current</p>
            </div>
            <p className="text-xl font-bold">{formatNTD(currentIncome)}</p>
          </div>

          <div className="rounded-lg border p-4 bg-muted/50">
            <div className="flex items-center gap-2 mb-2">
              <DollarSign className="h-4 w-4 text-blue-600" />
              <p className="text-xs font-medium text-muted-foreground">Remaining</p>
            </div>
            <p className="text-xl font-bold">{formatNTD(remaining)}</p>
          </div>
        </div>

        {progress >= 100 ? (
          <div className="rounded-lg bg-gradient-to-r from-green-500/10 to-emerald-500/10 border border-green-500/20 p-4 text-center">
            <p className="text-lg font-semibold text-green-600 dark:text-green-400">
              🎉 Goal Achieved!
            </p>
            <p className="text-sm text-muted-foreground mt-1">
              You're earning {formatNTD(currentIncome)} per week
            </p>
          </div>
        ) : (
          <div className="rounded-lg bg-blue-50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-800 p-4">
            <p className="text-sm text-muted-foreground">
              At a 4% dividend yield, you need approximately{' '}
              <span className="font-semibold text-foreground">
                {formatNTD(Math.round((weeklyGoal * 52) / 0.04))}
              </span>{' '}
              in dividend stocks to reach your weekly goal.
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
