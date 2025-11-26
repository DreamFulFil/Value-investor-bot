import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { usePortfolio } from '@/hooks/usePortfolio';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { formatCurrency } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';

const COLORS = [
  'hsl(var(--chart-1))',
  'hsl(var(--chart-2))',
  'hsl(var(--chart-3))',
  'hsl(var(--chart-4))',
  'hsl(var(--chart-5))',
];

export const AllocationPieChart = () => {
  const { t } = useTranslation();
  const { portfolio, loading } = usePortfolio();

  if (loading || !portfolio) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.allocation')}</CardTitle>
          <CardDescription>By stock</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] flex items-center justify-center">
            <p className="text-muted-foreground">{t('common.loading')}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const chartData = portfolio.positions.map((position) => ({
    name: position.symbol,
    value: position.totalValue,
  }));

  // Add cash if any
  if (portfolio.cash > 0) {
    chartData.push({
      name: 'Cash',
      value: portfolio.cash,
    });
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('dashboard.allocation')}</CardTitle>
        <CardDescription>By stock</CardDescription>
      </CardHeader>
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
            >
              {chartData.map((_entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value: number) => formatCurrency(value)} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};
