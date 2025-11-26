import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Heart } from 'lucide-react';
import { DailyTip } from '@/hooks/useInsights';
import { formatDate } from '@/lib/formatters';

interface TipHistoryProps {
  tips: DailyTip[];
}

export const TipHistory = ({ tips }: TipHistoryProps) => {
  const getCategoryColor = (category: string) => {
    const colors: Record<string, string> = {
      dividends: 'bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400',
      valuation: 'bg-blue-100 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400',
      risk: 'bg-red-100 text-red-700 dark:bg-red-900/20 dark:text-red-400',
      diversification: 'bg-purple-100 text-purple-700 dark:bg-purple-900/20 dark:text-purple-400',
      strategy: 'bg-orange-100 text-orange-700 dark:bg-orange-900/20 dark:text-orange-400',
    };
    return colors[category.toLowerCase()] || 'bg-gray-100 text-gray-700 dark:bg-gray-900/20 dark:text-gray-400';
  };

  if (tips.length === 0) {
    return (
      <Card>
        <CardContent className="py-8">
          <p className="text-muted-foreground text-center">No learning tips yet.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {tips.map((tip) => (
        <Card key={tip.id} className="hover:shadow-md transition-shadow">
          <CardContent className="p-4">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 space-y-2">
                <div className="flex items-center gap-2">
                  <h4 className="font-semibold">{tip.title}</h4>
                  {tip.liked && (
                    <Heart className="h-4 w-4 text-red-500 fill-current" />
                  )}
                </div>
                <p className="text-sm text-muted-foreground line-clamp-2">
                  {tip.content}
                </p>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Badge className={getCategoryColor(tip.category)} variant="outline">
                    {tip.category}
                  </Badge>
                  <span>{formatDate(tip.date)}</span>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};
