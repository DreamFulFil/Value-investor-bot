import { usePortfolio } from '@/hooks/usePortfolio';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { formatCurrency, formatPercent, formatShares, getChangeColor } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';
import { TrendingUp, TrendingDown } from 'lucide-react';

export const PositionsTable = () => {
  const { t } = useTranslation();
  const { portfolio, loading } = usePortfolio();

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.positions')}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground text-center py-8">{t('common.loading')}</p>
        </CardContent>
      </Card>
    );
  }

  if (!portfolio || portfolio.positions.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.positions')}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground text-center py-8">{t('dashboard.noPositions')}</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('dashboard.positions')}</CardTitle>
        <CardDescription>{portfolio.positions.length} active positions</CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('positions.symbol')}</TableHead>
              <TableHead className="text-right">{t('positions.shares')}</TableHead>
              <TableHead className="text-right">{t('positions.avgCost')}</TableHead>
              <TableHead className="text-right">{t('positions.currentPrice')}</TableHead>
              <TableHead className="text-right">{t('positions.totalValue')}</TableHead>
              <TableHead className="text-right">{t('positions.gainLoss')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {portfolio.positions.map((position) => (
              <TableRow key={position.symbol}>
                <TableCell className="font-semibold">
                  <div>
                    {position.symbol}
                    <div className="text-xs text-muted-foreground">
                      {formatPercent(position.dividendYield)} yield
                    </div>
                  </div>
                </TableCell>
                <TableCell className="text-right">{formatShares(position.shares)}</TableCell>
                <TableCell className="text-right">{formatCurrency(position.avgCost)}</TableCell>
                <TableCell className="text-right">{formatCurrency(position.currentPrice)}</TableCell>
                <TableCell className="text-right font-semibold">
                  {formatCurrency(position.totalValue)}
                </TableCell>
                <TableCell className="text-right">
                  <div className={getChangeColor(position.gainLoss)}>
                    <div className="flex items-center justify-end gap-1">
                      {position.gainLoss >= 0 ? (
                        <TrendingUp className="h-4 w-4" />
                      ) : (
                        <TrendingDown className="h-4 w-4" />
                      )}
                      <span>{formatCurrency(Math.abs(position.gainLoss))}</span>
                    </div>
                    <div className="text-xs">
                      {position.gainLossPercent >= 0 ? '+' : ''}
                      {formatPercent(position.gainLossPercent)}
                    </div>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
};
