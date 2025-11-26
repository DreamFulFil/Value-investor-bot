import { useTranslation } from 'react-i18next';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import type { PortfolioHistory } from '../lib/api';

interface PortfolioChartProps {
  data: PortfolioHistory[];
  isEmpty?: boolean;
}

export function PortfolioChart({ data, isEmpty }: PortfolioChartProps) {
  const { t } = useTranslation();

  const formatValue = (value: number) => {
    if (value >= 1000000) {
      return `${(value / 1000000).toFixed(1)}M`;
    }
    if (value >= 1000) {
      return `${(value / 1000).toFixed(0)}K`;
    }
    return value.toString();
  };

  const chartData = data.length > 0 ? data : [
    { date: '2024-01', value: 0 },
    { date: '2024-02', value: 0 },
    { date: '2024-03', value: 0 },
  ];

  return (
    <div className="card relative">
      {isEmpty && (
        <div className="absolute inset-0 bg-gray-900/60 dark:bg-gray-900/80 rounded-xl flex items-center justify-center z-10 backdrop-blur-sm">
          <p className="text-white text-center px-4 text-sm font-medium">
            {t('clickBlueButton')}
          </p>
        </div>
      )}
      
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        {t('portfolioPerformance')}
      </h3>
      
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 5, right: 20, left: 10, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-gray-200 dark:stroke-gray-700" />
            <XAxis
              dataKey="date"
              className="text-xs"
              tick={{ fill: 'currentColor' }}
              tickLine={{ stroke: 'currentColor' }}
            />
            <YAxis
              tickFormatter={formatValue}
              className="text-xs"
              tick={{ fill: 'currentColor' }}
              tickLine={{ stroke: 'currentColor' }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'var(--tooltip-bg, #fff)',
                border: 'none',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
              }}
              formatter={(value: number) => [`NT$${value.toLocaleString()}`, 'Value']}
            />
            <Line
              type="monotone"
              dataKey="value"
              stroke="#3b82f6"
              strokeWidth={3}
              dot={{ fill: '#3b82f6', strokeWidth: 2 }}
              activeDot={{ r: 6, fill: '#3b82f6' }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
