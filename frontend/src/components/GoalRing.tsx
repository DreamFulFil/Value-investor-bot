import { useTranslation } from 'react-i18next';

interface GoalRingProps {
  current: number;
  target: number;
  isEmpty?: boolean;
}

export function GoalRing({ current, target, isEmpty }: GoalRingProps) {
  const { t } = useTranslation();
  const safeTarget = target || 1;
  const safeCurrent = current ?? 0;
  const percentage = Math.min((safeCurrent / safeTarget) * 100, 100);
  const circumference = 2 * Math.PI * 45;
  const strokeDashoffset = circumference - (percentage / 100) * circumference;

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
        {t('weeklyDividendGoal')}
      </h3>
      
      <div className="flex flex-col items-center">
        <div className="relative w-32 h-32">
          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
            {/* Background circle */}
            <circle
              cx="50"
              cy="50"
              r="45"
              fill="none"
              stroke="currentColor"
              strokeWidth="8"
              className="text-gray-200 dark:text-gray-700"
            />
            {/* Progress circle */}
            <circle
              cx="50"
              cy="50"
              r="45"
              fill="none"
              stroke="currentColor"
              strokeWidth="8"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              className="text-green-500 transition-all duration-1000 ease-out"
            />
          </svg>
          
          {/* Center text */}
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-2xl font-bold text-gray-900 dark:text-white">
              {percentage.toFixed(0)}%
            </span>
            <span className="text-xs text-gray-500 dark:text-gray-400">
              {t('ofGoal')}
            </span>
          </div>
        </div>
        
        <div className="mt-4 text-center">
          <p className="text-lg font-semibold text-gray-900 dark:text-white">
            NT${safeCurrent.toLocaleString()} / NT${safeTarget.toLocaleString()}
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {t('weeklyGoal')}
          </p>
        </div>
      </div>
    </div>
  );
}
