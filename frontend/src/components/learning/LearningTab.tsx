import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useDailyTip, useLearningHistory } from '@/hooks/useInsights';
import { DailyTipCard } from './DailyTipCard';
import { TipHistory } from './TipHistory';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

export const LearningTab = () => {
  const { t } = useTranslation();
  const { tip, loading: tipLoading, likeTip } = useDailyTip();
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const { tips, loading: historyLoading } = useLearningHistory(
    selectedCategory === 'all' ? undefined : selectedCategory
  );

  const categories = [
    { value: 'all', label: t('learning.categories.all') },
    { value: 'dividends', label: t('learning.categories.dividends') },
    { value: 'valuation', label: t('learning.categories.valuation') },
    { value: 'risk', label: t('learning.categories.risk') },
    { value: 'diversification', label: t('learning.categories.diversification') },
    { value: 'strategy', label: t('learning.categories.strategy') },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">{t('learning.title')}</h2>
        <p className="text-muted-foreground">{t('learning.subtitle')}</p>
      </div>

      {/* Daily Tip */}
      <div>
        <h3 className="text-xl font-semibold mb-4">{t('learning.dailyTip')}</h3>
        {tipLoading ? (
          <Card>
            <CardContent className="py-8">
              <p className="text-muted-foreground text-center">{t('common.loading')}</p>
            </CardContent>
          </Card>
        ) : tip ? (
          <DailyTipCard tip={tip} onLike={likeTip} />
        ) : (
          <Card>
            <CardContent className="py-8">
              <p className="text-muted-foreground text-center">No daily tip available.</p>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Recent Tips with Category Filter */}
      <div>
        <h3 className="text-xl font-semibold mb-4">{t('learning.recentTips')}</h3>

        <Tabs value={selectedCategory} onValueChange={setSelectedCategory}>
          <TabsList className="grid w-full grid-cols-3 lg:grid-cols-6">
            {categories.map((category) => (
              <TabsTrigger key={category.value} value={category.value}>
                {category.label}
              </TabsTrigger>
            ))}
          </TabsList>

          <TabsContent value={selectedCategory} className="mt-6">
            {historyLoading ? (
              <Card>
                <CardContent className="py-8">
                  <p className="text-muted-foreground text-center">{t('common.loading')}</p>
                </CardContent>
              </Card>
            ) : (
              <TipHistory tips={tips} />
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};
