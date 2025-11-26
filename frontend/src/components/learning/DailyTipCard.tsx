import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Heart, Sparkles } from 'lucide-react';
import { DailyTip } from '@/hooks/useInsights';
import { formatDate } from '@/lib/formatters';
import { useTranslation } from 'react-i18next';

interface DailyTipCardProps {
  tip: DailyTip;
  onLike: (id: number) => void;
}

export const DailyTipCard = ({ tip, onLike }: DailyTipCardProps) => {
  const { t } = useTranslation();
  const [liked, setLiked] = useState(tip.liked);

  const handleLike = () => {
    if (!liked) {
      onLike(tip.id);
      setLiked(true);
    }
  };

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

  return (
    <Card className="overflow-hidden border-2 border-primary/20">
      <div className="h-2 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500" />
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <Sparkles className="h-5 w-5 text-yellow-500" />
              <CardTitle className="text-xl">{tip.title}</CardTitle>
            </div>
            <CardDescription>{formatDate(tip.date)}</CardDescription>
          </div>
          <Badge className={getCategoryColor(tip.category)}>
            {tip.category}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-muted-foreground leading-relaxed">{tip.content}</p>

        <div className="flex items-center justify-between pt-4 border-t">
          <Button
            variant={liked ? 'default' : 'outline'}
            size="sm"
            onClick={handleLike}
            disabled={liked}
            className="gap-2"
          >
            <Heart className={`h-4 w-4 ${liked ? 'fill-current' : ''}`} />
            {liked ? t('learning.liked') : 'Like'}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};
