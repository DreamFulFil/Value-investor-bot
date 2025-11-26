import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for adding auth token if needed
api.interceptors.request.use(
  (config) => {
    // Add auth token here if implementing authentication
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for handling errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// Portfolio endpoints
export const portfolioApi = {
  getCurrent: () => api.get('/portfolio/current'),
  getSnapshots: (startDate?: string, endDate?: string) =>
    api.get('/portfolio/snapshots', { params: { startDate, endDate } }),
  deposit: (amount: number) => api.post('/portfolio/deposit', { amount }),
  getHistory: (months: number = 6) =>
    api.get('/portfolio/history', { params: { months } }),
};

// Trading endpoints
export const tradingApi = {
  getTransactions: (limit: number = 20) =>
    api.get('/trading/transactions', { params: { limit } }),
  rebalance: () => api.post('/trading/rebalance'),
  executeOrder: (order: { symbol: string; shares: number; type: 'BUY' | 'SELL' }) =>
    api.post('/trading/execute', order),
};

// Insights endpoints
export const insightsApi = {
  getCurrent: () => api.get('/insights/current'),
  getPortfolioReport: () => api.get('/insights/portfolio'),
  getDailyTip: () => api.get('/insights/learning/daily'),
  likeTip: (id: number) => api.post(`/insights/learning/${id}/like`),
  getLearningHistory: (category?: string) =>
    api.get('/insights/learning/history', { params: { category } }),
};

// Market data endpoints
export const marketApi = {
  getUniverse: () => api.get('/data/universe'),
  getTopDividends: (limit: number = 10) =>
    api.get('/market/top-dividends', { params: { limit } }),
  getQuote: (symbol: string) => api.get(`/market/quote/${symbol}`),
};

// Backtest endpoints
export const backtestApi = {
  run: (params: {
    startDate: string;
    endDate: string;
    monthlyInvestment: number;
    symbols: string[];
  }) => api.post('/backtest/run', params),
  getResults: (backtestId: string) => api.get(`/backtest/results/${backtestId}`),
};

// Goals endpoints
export const goalsApi = {
  getProgress: () => api.get('/goals/progress'),
  setWeeklyIncomeGoal: (amount: number) =>
    api.post('/goals/weekly-income', { amount }),
  getMilestones: () => api.get('/goals/milestones'),
};

export default api;
