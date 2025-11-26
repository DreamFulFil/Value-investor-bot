import { usePortfolio } from '@/hooks/usePortfolio';
import { Card, CardContent } from '@/components/ui/card';
import { formatCurrency, formatPercentage, getChangeColor } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';
import { TrendingUp, TrendingDown, DollarSign, Wallet } from 'lucide-react';
import { PortfolioValueChart } from './PortfolioValueChart';
import { AllocationPieChart } from './AllocationPieChart';
import { DividendProgress } from './DividendProgress';
import { PositionsTable } from './PositionsTable';
import { TransactionsTable } from './TransactionsTable';
import { InsightsViewer } from './InsightsViewer';
import { PortfolioReportViewer } from './PortfolioReportViewer';

export const DashboardOverview = () => {
  const { t } = useTranslation();
  const { portfolio, loading } = usePortfolio();

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">{t('dashboard.title')}</h2>
        <p className="text-muted-foreground">
          Welcome back! Here's your portfolio overview.
        </p>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between space-y-0">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-blue-100 dark:bg-blue-900/20 rounded-lg">
                  <Wallet className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                </div>
                <p className="text-sm font-medium text-muted-foreground">
                  {t('dashboard.totalValue')}
                </p>
              </div>
            </div>
            <div className="mt-3">
              <div className="text-2xl font-bold">
                {loading ? '...' : formatCurrency(portfolio?.totalValue || 0)}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between space-y-0">
              <div className="flex items-center gap-2">
                <div className={`p-2 rounded-lg ${portfolio && portfolio.dayChange >= 0 ? 'bg-green-100 dark:bg-green-900/20' : 'bg-red-100 dark:bg-red-900/20'}`}>
                  {portfolio && portfolio.dayChange >= 0 ? (
                    <TrendingUp className="h-4 w-4 text-green-600 dark:text-green-400" />
                  ) : (
                    <TrendingDown className="h-4 w-4 text-red-600 dark:text-red-400" />
                  )}
                </div>
                <p className="text-sm font-medium text-muted-foreground">
                  {t('dashboard.dayChange')}
                </p>
              </div>
            </div>
            <div className="mt-3">
              <div className={`text-2xl font-bold ${getChangeColor(portfolio?.dayChange || 0)}`}>
                {loading ? '...' : formatPercentage(portfolio?.dayChangePercent || 0)}
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                {!loading && formatCurrency(portfolio?.dayChange || 0)}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between space-y-0">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-purple-100 dark:bg-purple-900/20 rounded-lg">
                  <DollarSign className="h-4 w-4 text-purple-600 dark:text-purple-400" />
                </div>
                <p className="text-sm font-medium text-muted-foreground">
                  Cash Available
                </p>
              </div>
            </div>
            <div className="mt-3">
              <div className="text-2xl font-bold">
                {loading ? '...' : formatCurrency(portfolio?.cash || 0)}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between space-y-0">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-green-100 dark:bg-green-900/20 rounded-lg">
                  <DollarSign className="h-4 w-4 text-green-600 dark:text-green-400" />
                </div>
                <p className="text-sm font-medium text-muted-foreground">
                  Weekly Dividends
                </p>
              </div>
            </div>
            <div className="mt-3">
              <div className="text-2xl font-bold">
                {loading ? '...' : formatCurrency(portfolio?.weeklyDividendIncome || 0)}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Charts Row */}
      <div className="grid gap-4 md:grid-cols-2">
        <PortfolioValueChart />
        <AllocationPieChart />
      </div>

      {/* Dividend Progress */}
      <DividendProgress />

      {/* Positions Table */}
      <PositionsTable />

      {/* Transactions Table */}
      <TransactionsTable />

      {/* Insights Row */}
      <div className="grid gap-4 md:grid-cols-2">
        <InsightsViewer />
        <PortfolioReportViewer />
      </div>
    </div>
  );
};
