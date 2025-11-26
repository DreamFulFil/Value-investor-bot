import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatCurrency, formatPercent, formatSharpeRatio, formatDrawdown } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';
import { TrendingUp, Activity, TrendingDown } from 'lucide-react';

interface BacktestResultsProps {
  results: BacktestResult | null;
}

export interface BacktestResult {
  portfolioValue: { date: string; value: number }[];
  benchmarkValue: { date: string; value: number }[];
  totalReturn: number;
  sharpeRatio: number;
  maxDrawdown: number;
  benchmarkReturn: number;
}

export const BacktestResults = ({ results }: BacktestResultsProps) => {
  const { t } = useTranslation();

  if (!results) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('backtest.results')}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground text-center py-8">
            Run a backtest to see results
          </p>
        </CardContent>
      </Card>
    );
  }

  // Merge portfolio and benchmark data
  const chartData = results.portfolioValue.map((point, index) => ({
    date: new Date(point.date).toLocaleDateString('en-US', { month: 'short', year: '2-digit' }),
    portfolio: point.value,
    benchmark: results.benchmarkValue[index]?.value || 0,
  }));

  return (
    <div className="space-y-6">
      {/* Metrics Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-2 mb-2">
              <div className="p-2 bg-green-100 dark:bg-green-900/20 rounded-lg">
                <TrendingUp className="h-4 w-4 text-green-600 dark:text-green-400" />
              </div>
              <p className="text-sm font-medium text-muted-foreground">
                {t('backtest.totalReturn')}
              </p>
            </div>
            <div className="text-2xl font-bold">{formatPercent(results.totalReturn)}</div>
            <p className="text-xs text-muted-foreground mt-1">
              Benchmark: {formatPercent(results.benchmarkReturn)}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-2 mb-2">
              <div className="p-2 bg-blue-100 dark:bg-blue-900/20 rounded-lg">
                <Activity className="h-4 w-4 text-blue-600 dark:text-blue-400" />
              </div>
              <p className="text-sm font-medium text-muted-foreground">
                {t('backtest.sharpeRatio')}
              </p>
            </div>
            <div className="text-2xl font-bold">{formatSharpeRatio(results.sharpeRatio)}</div>
            <p className="text-xs text-muted-foreground mt-1">
              Risk-adjusted returns
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-2 mb-2">
              <div className="p-2 bg-red-100 dark:bg-red-900/20 rounded-lg">
                <TrendingDown className="h-4 w-4 text-red-600 dark:text-red-400" />
              </div>
              <p className="text-sm font-medium text-muted-foreground">
                {t('backtest.maxDrawdown')}
              </p>
            </div>
            <div className="text-2xl font-bold">{formatDrawdown(results.maxDrawdown)}</div>
            <p className="text-xs text-muted-foreground mt-1">
              Maximum decline from peak
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Performance Chart */}
      <Card>
        <CardHeader>
          <CardTitle>Performance Comparison</CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
              <XAxis
                dataKey="date"
                className="text-xs"
                tick={{ fill: 'hsl(var(--muted-foreground))' }}
              />
              <YAxis
                className="text-xs"
                tick={{ fill: 'hsl(var(--muted-foreground))' }}
                tickFormatter={(value) => `NT$${(value / 1000).toFixed(0)}k`}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'hsl(var(--background))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '6px',
                }}
                formatter={(value: number) => formatCurrency(value)}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="portfolio"
                stroke="hsl(var(--primary))"
                strokeWidth={2}
                name={t('backtest.portfolio')}
                dot={false}
              />
              <Line
                type="monotone"
                dataKey="benchmark"
                stroke="hsl(var(--muted-foreground))"
                strokeWidth={2}
                strokeDasharray="5 5"
                name={t('backtest.benchmark')}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  );
};
