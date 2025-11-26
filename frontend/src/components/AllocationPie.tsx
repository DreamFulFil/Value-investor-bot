import { useTranslation } from 'react-i18next';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import type { Position } from '../lib/api';

interface AllocationPieProps {
  positions: Position[];
  isEmpty?: boolean;
}

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'];

export function AllocationPie({ positions, isEmpty }: AllocationPieProps) {
  const { t } = useTranslation();

  const data = positions.length > 0
    ? positions.slice(0, 8).map((p) => ({
        name: p.symbol,
        value: p.marketValue,
        weight: p.weight,
      }))
    : [
        { name: 'Cash', value: 100, weight: 100 },
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
        {t('assetAllocation')}
      </h3>
      
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={50}
              outerRadius={80}
              paddingAngle={2}
              dataKey="value"
            >
              {data.map((_, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip
              formatter={(value: number, name: string) => [
                `NT$${value.toLocaleString()}`,
                name,
              ]}
              contentStyle={{
                backgroundColor: 'var(--tooltip-bg, #fff)',
                border: 'none',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
              }}
            />
            <Legend
              formatter={(value: string) => (
                <span className="text-sm text-gray-700 dark:text-gray-300">{value}</span>
              )}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
