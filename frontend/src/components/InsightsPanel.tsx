import { useTranslation } from 'react-i18next';
import ReactMarkdown from 'react-markdown';
import type { Insight } from '../lib/api';

interface InsightsPanelProps {
  insights: Insight[];
  isEmpty?: boolean;
}

export function InsightsPanel({ insights, isEmpty }: InsightsPanelProps) {
  const { t } = useTranslation();

  const latestInsight = insights[0];

  return (
    <div className="card relative">
      {isEmpty && (
        <div className="absolute inset-0 bg-gray-900/60 dark:bg-gray-900/80 rounded-xl flex items-center justify-center z-10 backdrop-blur-sm">
          <p className="text-white text-center px-4 text-sm font-medium">
            {t('clickBlueButton')}
          </p>
        </div>
      )}
      
      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
        <span className="text-2xl">🤖</span>
        {t('latestInsights')}
      </h3>
      
      {latestInsight ? (
        <div className="prose prose-sm dark:prose-invert max-w-none">
          <ReactMarkdown
            components={{
              h1: ({ children }) => <h1 className="text-xl font-bold mb-2">{children}</h1>,
              h2: ({ children }) => <h2 className="text-lg font-semibold mb-2">{children}</h2>,
              h3: ({ children }) => <h3 className="text-base font-medium mb-1">{children}</h3>,
              p: ({ children }) => <p className="mb-2 text-gray-700 dark:text-gray-300">{children}</p>,
              ul: ({ children }) => <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>,
              li: ({ children }) => <li className="text-gray-700 dark:text-gray-300">{children}</li>,
              strong: ({ children }) => <strong className="font-semibold text-gray-900 dark:text-white">{children}</strong>,
            }}
          >
            {latestInsight.content}
          </ReactMarkdown>
          <p className="text-xs text-gray-400 mt-4">
            {new Date(latestInsight.createdAt).toLocaleString()}
          </p>
        </div>
      ) : (
        <p className="text-gray-500 dark:text-gray-400 text-center py-8">
          {t('noInsights')}
        </p>
      )}
    </div>
  );
}
