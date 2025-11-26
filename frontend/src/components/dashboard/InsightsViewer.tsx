import ReactMarkdown from 'react-markdown';
import { useInsights } from '@/hooks/useInsights';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useTranslation } from 'react-i18next';
import { Lightbulb } from 'lucide-react';

export const InsightsViewer = () => {
  const { t } = useTranslation();
  const { insights, loading, error } = useInsights();

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Lightbulb className="h-5 w-5 text-yellow-600" />
            {t('dashboard.insights')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">{t('common.loading')}</p>
        </CardContent>
      </Card>
    );
  }

  if (error || !insights) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Lightbulb className="h-5 w-5 text-yellow-600" />
            {t('dashboard.insights')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">No insights available yet.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Lightbulb className="h-5 w-5 text-yellow-600" />
          {t('dashboard.insights')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="prose prose-sm dark:prose-invert max-w-none">
          <ReactMarkdown>{insights}</ReactMarkdown>
        </div>
      </CardContent>
    </Card>
  );
};
