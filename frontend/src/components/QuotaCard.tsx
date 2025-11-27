import { useTranslation } from 'react-i18next';
import type { QuotaStatus } from '../lib/api';

interface QuotaCardProps {
  quota: QuotaStatus | undefined;
  isLoading: boolean;
}

export function QuotaCard({ quota, isLoading }: QuotaCardProps) {
  const { t } = useTranslation();

  if (isLoading || !quota) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700">
        <div className="flex items-center justify-between mb-3">
          <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
            {t('apiQuota')}
          </span>
          <div className="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
            <svg className="w-5 h-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
        </div>
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-24 mb-2"></div>
          <div className="h-2 bg-gray-200 dark:bg-gray-700 rounded w-full"></div>
        </div>
      </div>
    );
  }

  const { usedMB, limitMB, remainingMB, percentageUsed, fallbackActive } = quota;

  // Color based on usage
  let barColor = 'bg-green-500';
  let textColor = 'text-green-600 dark:text-green-400';
  if (percentageUsed > 90) {
    barColor = 'bg-red-500';
    textColor = 'text-red-600 dark:text-red-400';
  } else if (percentageUsed > 50) {
    barColor = 'bg-yellow-500';
    textColor = 'text-yellow-600 dark:text-yellow-400';
  }

  return (
    <div className="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700">
      <div className="flex items-center justify-between mb-3">
        <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
          {t('apiQuota')}
        </span>
        <div className="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
          <svg className="w-5 h-5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
      </div>

      <div className="mb-2">
        <span className={`text-lg font-bold ${textColor}`}>
          {usedMB.toFixed(0)} / {limitMB.toFixed(0)} MB
        </span>
      </div>

      {/* Progress bar */}
      <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden mb-2">
        <div
          className={`h-full ${barColor} transition-all duration-500`}
          style={{ width: `${Math.min(percentageUsed, 100)}%` }}
        />
      </div>

      <div className="flex items-center justify-between text-xs">
        <span className="text-gray-500 dark:text-gray-400">
          {remainingMB.toFixed(0)} MB {t('remaining')}
        </span>
        {fallbackActive && (
          <span className="px-2 py-0.5 bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400 rounded-full text-xs font-medium">
            yfinance {t('active')}
          </span>
        )}
      </div>
    </div>
  );
}
