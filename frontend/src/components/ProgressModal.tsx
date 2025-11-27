import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createProgressEventSource, type ProgressEvent } from '../lib/api';

interface ProgressModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function ProgressModal({ isOpen, onClose }: ProgressModalProps) {
  const { t } = useTranslation();
  const [progress, setProgress] = useState<ProgressEvent | null>(null);
  const [logs, setLogs] = useState<string[]>([]);

  useEffect(() => {
    if (!isOpen) {
      setProgress(null);
      setLogs([]);
      return;
    }

    const eventSource = createProgressEventSource();

    eventSource.addEventListener('progress', (event) => {
      try {
        const data = JSON.parse(event.data) as ProgressEvent;
        setProgress(data);
        setLogs(prev => [...prev, data.message]);

        if (data.type === 'complete' || data.type === 'error') {
          setTimeout(() => {
            eventSource.close();
          }, 1000);
        }
      } catch (e) {
        console.error('Failed to parse progress event:', e);
      }
    });

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const isComplete = progress?.type === 'complete';
  const isError = progress?.type === 'error';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-lg mx-4 bg-slate-800 rounded-2xl shadow-2xl border border-slate-700 overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 bg-gradient-to-r from-blue-600 to-blue-700">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            {isComplete ? '✅' : isError ? '❌' : '🚀'} 
            {t('rebalancing')}
          </h2>
        </div>

        {/* Progress bar */}
        <div className="px-6 pt-4">
          <div className="h-3 bg-slate-700 rounded-full overflow-hidden">
            <div 
              className={`h-full transition-all duration-500 ease-out ${
                isError ? 'bg-red-500' : isComplete ? 'bg-green-500' : 'bg-blue-500'
              }`}
              style={{ width: `${progress?.percentage || 0}%` }}
            />
          </div>
          <div className="mt-2 text-right text-sm text-slate-400">
            {progress?.percentage || 0}%
          </div>
        </div>

        {/* Current status */}
        <div className="px-6 py-4">
          <div className="flex items-center gap-3">
            {!isComplete && !isError && (
              <div className="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
            )}
            <span className="text-white font-medium">
              {progress?.message || t('preparingRebalance')}
            </span>
          </div>
        </div>

        {/* Log output */}
        <div className="px-6 pb-4">
          <div className="h-40 bg-slate-900 rounded-lg p-3 overflow-y-auto font-mono text-xs">
            {logs.map((log, i) => (
              <div key={i} className="text-slate-400 py-0.5">
                <span className="text-slate-600">[{String(i + 1).padStart(2, '0')}]</span> {log}
              </div>
            ))}
            {logs.length === 0 && (
              <div className="text-slate-600">{t('waitingForProgress')}</div>
            )}
          </div>
        </div>

        {/* Footer */}
        {(isComplete || isError) && (
          <div className="px-6 pb-6">
            <button
              onClick={onClose}
              className={`w-full py-3 rounded-lg font-semibold transition-colors ${
                isError 
                  ? 'bg-red-600 hover:bg-red-700 text-white' 
                  : 'bg-green-600 hover:bg-green-700 text-white'
              }`}
            >
              {isError ? t('tryAgain') : t('done')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
