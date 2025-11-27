import { useTranslation } from 'react-i18next';
import type { Position } from '../lib/api';

interface HoldingsTableProps {
  positions: Position[];
  isEmpty?: boolean;
}

export function HoldingsTable({ positions, isEmpty }: HoldingsTableProps) {
  const { t } = useTranslation();

  const topPositions = positions.slice(0, 5);

  return (
    <div className="card relative overflow-hidden">
      {isEmpty && (
        <div className="absolute inset-0 bg-gray-900/60 dark:bg-gray-900/80 rounded-xl flex items-center justify-center z-10 backdrop-blur-sm">
          <p className="text-white text-center px-4 text-sm font-medium">
            {t('clickBlueButton')}
          </p>
        </div>
      )}
      
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
        {t('topHoldings')}
      </h3>
      
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200 dark:border-gray-700">
              <th className="text-left py-3 px-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                {t('symbol')}
              </th>
              <th className="text-left py-3 px-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                {t('name')}
              </th>
              <th className="text-right py-3 px-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                {t('shares')}
              </th>
              <th className="text-right py-3 px-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                {t('value')}
              </th>
              <th className="text-right py-3 px-2 text-sm font-medium text-gray-500 dark:text-gray-400">
                {t('weight')}
              </th>
            </tr>
          </thead>
          <tbody>
            {topPositions.length > 0 ? (
              topPositions.map((position) => (
                <tr
                  key={position.symbol}
                  className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                >
                  <td className="py-3 px-2 font-medium text-gray-900 dark:text-white">
                    {position.symbol}
                  </td>
                  <td className="py-3 px-2 text-gray-600 dark:text-gray-300">
                    {position.name}
                  </td>
                  <td className="py-3 px-2 text-right text-gray-900 dark:text-white">
                    {(position.shares ?? 0).toLocaleString()}
                  </td>
                  <td className="py-3 px-2 text-right text-gray-900 dark:text-white">
                    NT${(position.marketValue ?? 0).toLocaleString()}
                  </td>
                  <td className="py-3 px-2 text-right">
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300">
                      {(position.weight ?? 0).toFixed(1)}%
                    </span>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={5} className="py-8 text-center text-gray-500 dark:text-gray-400">
                  {t('noDataYet')}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
