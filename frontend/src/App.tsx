import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { QueryClient, QueryClientProvider, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Header,
  StatCard,
  RebalanceButton,
  GoalRing,
  PortfolioChart,
  AllocationPie,
  HoldingsTable,
  InsightsPanel,
  GoLiveWizard,
  QuotaCard,
  ProgressModal,
  SimulationBadge,
} from './components';
import {
  fetchPortfolioSummary,
  fetchPositions,
  fetchDividendSummary,
  fetchPortfolioHistory,
  fetchInsights,
  fetchQuotaStatus,
  fetchAppConfig,
  runMonthlyRebalance,
} from './lib/api';
import './i18n';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30000,
    },
  },
});

function Dashboard() {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const [hasRebalanced, setHasRebalanced] = useState(() => {
    return localStorage.getItem('hasRebalanced') === 'true';
  });
  const [showSuccess, setShowSuccess] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [showGoLiveWizard, setShowGoLiveWizard] = useState(false);
  const [showProgressModal, setShowProgressModal] = useState(false);

  const { data: summary } = useQuery({
    queryKey: ['portfolioSummary'],
    queryFn: fetchPortfolioSummary,
  });

  const { data: positions = [] } = useQuery({
    queryKey: ['positions'],
    queryFn: fetchPositions,
  });

  const { data: dividends } = useQuery({
    queryKey: ['dividends'],
    queryFn: fetchDividendSummary,
  });

  const { data: history = [] } = useQuery({
    queryKey: ['portfolioHistory'],
    queryFn: fetchPortfolioHistory,
  });

  const { data: insights = [] } = useQuery({
    queryKey: ['insights'],
    queryFn: fetchInsights,
  });

  const { data: quota, isLoading: quotaLoading } = useQuery({
    queryKey: ['quota'],
    queryFn: fetchQuotaStatus,
    refetchInterval: 5 * 60 * 1000, // Poll every 5 minutes
  });

  const { data: appConfig } = useQuery({
    queryKey: ['appConfig'],
    queryFn: fetchAppConfig,
  });

  const rebalanceMutation = useMutation({
    mutationFn: runMonthlyRebalance,
    onMutate: () => {
      setShowProgressModal(true);
    },
    onSuccess: (data) => {
      setHasRebalanced(true);
      localStorage.setItem('hasRebalanced', 'true');
      
      // Show appropriate message
      if (data.message?.includes('Already rebalanced')) {
        setSuccessMessage(t('alreadyRebalancedThisMonth'));
      } else if (data.newPositions > 0) {
        setSuccessMessage(t('success'));
      } else {
        setSuccessMessage(t('success'));
      }
      
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 4000);
      qc.invalidateQueries();
    },
    onError: () => {
      setShowProgressModal(false);
    },
  });

  const handleProgressModalClose = () => {
    setShowProgressModal(false);
    qc.invalidateQueries();
  };

  const handleGoLive = (option: 'fresh' | 'gradual' | 'oneshot', amount: number) => {
    console.log('Go Live:', option, amount);
    // TODO: Implement actual go-live API call
    alert(`Going live with option: ${option}, amount: NT$${amount.toLocaleString()}`);
  };

  const isEmpty = !hasRebalanced || (summary?.totalValue === 0 && positions.length === 0);
  const weeklyDividend = dividends ? dividends.projectedAnnualDividends / 52 : 0;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Simulation Mode Badge */}
        {appConfig && (
          <div className="flex justify-center mb-4">
            <SimulationBadge mode={appConfig.tradingMode} />
          </div>
        )}

        {showSuccess && (
          <div className="fixed top-20 right-4 z-50 animate-bounce">
            <div className="bg-green-500 text-white px-6 py-3 rounded-xl shadow-lg flex items-center gap-2">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              {successMessage}
            </div>
          </div>
        )}

        <div className="mb-8">
          <RebalanceButton
            hasRebalanced={hasRebalanced}
            isLoading={rebalanceMutation.isPending}
            onClick={() => rebalanceMutation.mutate()}
          />
          {rebalanceMutation.isError && (
            <p className="text-center text-red-500 mt-2 text-sm">
              {t('error')}: {(rebalanceMutation.error as Error).message}
            </p>
          )}
          
          {/* Go Live Button - show after some backtest months */}
          {hasRebalanced && summary && summary.totalValue > 0 && (
            <div className="text-center mt-4">
              <button
                onClick={() => setShowGoLiveWizard(true)}
                className="px-6 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg font-medium transition-colors"
              >
                {t('goLive')} →
              </button>
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
          <StatCard
            title={t('portfolioValue')}
            value={summary?.totalValue ?? 0}
            trend={summary?.returnPercentage}
            isEmpty={isEmpty}
            icon={<svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>}
          />
          <StatCard
            title={t('totalReturn')}
            value={summary?.totalReturn ?? 0}
            trend={summary?.returnPercentage}
            isEmpty={isEmpty}
            icon={<svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" /></svg>}
          />
          <StatCard
            title={t('dividendIncome')}
            value={dividends?.ytdDividends ?? 0}
            subtitle={`${t('lastRebalance')}: ${hasRebalanced ? new Date().toLocaleDateString() : t('never')}`}
            isEmpty={isEmpty}
            icon={<svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" /></svg>}
          />
          <StatCard
            title={t('cashBalance')}
            value={summary?.cashBalance ?? 0}
            isEmpty={isEmpty}
            icon={<svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>}
          />
          <QuotaCard quota={quota} isLoading={quotaLoading} />
        </div>

        {/* Fallback warning toast */}
        {quota?.fallbackActive && (
          <div className="mb-4 p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg text-yellow-700 dark:text-yellow-400 text-sm flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            {t('quotaLowFallback')}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          <PortfolioChart data={history} isEmpty={isEmpty} />
          <AllocationPie positions={positions} isEmpty={isEmpty} />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <HoldingsTable positions={positions} isEmpty={isEmpty} />
          </div>
          <div className="space-y-6">
            <GoalRing current={weeklyDividend} target={1600} isEmpty={isEmpty} />
            <InsightsPanel insights={insights} isEmpty={isEmpty} />
          </div>
        </div>
      </main>

      <footer className="mt-12 py-6 text-center text-sm text-gray-500 dark:text-gray-400">
        <p>Value Investor Bot - Taiwan Edition © {new Date().getFullYear()}</p>
      </footer>

      {/* Go Live Wizard Modal */}
      <GoLiveWizard
        isOpen={showGoLiveWizard}
        onClose={() => setShowGoLiveWizard(false)}
        currentBacktestValue={summary?.totalValue ?? 0}
        onGoLive={handleGoLive}
      />

      {/* Progress Modal */}
      <ProgressModal
        isOpen={showProgressModal}
        onClose={handleProgressModalClose}
      />
    </div>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Dashboard />
    </QueryClientProvider>
  );
}

export default App;
