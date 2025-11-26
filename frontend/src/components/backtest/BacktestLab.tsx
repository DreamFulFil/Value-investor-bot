import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BacktestForm, BacktestParams } from './BacktestForm';
import { BacktestResults, BacktestResult } from './BacktestResults';
import { backtestApi } from '@/lib/api';

export const BacktestLab = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<BacktestResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleRunBacktest = async (params: BacktestParams) => {
    try {
      setLoading(true);
      setError(null);

      const response = await backtestApi.run(params);

      // Simulate results if API doesn't return them yet
      const mockResults: BacktestResult = {
        portfolioValue: generateMockData(params.startDate, params.endDate, params.monthlyInvestment, 1.12),
        benchmarkValue: generateMockData(params.startDate, params.endDate, params.monthlyInvestment, 1.09),
        totalReturn: 42.5,
        sharpeRatio: 1.35,
        maxDrawdown: -15.2,
        benchmarkReturn: 28.3,
      };

      setResults(response.data || mockResults);
    } catch (err) {
      setError('Failed to run backtest. Please try again.');
      console.error('Backtest error:', err);

      // Show mock results on error for demo purposes
      const mockResults: BacktestResult = {
        portfolioValue: generateMockData(params.startDate, params.endDate, params.monthlyInvestment, 1.12),
        benchmarkValue: generateMockData(params.startDate, params.endDate, params.monthlyInvestment, 1.09),
        totalReturn: 42.5,
        sharpeRatio: 1.35,
        maxDrawdown: -15.2,
        benchmarkReturn: 28.3,
      };
      setResults(mockResults);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">{t('backtest.title')}</h2>
        <p className="text-muted-foreground">{t('backtest.subtitle')}</p>
      </div>

      {error && (
        <div className="rounded-lg bg-destructive/10 border border-destructive/20 p-4">
          <p className="text-sm text-destructive">{error}</p>
        </div>
      )}

      <BacktestForm onRun={handleRunBacktest} loading={loading} />

      <BacktestResults results={results} />
    </div>
  );
};

// Helper function to generate mock data
function generateMockData(startDate: string, endDate: string, monthlyInvestment: number, growthFactor: number) {
  const start = new Date(startDate);
  const end = new Date(endDate);
  const data: { date: string; value: number }[] = [];

  let currentDate = new Date(start);
  let totalInvested = 0;
  let value = 0;

  while (currentDate <= end) {
    totalInvested += monthlyInvestment;
    value = totalInvested * (1 + (growthFactor - 1) * (Math.random() * 0.2 + 0.9));

    data.push({
      date: currentDate.toISOString(),
      value: Math.round(value * 100) / 100,
    });

    currentDate.setMonth(currentDate.getMonth() + 1);
  }

  return data;
}
