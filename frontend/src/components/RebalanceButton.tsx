import { useTranslation } from 'react-i18next';
import confetti from 'canvas-confetti';

interface RebalanceButtonProps {
  hasRebalanced: boolean;
  isLoading: boolean;
  onClick: () => void;
}

export function RebalanceButton({ hasRebalanced, isLoading, onClick }: RebalanceButtonProps) {
  const { t } = useTranslation();

  const handleClick = () => {
    onClick();
    if (!hasRebalanced) {
      // Fire confetti on first rebalance
      setTimeout(() => {
        confetti({
          particleCount: 150,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
        });
      }, 500);
    }
  };

  return (
    <button
      onClick={handleClick}
      disabled={isLoading}
      className={`
        relative overflow-hidden
        w-full max-w-md mx-auto
        py-5 px-8
        text-xl font-bold text-white
        bg-gradient-to-r from-blue-500 via-blue-600 to-blue-700
        hover:from-blue-600 hover:via-blue-700 hover:to-blue-800
        rounded-2xl
        shadow-2xl hover:shadow-blue-500/50
        transform transition-all duration-300
        hover:scale-105 active:scale-95
        disabled:opacity-70 disabled:cursor-not-allowed disabled:transform-none
        focus:outline-none focus:ring-4 focus:ring-blue-300 dark:focus:ring-blue-800
        ${!hasRebalanced ? 'animate-pulse-slow' : ''}
      `}
    >
      {/* Shine effect */}
      <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full hover:translate-x-full transition-transform duration-1000" />
      
      {/* Button content */}
      <span className="relative flex items-center justify-center gap-3">
        {isLoading ? (
          <>
            <svg className="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            {t('rebalancing')}
          </>
        ) : (
          hasRebalanced ? t('runMonthlyRebalance') : t('runFirstRebalance')
        )}
      </span>
    </button>
  );
}
