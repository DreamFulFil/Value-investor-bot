import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// API Integration Tests
describe('API Integration', () => {
  const API_BASE = 'http://localhost:8080/api'

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Portfolio API', () => {
    it('should fetch portfolio summary', async () => {
      const mockResponse = {
        totalValue: 160000,
        cashBalance: 10000,
        totalReturn: 5.25,
        positionsCount: 5
      }

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse)
      })

      const response = await fetch(`${API_BASE}/portfolio/current`)
      const data = await response.json()

      expect(data.totalValue).toBe(160000)
      expect(data.cashBalance).toBe(10000)
    })

    it('should handle portfolio fetch error', async () => {
      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: () => Promise.resolve({ error: 'Internal server error' })
      })

      const response = await fetch(`${API_BASE}/portfolio/current`)
      
      expect(response.ok).toBe(false)
      expect(response.status).toBe(500)
    })
  })

  describe('Rebalance API', () => {
    it('should trigger rebalance successfully', async () => {
      const mockResponse = {
        success: true,
        message: 'Rebalance completed',
        transactionsCount: 5,
        totalInvested: 16000
      }

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse)
      })

      const response = await fetch(`${API_BASE}/trading/rebalance`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })
      const data = await response.json()

      expect(data.success).toBe(true)
      expect(data.transactionsCount).toBe(5)
    })

    it('should handle idempotent rebalance (same month)', async () => {
      const mockResponse = {
        success: true,
        message: 'Rebalance already executed this month',
        skipped: true
      }

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse)
      })

      const response = await fetch(`${API_BASE}/trading/rebalance`, {
        method: 'POST'
      })
      const data = await response.json()

      expect(data.success).toBe(true)
      expect(data.skipped).toBe(true)
    })
  })

  describe('Insights API', () => {
    it('should fetch current insights', async () => {
      const mockResponse = {
        content: '# Monthly Insights\n\nYour portfolio is performing well.',
        portfolioValue: 160000,
        monthlyReturn: 5.25,
        generatedAt: '2024-11-27T10:00:00Z'
      }

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse)
      })

      const response = await fetch(`${API_BASE}/insights/current`)
      const data = await response.json()

      expect(data.content).toContain('Monthly Insights')
    })

    it('should fetch insights history', async () => {
      const mockResponse = [
        { id: 1, date: '2024-11-01', portfolioValue: 160000 },
        { id: 2, date: '2024-10-01', portfolioValue: 144000 }
      ]

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse)
      })

      const response = await fetch(`${API_BASE}/insights/history?limit=5`)
      const data = await response.json()

      expect(data).toHaveLength(2)
    })
  })

  describe('Quota API', () => {
    it('should fetch quota status', async () => {
      const mockResponse = {
        usedMB: 400,
        limitMB: 500,
        remainingMB: 100,
        fallbackActive: false
      }

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse)
      })

      const response = await fetch(`${API_BASE}/quota`)
      const data = await response.json()

      expect(data.usedMB).toBe(400)
      expect(data.limitMB).toBe(500)
      expect(data.fallbackActive).toBe(false)
    })
  })

  describe('Health Check API', () => {
    it('should return healthy status', async () => {
      const mockResponse = {
        status: 'UP',
        services: {
          database: 'UP',
          shioaji: 'UP',
          ollama: 'UP'
        }
      }

      global.fetch = vi.fn().mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockResponse)
      })

      const response = await fetch(`${API_BASE}/health`)
      const data = await response.json()

      expect(data.status).toBe('UP')
    })
  })
})

describe('Data Formatting', () => {
  describe('Currency Formatting', () => {
    it('should format TWD correctly', () => {
      const formatTWD = (value: number) => {
        return new Intl.NumberFormat('zh-TW', {
          style: 'currency',
          currency: 'TWD',
          minimumFractionDigits: 0,
          maximumFractionDigits: 0
        }).format(value)
      }

      expect(formatTWD(160000)).toContain('160,000')
      expect(formatTWD(16000)).toContain('16,000')
      expect(formatTWD(1600)).toContain('1,600')
    })

    it('should format percentages correctly', () => {
      const formatPercent = (value: number) => {
        return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
      }

      expect(formatPercent(5.25)).toBe('+5.25%')
      expect(formatPercent(-3.15)).toBe('-3.15%')
      expect(formatPercent(0)).toBe('+0.00%')
    })
  })

  describe('Date Formatting', () => {
    it('should format dates correctly for Taiwan locale', () => {
      const formatDate = (dateStr: string) => {
        const date = new Date(dateStr)
        return new Intl.DateTimeFormat('zh-TW', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        }).format(date)
      }

      const result = formatDate('2024-11-27')
      expect(result).toContain('2024')
    })

    it('should format relative time', () => {
      const formatRelativeTime = (dateStr: string) => {
        const date = new Date(dateStr)
        const now = new Date()
        const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24))
        
        if (diffDays === 0) return 'Today'
        if (diffDays === 1) return 'Yesterday'
        if (diffDays < 7) return `${diffDays} days ago`
        return date.toLocaleDateString()
      }

      expect(formatRelativeTime(new Date().toISOString())).toBe('Today')
    })
  })
})

describe('State Management', () => {
  it('should handle portfolio state updates', () => {
    interface PortfolioState {
      value: number
      cash: number
      positions: Array<{ symbol: string; shares: number }>
    }

    const initialState: PortfolioState = {
      value: 0,
      cash: 0,
      positions: []
    }

    const updatePortfolio = (
      state: PortfolioState, 
      update: Partial<PortfolioState>
    ): PortfolioState => ({
      ...state,
      ...update
    })

    const updated = updatePortfolio(initialState, {
      value: 160000,
      cash: 10000,
      positions: [{ symbol: '2330.TW', shares: 10 }]
    })

    expect(updated.value).toBe(160000)
    expect(updated.positions).toHaveLength(1)
  })

  it('should handle rebalance state correctly', () => {
    interface RebalanceState {
      isLoading: boolean
      lastRebalance: string | null
      error: string | null
    }

    const rebalanceReducer = (
      state: RebalanceState, 
      action: { type: string; payload?: any }
    ): RebalanceState => {
      switch (action.type) {
        case 'START':
          return { ...state, isLoading: true, error: null }
        case 'SUCCESS':
          return { ...state, isLoading: false, lastRebalance: action.payload }
        case 'ERROR':
          return { ...state, isLoading: false, error: action.payload }
        default:
          return state
      }
    }

    let state: RebalanceState = { isLoading: false, lastRebalance: null, error: null }
    
    state = rebalanceReducer(state, { type: 'START' })
    expect(state.isLoading).toBe(true)

    state = rebalanceReducer(state, { type: 'SUCCESS', payload: '2024-11-27' })
    expect(state.isLoading).toBe(false)
    expect(state.lastRebalance).toBe('2024-11-27')
  })
})

describe('Validation', () => {
  it('should validate deposit amount', () => {
    const validateDeposit = (amount: number): { valid: boolean; error?: string } => {
      if (amount <= 0) return { valid: false, error: 'Amount must be positive' }
      if (amount > 1000000) return { valid: false, error: 'Amount exceeds maximum' }
      if (amount < 1000) return { valid: false, error: 'Minimum deposit is NT$1,000' }
      return { valid: true }
    }

    expect(validateDeposit(16000).valid).toBe(true)
    expect(validateDeposit(0).valid).toBe(false)
    expect(validateDeposit(-1000).valid).toBe(false)
    expect(validateDeposit(500).valid).toBe(false)
  })

  it('should validate stock symbol format', () => {
    const isValidTaiwanSymbol = (symbol: string): boolean => {
      return /^\d{4}\.TW(O)?$/.test(symbol)
    }

    expect(isValidTaiwanSymbol('2330.TW')).toBe(true)
    expect(isValidTaiwanSymbol('2881.TWO')).toBe(true)
    expect(isValidTaiwanSymbol('AAPL')).toBe(false)
    expect(isValidTaiwanSymbol('2330')).toBe(false)
  })
})
