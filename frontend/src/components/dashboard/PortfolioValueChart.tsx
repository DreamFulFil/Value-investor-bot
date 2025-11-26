import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { usePortfolioHistory } from '@/hooks/usePortfolio';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { formatCurrency, formatShortDate } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';

export const PortfolioValueChart = () => {
  const { t } = useTranslation();
  const { history, loading } = usePortfolioHistory(6);

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.portfolioValue')}</CardTitle>
          <CardDescription>Last 6 months</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] flex items-center justify-center">
            <p className="text-muted-foreground">{t('common.loading')}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const chartData = history.map((snapshot) => ({
    date: formatShortDate(snapshot.date),
    value: snapshot.totalValue,
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('dashboard.portfolioValue')}</CardTitle>
        <CardDescription>Last 6 months</CardDescription>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
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
              tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'hsl(var(--background))',
                border: '1px solid hsl(var(--border))',
                borderRadius: '6px',
              }}
              formatter={(value: number) => [formatCurrency(value), 'Value']}
            />
            <Line
              type="monotone"
              dataKey="value"
              stroke="hsl(var(--primary))"
              strokeWidth={2}
              dot={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};
