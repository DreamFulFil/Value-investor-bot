import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

export interface PortfolioSummary {
  totalValue: number;
  totalCost: number;
  totalReturn: number;
  returnPercentage: number;
  cashBalance: number;
  positionCount: number;
}

export interface Position {
  symbol: string;
  name: string;
  shares: number;
  currentPrice: number;
  marketValue: number;
  costBasis: number;
  unrealizedGain: number;
  weight: number;
}

export interface DividendSummary {
  totalDividends: number;
  ytdDividends: number;
  lastMonthDividends: number;
  projectedAnnualDividends: number;
}

export interface RebalanceResult {
  success: boolean;
  message: string;
  tradesExecuted: number;
  newPositions: number;
  timestamp: string;
}

export interface PortfolioHistory {
  date: string;
  value: number;
}

export interface Insight {
  id: number;
  content: string;
  createdAt: string;
}

// Backend endpoints:
// /api/portfolio/current - Get current portfolio
// /api/portfolio/history - Get portfolio snapshots
// /api/insights/current - Get current insight
// /api/insights/history - Get insight history
// /api/trading/rebalance - Trigger rebalance

export const fetchPortfolioSummary = async (): Promise<PortfolioSummary> => {
  try {
    const { data } = await api.get('/portfolio/current');
    return {
      totalValue: data.totalValue || 0,
      totalCost: data.totalCost || 0,
      totalReturn: data.unrealizedGain || 0,
      returnPercentage: data.returnPercentage || 0,
      cashBalance: data.cashBalance || 0,
      positionCount: data.positions?.length || 0,
    };
  } catch {
    return { totalValue: 0, totalCost: 0, totalReturn: 0, returnPercentage: 0, cashBalance: 0, positionCount: 0 };
  }
};

export const fetchPositions = async (): Promise<Position[]> => {
  try {
    const { data } = await api.get('/portfolio/current');
    return data.positions || [];
  } catch {
    return [];
  }
};

export const fetchDividendSummary = async (): Promise<DividendSummary> => {
  try {
    const { data } = await api.get('/portfolio/metrics');
    return {
      totalDividends: data.totalDividendsReceived || 0,
      ytdDividends: data.ytdDividends || 0,
      lastMonthDividends: data.lastMonthDividends || 0,
      projectedAnnualDividends: data.projectedAnnualDividends || 0,
    };
  } catch {
    return { totalDividends: 0, ytdDividends: 0, lastMonthDividends: 0, projectedAnnualDividends: 0 };
  }
};

export const fetchPortfolioHistory = async (): Promise<PortfolioHistory[]> => {
  try {
    // No date params needed - backend defaults to last 365 days
    const { data } = await api.get('/portfolio/history');
    if (!Array.isArray(data)) return [];
    return data.map((item: { timestamp?: string; snapshotDate?: string; totalValue: number }) => ({
      date: item.timestamp || item.snapshotDate || new Date().toISOString(),
      value: item.totalValue || 0,
    }));
  } catch {
    return [];
  }
};

export const fetchInsights = async (): Promise<Insight[]> => {
  try {
    const { data } = await api.get('/insights/history');
    return data || [];
  } catch {
    return [];
  }
};

export const runMonthlyRebalance = async (): Promise<RebalanceResult> => {
  const { data } = await api.post('/trading/rebalance');
  return {
    success: data.success,
    message: data.errorMessage || 'Rebalance completed',
    tradesExecuted: data.totalTransactions || 0,
    newPositions: data.monthlyResults?.[0]?.stocksPurchased || 0,
    timestamp: data.endTime,
  };
};

export const checkHealth = async (): Promise<boolean> => {
  try {
    await api.get('/health');
    return true;
  } catch {
    return false;
  }
};

export default api;
