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

export const fetchPortfolioSummary = async (): Promise<PortfolioSummary> => {
  const { data } = await api.get('/portfolio/summary');
  return data;
};

export const fetchPositions = async (): Promise<Position[]> => {
  const { data } = await api.get('/portfolio/positions');
  return data;
};

export const fetchDividendSummary = async (): Promise<DividendSummary> => {
  const { data } = await api.get('/dividends/summary');
  return data;
};

export const fetchPortfolioHistory = async (): Promise<PortfolioHistory[]> => {
  const { data } = await api.get('/portfolio/history');
  return data;
};

export const fetchInsights = async (): Promise<Insight[]> => {
  const { data } = await api.get('/insights');
  return data;
};

export const runMonthlyRebalance = async (): Promise<RebalanceResult> => {
  const { data } = await api.post('/rebalance/monthly');
  return data;
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
