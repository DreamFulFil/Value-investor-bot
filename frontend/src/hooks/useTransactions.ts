import { useState, useEffect } from 'react';
import { tradingApi } from '@/lib/api';

export interface Transaction {
  id: number;
  date: string;
  type: 'BUY' | 'SELL' | 'DIVIDEND';
  symbol: string;
  shares: number;
  price: number;
  total: number;
  notes?: string;
}

export const useTransactions = (limit: number = 20) => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await tradingApi.getTransactions(limit);
      setTransactions(response.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch transactions');
      console.error('Error fetching transactions:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [limit]);

  const refresh = () => {
    fetchTransactions();
  };

  return { transactions, loading, error, refresh };
};

export const useRebalance = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const rebalance = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await tradingApi.rebalance();
      return response.data;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to rebalance');
      console.error('Error rebalancing:', err);
      return null;
    } finally {
      setLoading(false);
    }
  };

  return { rebalance, loading, error };
};
