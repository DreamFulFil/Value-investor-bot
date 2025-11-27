/**
 * API Response Fixtures - Captured from actual backend responses
 * Used for contract and integration tests
 */

// Actual response from GET /api/portfolio/current
export const portfolioCurrentResponse = {
  totalValue: 15342.49652890402273483437,
  cashBalance: 0,
  investedAmount: 15999.9999975850,
  totalPL: -657.50346868097726516563,
  plPercentage: -4.1100,
  positionCount: 5,
  positions: [
    {
      symbol: "2002.TW",
      quantity: 170.66666666,
      averagePrice: 18.75,
      currentPrice: 18.1,
      marketValue: 3089.066666546,
      unrealizedPL: -110.9333333290,
      plPercentage: -3.4700
    },
    {
      symbol: "2303.TW",
      quantity: 68.52248394,
      averagePrice: 46.7,
      currentPrice: 45.6,
      marketValue: 3124.625267664,
      unrealizedPL: -75.374732334,
      plPercentage: -2.3600
    },
    {
      symbol: "2317.TW",
      quantity: 12.28406909,
      averagePrice: 260.5,
      currentPrice: 228,
      marketValue: 2800.76775252,
      unrealizedPL: -399.232245425,
      plPercentage: -12.4800
    },
    {
      symbol: "2886.TW",
      quantity: 77.66990291,
      averagePrice: 41.2,
      currentPrice: 40.1,
      marketValue: 3114.563106691,
      unrealizedPL: -85.436893201,
      plPercentage: -2.6700
    },
    {
      symbol: "5880.TW",
      quantity: 134.7368421,
      averagePrice: 23.75,
      currentPrice: 23.8500003814697,
      marketValue: 3213.47373548302273483437,
      unrealizedPL: 13.47373560802273483437,
      plPercentage: 0.4200
    }
  ]
};

// Actual response from GET /api/portfolio/history
export const portfolioHistoryResponse = [
  {
    id: 1,
    timestamp: "2025-11-27T20:29:38.537",
    totalValue: 15342.496528904,
    cashBalance: 0,
    investedAmount: 15999.999997585,
    totalPL: -657.503468680977,
    positionsJson: "[{\"symbol\":\"2002.TW\",\"quantity\":170.66666666}]",
    snapshotType: "MONTHLY_REBALANCE"
  }
];

// Actual response from GET /api/quota
export const quotaResponse = {
  usedMB: 0,
  limitMB: 500,
  remainingMB: 500,
  percentageUsed: 0,
  fallbackActive: false,
  timestamp: "2025-11-27T12:00:00"
};

// Actual response from GET /api/health
export const healthResponse = {
  status: "UP",
  timestamp: "2025-11-27T12:00:00",
  service: "Value Investor Bot",
  version: "0.0.1",
  tradingMode: "SIMULATION"
};

// Actual response from GET /api/config
export const configResponse = {
  tradingMode: "SIMULATION",
  monthlyInvestment: 16000,
  targetWeeklyDividend: 1600,
  currency: "TWD"
};

// Edge case: Empty portfolio
export const emptyPortfolioResponse = {
  totalValue: 0,
  cashBalance: 0,
  investedAmount: 0,
  totalPL: 0,
  plPercentage: 0,
  positionCount: 0,
  positions: []
};

// Edge case: Portfolio with null fields
export const portfolioWithNullsResponse = {
  totalValue: null,
  cashBalance: null,
  investedAmount: null,
  totalPL: null,
  plPercentage: null,
  positionCount: 0,
  positions: null
};

// Edge case: Position with null/undefined fields
export const positionWithNullsResponse = {
  symbol: "2330.TW",
  quantity: null,
  averagePrice: undefined,
  currentPrice: null,
  marketValue: undefined,
  unrealizedPL: null,
  plPercentage: undefined
};

// Edge case: Malformed response (missing fields)
export const malformedPortfolioResponse = {
  totalValue: 10000
  // All other fields missing
};
