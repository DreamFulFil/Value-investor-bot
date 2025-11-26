import ReactMarkdown from 'react-markdown';
import { usePortfolioReport } from '@/hooks/useInsights';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useTranslation } from 'react-i18next';
import { FileText } from 'lucide-react';

export const PortfolioReportViewer = () => {
  const { t } = useTranslation();
  const { report, loading, error } = usePortfolioReport();

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-blue-600" />
            {t('dashboard.portfolioReport')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">{t('common.loading')}</p>
        </CardContent>
      </Card>
    );
  }

  if (error || !report) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-blue-600" />
            {t('dashboard.portfolioReport')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">No report available yet.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <FileText className="h-5 w-5 text-blue-600" />
          {t('dashboard.portfolioReport')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="prose prose-sm dark:prose-invert max-w-none">
          <ReactMarkdown>{report}</ReactMarkdown>
        </div>
      </CardContent>
    </Card>
  );
};
