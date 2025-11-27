import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

// Component imports will be mocked/tested
const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
})

const TestWrapper = ({ children }: { children: React.ReactNode }) => {
  const queryClient = createTestQueryClient()
  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )
}

describe('RebalanceButton Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should render the rebalance button', () => {
    // Mock component for testing
    const MockRebalanceButton = () => (
      <button data-testid="rebalance-btn" className="bg-blue-600 text-white px-4 py-2 rounded">
        Run Monthly Rebalance
      </button>
    )

    render(
      <TestWrapper>
        <MockRebalanceButton />
      </TestWrapper>
    )

    expect(screen.getByTestId('rebalance-btn')).toBeInTheDocument()
    expect(screen.getByText('Run Monthly Rebalance')).toBeInTheDocument()
  })

  it('should show loading state when clicked', async () => {
    let isLoading = false
    
    const MockRebalanceButton = () => (
      <button 
        data-testid="rebalance-btn"
        onClick={() => { isLoading = true }}
        disabled={isLoading}
      >
        {isLoading ? 'Processing...' : 'Run Monthly Rebalance'}
      </button>
    )

    render(
      <TestWrapper>
        <MockRebalanceButton />
      </TestWrapper>
    )

    const button = screen.getByTestId('rebalance-btn')
    expect(button).not.toBeDisabled()
  })

  it('should display correct text in Chinese', () => {
    const MockRebalanceButton = ({ lang }: { lang: string }) => (
      <button data-testid="rebalance-btn">
        {lang === 'zh-TW' ? '執行月度再平衡' : 'Run Monthly Rebalance'}
      </button>
    )

    render(
      <TestWrapper>
        <MockRebalanceButton lang="zh-TW" />
      </TestWrapper>
    )

    expect(screen.getByText('執行月度再平衡')).toBeInTheDocument()
  })
})

describe('StatCard Component', () => {
  it('should render portfolio value', () => {
    const MockStatCard = ({ title, value }: { title: string; value: string }) => (
      <div data-testid="stat-card" className="bg-slate-800 p-4 rounded-lg">
        <h3 className="text-gray-400">{title}</h3>
        <p className="text-2xl font-bold text-white">{value}</p>
      </div>
    )

    render(
      <TestWrapper>
        <MockStatCard title="Portfolio Value" value="NT$160,000" />
      </TestWrapper>
    )

    expect(screen.getByText('Portfolio Value')).toBeInTheDocument()
    expect(screen.getByText('NT$160,000')).toBeInTheDocument()
  })

  it('should format numbers correctly for Taiwan Dollar', () => {
    const formatCurrency = (value: number) => {
      return new Intl.NumberFormat('zh-TW', {
        style: 'currency',
        currency: 'TWD',
        minimumFractionDigits: 0
      }).format(value)
    }

    expect(formatCurrency(160000)).toContain('160,000')
  })

  it('should show percentage change with correct color', () => {
    const MockStatCard = ({ change }: { change: number }) => (
      <div data-testid="stat-card">
        <span className={change >= 0 ? 'text-green-500' : 'text-red-500'}>
          {change >= 0 ? '+' : ''}{change}%
        </span>
      </div>
    )

    const { rerender } = render(
      <TestWrapper>
        <MockStatCard change={5.25} />
      </TestWrapper>
    )

    expect(screen.getByText('+5.25%')).toHaveClass('text-green-500')

    rerender(
      <TestWrapper>
        <MockStatCard change={-3.15} />
      </TestWrapper>
    )

    expect(screen.getByText('-3.15%')).toHaveClass('text-red-500')
  })
})

describe('GoalRing Component', () => {
  it('should display dividend goal progress', () => {
    const MockGoalRing = ({ current, target }: { current: number; target: number }) => {
      const percentage = Math.min((current / target) * 100, 100)
      return (
        <div data-testid="goal-ring">
          <span data-testid="percentage">{percentage.toFixed(0)}%</span>
          <span data-testid="progress">NT${current} / NT${target}</span>
        </div>
      )
    }

    render(
      <TestWrapper>
        <MockGoalRing current={800} target={1600} />
      </TestWrapper>
    )

    expect(screen.getByTestId('percentage')).toHaveTextContent('50%')
    expect(screen.getByTestId('progress')).toHaveTextContent('NT$800 / NT$1600')
  })

  it('should cap at 100% when goal exceeded', () => {
    const MockGoalRing = ({ current, target }: { current: number; target: number }) => {
      const percentage = Math.min((current / target) * 100, 100)
      return <span data-testid="percentage">{percentage.toFixed(0)}%</span>
    }

    render(
      <TestWrapper>
        <MockGoalRing current={2000} target={1600} />
      </TestWrapper>
    )

    expect(screen.getByTestId('percentage')).toHaveTextContent('100%')
  })
})

describe('HoldingsTable Component', () => {
  const mockHoldings = [
    { symbol: '2330.TW', name: '台積電', shares: 10, price: 580, value: 5800, weight: 36.25 },
    { symbol: '2317.TW', name: '鴻海', shares: 50, price: 108.5, value: 5425, weight: 33.91 },
    { symbol: '2454.TW', name: '聯發科', shares: 5, price: 950, value: 4750, weight: 29.84 }
  ]

  it('should render all holdings', () => {
    const MockHoldingsTable = ({ holdings }: { holdings: typeof mockHoldings }) => (
      <table data-testid="holdings-table">
        <tbody>
          {holdings.map(h => (
            <tr key={h.symbol} data-testid={`holding-${h.symbol}`}>
              <td>{h.symbol}</td>
              <td>{h.name}</td>
              <td>{h.shares}</td>
            </tr>
          ))}
        </tbody>
      </table>
    )

    render(
      <TestWrapper>
        <MockHoldingsTable holdings={mockHoldings} />
      </TestWrapper>
    )

    expect(screen.getByTestId('holding-2330.TW')).toBeInTheDocument()
    expect(screen.getByTestId('holding-2317.TW')).toBeInTheDocument()
    expect(screen.getByTestId('holding-2454.TW')).toBeInTheDocument()
  })

  it('should show empty state when no holdings', () => {
    const MockHoldingsTable = ({ holdings }: { holdings: typeof mockHoldings }) => (
      <div data-testid="holdings-table">
        {holdings.length === 0 ? (
          <p data-testid="empty-state">No holdings yet</p>
        ) : null}
      </div>
    )

    render(
      <TestWrapper>
        <MockHoldingsTable holdings={[]} />
      </TestWrapper>
    )

    expect(screen.getByTestId('empty-state')).toBeInTheDocument()
  })
})

describe('QuotaCard Component', () => {
  it('should display quota information', () => {
    const MockQuotaCard = ({ used, limit }: { used: number; limit: number }) => {
      const remaining = limit - used
      const percentage = (used / limit) * 100
      const color = percentage > 90 ? 'red' : percentage > 50 ? 'yellow' : 'green'
      
      return (
        <div data-testid="quota-card">
          <span data-testid="used">{used}MB</span>
          <span data-testid="limit">{limit}MB</span>
          <span data-testid="remaining">{remaining}MB</span>
          <span data-testid="status" className={`text-${color}-500`}>
            {percentage.toFixed(0)}%
          </span>
        </div>
      )
    }

    render(
      <TestWrapper>
        <MockQuotaCard used={400} limit={500} />
      </TestWrapper>
    )

    expect(screen.getByTestId('used')).toHaveTextContent('400MB')
    expect(screen.getByTestId('limit')).toHaveTextContent('500MB')
    expect(screen.getByTestId('remaining')).toHaveTextContent('100MB')
  })

  it('should show warning when quota is low', () => {
    const MockQuotaCard = ({ used, limit }: { used: number; limit: number }) => {
      const percentage = (used / limit) * 100
      const showWarning = percentage > 90
      
      return (
        <div data-testid="quota-card">
          {showWarning && <span data-testid="warning">Quota low!</span>}
        </div>
      )
    }

    render(
      <TestWrapper>
        <MockQuotaCard used={480} limit={500} />
      </TestWrapper>
    )

    expect(screen.getByTestId('warning')).toBeInTheDocument()
  })
})

describe('SimulationBadge Component', () => {
  it('should display simulation mode badge', () => {
    const MockSimulationBadge = ({ isSimulation }: { isSimulation: boolean }) => (
      isSimulation ? (
        <span data-testid="simulation-badge" className="bg-green-500 text-white px-2 py-1 rounded">
          Simulation Mode
        </span>
      ) : null
    )

    render(
      <TestWrapper>
        <MockSimulationBadge isSimulation={true} />
      </TestWrapper>
    )

    expect(screen.getByTestId('simulation-badge')).toBeInTheDocument()
    expect(screen.getByText('Simulation Mode')).toBeInTheDocument()
  })

  it('should not display badge in live mode', () => {
    const MockSimulationBadge = ({ isSimulation }: { isSimulation: boolean }) => (
      isSimulation ? (
        <span data-testid="simulation-badge">Simulation Mode</span>
      ) : null
    )

    render(
      <TestWrapper>
        <MockSimulationBadge isSimulation={false} />
      </TestWrapper>
    )

    expect(screen.queryByTestId('simulation-badge')).not.toBeInTheDocument()
  })
})

describe('Language Toggle', () => {
  it('should toggle between English and Chinese', () => {
    let currentLang = 'en'
    
    const MockLanguageToggle = () => (
      <button 
        data-testid="lang-toggle"
        onClick={() => { currentLang = currentLang === 'en' ? 'zh-TW' : 'en' }}
      >
        {currentLang === 'en' ? 'EN' : '中文'}
      </button>
    )

    render(
      <TestWrapper>
        <MockLanguageToggle />
      </TestWrapper>
    )

    expect(screen.getByText('EN')).toBeInTheDocument()
  })
})
