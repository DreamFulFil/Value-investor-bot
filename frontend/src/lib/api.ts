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

export interface QuotaStatus {
  usedMB: number;
  limitMB: number;
  remainingMB: number;
  percentageUsed: number;
  fallbackActive: boolean;
}

export interface AppConfig {
  tradingMode: 'SIMULATION' | 'LIVE';
  monthlyInvestment: number;
  targetWeeklyDividend: number;
  currency: string;
}

export interface ProgressEvent {
  type: 'deposit' | 'screening' | 'fetching_prices' | 'buying' | 'generating_insights' | 'complete' | 'error';
  message: string;
  percentage: number;
  timestamp: number;
}

// Backend endpoints:
// /api/portfolio/current - Get current portfolio
// /api/portfolio/history - Get portfolio snapshots
// /api/insights/current - Get current insight
// /api/insights/history - Get insight history
// /api/trading/rebalance - Trigger rebalance
// /api/trading/rebalance/progress - SSE for progress updates
// /api/config - Get app config

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
    const positions = data.positions || [];
    const totalValue = data.totalValue || 1;
    return positions.map((p: { symbol: string; quantity: number; currentPrice: number; marketValue: number; averagePrice: number; unrealizedPL: number }) => ({
      symbol: p.symbol,
      name: p.symbol,
      shares: p.quantity || 0,
      currentPrice: p.currentPrice || 0,
      marketValue: p.marketValue || 0,
      costBasis: (p.quantity || 0) * (p.averagePrice || 0),
      unrealizedGain: p.unrealizedPL || 0,
      weight: totalValue > 0 ? ((p.marketValue || 0) / totalValue) * 100 : 0,
    }));
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
    message: data.message || data.errorMessage || 'Rebalance completed',
    tradesExecuted: data.totalTransactions || 0,
    newPositions: data.monthlyResults?.[0]?.stocksPurchased || 0,
    timestamp: data.endTime,
  };
};

export const fetchQuotaStatus = async (): Promise<QuotaStatus> => {
  try {
    const { data } = await api.get('/quota');
    return {
      usedMB: data.usedMB || 0,
      limitMB: data.limitMB || 500,
      remainingMB: data.remainingMB || 500,
      percentageUsed: data.percentageUsed || 0,
      fallbackActive: data.fallbackActive || false,
    };
  } catch {
    return { usedMB: 0, limitMB: 500, remainingMB: 500, percentageUsed: 0, fallbackActive: false };
  }
};

export const fetchAppConfig = async (): Promise<AppConfig> => {
  try {
    const { data } = await api.get('/config');
    return {
      tradingMode: data.tradingMode || 'SIMULATION',
      monthlyInvestment: data.monthlyInvestment || 16000,
      targetWeeklyDividend: data.targetWeeklyDividend || 1600,
      currency: data.currency || 'TWD',
    };
  } catch {
    return { tradingMode: 'SIMULATION', monthlyInvestment: 16000, targetWeeklyDividend: 1600, currency: 'TWD' };
  }
};

export const checkHealth = async (): Promise<boolean> => {
  try {
    await api.get('/health');
    return true;
  } catch {
    return false;
  }
};

// SSE helper for progress updates
export const createProgressEventSource = (): EventSource => {
  return new EventSource('/api/trading/rebalance/progress');
};

export default api;
