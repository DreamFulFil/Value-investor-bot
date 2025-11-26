import { useTransactions } from '@/hooks/useTransactions';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { formatCurrency, formatShares, formatDate } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';

export const TransactionsTable = () => {
  const { t } = useTranslation();
  const { transactions, loading } = useTransactions(10);

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.recentTransactions')}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground text-center py-8">{t('common.loading')}</p>
        </CardContent>
      </Card>
    );
  }

  if (!transactions || transactions.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{t('dashboard.recentTransactions')}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground text-center py-8">{t('dashboard.noTransactions')}</p>
        </CardContent>
      </Card>
    );
  }

  const getTypeBadge = (type: string) => {
    switch (type) {
      case 'BUY':
        return <Badge variant="default">Buy</Badge>;
      case 'SELL':
        return <Badge variant="secondary">Sell</Badge>;
      case 'DIVIDEND':
        return <Badge variant="outline" className="border-green-500 text-green-600">Dividend</Badge>;
      default:
        return <Badge variant="outline">{type}</Badge>;
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('dashboard.recentTransactions')}</CardTitle>
        <CardDescription>Last 10 transactions</CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('transactions.date')}</TableHead>
              <TableHead>{t('transactions.type')}</TableHead>
              <TableHead>{t('transactions.symbol')}</TableHead>
              <TableHead className="text-right">{t('transactions.shares')}</TableHead>
              <TableHead className="text-right">{t('transactions.price')}</TableHead>
              <TableHead className="text-right">{t('transactions.total')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {transactions.map((tx) => (
              <TableRow key={tx.id}>
                <TableCell className="text-sm text-muted-foreground">
                  {formatDate(tx.date, 'MMM dd')}
                </TableCell>
                <TableCell>{getTypeBadge(tx.type)}</TableCell>
                <TableCell className="font-semibold">{tx.symbol}</TableCell>
                <TableCell className="text-right">{formatShares(tx.shares)}</TableCell>
                <TableCell className="text-right">{formatCurrency(tx.price)}</TableCell>
                <TableCell className="text-right font-semibold">
                  {formatCurrency(tx.total)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
};
