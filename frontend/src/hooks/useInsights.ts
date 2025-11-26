import { useState, useEffect } from 'react';
import { insightsApi } from '@/lib/api';

export interface Insight {
  id: number;
  title: string;
  content: string;
  category: string;
  createdAt: string;
  liked?: boolean;
}

export interface DailyTip {
  id: number;
  title: string;
  content: string;
  category: string;
  date: string;
  liked: boolean;
}

export const useInsights = () => {
  const [insights, setInsights] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchInsights = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await insightsApi.getCurrent();
        setInsights(response.data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch insights');
        console.error('Error fetching insights:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchInsights();
  }, []);

  return { insights, loading, error };
};

export const usePortfolioReport = () => {
  const [report, setReport] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchReport = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await insightsApi.getPortfolioReport();
        setReport(response.data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch report');
        console.error('Error fetching portfolio report:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchReport();
  }, []);

  return { report, loading, error };
};

export const useDailyTip = () => {
  const [tip, setTip] = useState<DailyTip | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTip = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await insightsApi.getDailyTip();
      setTip(response.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch daily tip');
      console.error('Error fetching daily tip:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTip();
  }, []);

  const likeTip = async (id: number) => {
    try {
      await insightsApi.likeTip(id);
      setTip((prev) => (prev ? { ...prev, liked: true } : null));
    } catch (err) {
      console.error('Error liking tip:', err);
    }
  };

  return { tip, loading, error, likeTip, refresh: fetchTip };
};

export const useLearningHistory = (category?: string) => {
  const [tips, setTips] = useState<DailyTip[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await insightsApi.getLearningHistory(category);
        setTips(response.data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch learning history');
        console.error('Error fetching learning history:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, [category]);

  return { tips, loading, error };
};
