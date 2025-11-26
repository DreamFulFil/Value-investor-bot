import { useState, useEffect } from 'react';
import { portfolioApi } from '@/lib/api';

export interface Position {
  symbol: string;
  shares: number;
  avgCost: number;
  currentPrice: number;
  totalValue: number;
  gainLoss: number;
  gainLossPercent: number;
  dividendYield: number;
}

export interface PortfolioSnapshot {
  date: string;
  totalValue: number;
  cash: number;
  investedAmount: number;
  unrealizedGainLoss: number;
}

export interface Portfolio {
  totalValue: number;
  cash: number;
  investedAmount: number;
  unrealizedGainLoss: number;
  dayChange: number;
  dayChangePercent: number;
  positions: Position[];
  weeklyDividendIncome: number;
}

export const usePortfolio = () => {
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPortfolio = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await portfolioApi.getCurrent();
      setPortfolio(response.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch portfolio');
      console.error('Error fetching portfolio:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPortfolio();
  }, []);

  const refresh = () => {
    fetchPortfolio();
  };

  return { portfolio, loading, error, refresh };
};

export const usePortfolioHistory = (months: number = 6) => {
  const [history, setHistory] = useState<PortfolioSnapshot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await portfolioApi.getHistory(months);
        setHistory(response.data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch history');
        console.error('Error fetching portfolio history:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, [months]);

  return { history, loading, error };
};

export const useDeposit = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const deposit = async (amount: number) => {
    try {
      setLoading(true);
      setError(null);
      await portfolioApi.deposit(amount);
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to deposit');
      console.error('Error depositing:', err);
      return false;
    } finally {
      setLoading(false);
    }
  };

  return { deposit, loading, error };
};
