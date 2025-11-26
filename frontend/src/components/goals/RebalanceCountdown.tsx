import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useTranslation } from 'react-i18next';
import { Calendar, Clock } from 'lucide-react';
import { formatDate } from '@/lib/formatters';

export const RebalanceCountdown = () => {
  const { t } = useTranslation();

  // Calculate next rebalance date (first day of next month)
  const getNextRebalanceDate = () => {
    const now = new Date();
    const next = new Date(now.getFullYear(), now.getMonth() + 1, 1);
    return next;
  };

  const nextRebalance = getNextRebalanceDate();
  const daysUntil = Math.ceil(
    (nextRebalance.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24)
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Calendar className="h-5 w-5 text-blue-600" />
          {t('goals.nextRebalance')}
        </CardTitle>
        <CardDescription>Automatic portfolio optimization</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="text-center space-y-2">
          <div className="text-5xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
            {daysUntil}
          </div>
          <p className="text-sm text-muted-foreground">
            days until next rebalance
          </p>
        </div>

        <div className="rounded-lg border p-4 bg-muted/50">
          <div className="flex items-center gap-2 mb-2">
            <Clock className="h-4 w-4 text-muted-foreground" />
            <p className="text-xs font-medium text-muted-foreground">Scheduled Date</p>
          </div>
          <p className="text-lg font-semibold">{formatDate(nextRebalance, 'MMMM dd, yyyy')}</p>
        </div>

        <div className="space-y-2">
          <p className="text-sm font-medium">What happens during rebalance?</p>
          <ul className="space-y-1 text-sm text-muted-foreground">
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>Portfolio analyzed for optimal allocation</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>Underperforming stocks may be sold</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>New dividend opportunities added</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-primary">•</span>
              <span>Maintain target 5-stock allocation</span>
            </li>
          </ul>
        </div>
      </CardContent>
    </Card>
  );
};
