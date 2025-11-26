import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { formatCurrency } from '@/lib/formatters';
import { useDeposit } from '@/hooks/usePortfolio';
import { WizardMode } from './GoLiveWizard';

interface WizardStep3Props {
  mode: WizardMode;
  initialDeposit: number;
  onConfirm: () => void;
  onBack: () => void;
}

export const WizardStep3 = ({
  mode,
  initialDeposit,
  onConfirm,
  onBack,
}: WizardStep3Props) => {
  const { t } = useTranslation();
  const { deposit, loading } = useDeposit();
  const [error, setError] = useState<string | null>(null);

  const handleConfirm = async () => {
    try {
      setError(null);

      // Only deposit if starting from zero
      if (mode === 'zero' && initialDeposit > 0) {
        const success = await deposit(initialDeposit);
        if (!success) {
          setError('Failed to deposit funds. Please try again.');
          return;
        }
      }

      onConfirm();
    } catch (err) {
      setError('An error occurred. Please try again.');
    }
  };

  const getModeLabel = () => {
    switch (mode) {
      case 'zero':
        return 'Live Trading';
      case 'import':
        return 'Live Trading';
      case 'backtest':
        return 'Simulation Mode';
      default:
        return 'Unknown';
    }
  };

  const getModeColor = () => {
    return mode === 'backtest' ? 'secondary' : 'default';
  };

  return (
    <div className="space-y-6">
      <div className="text-center">
        <div className="mx-auto w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/20 flex items-center justify-center mb-4">
          <CheckCircle2 className="h-8 w-8 text-green-600 dark:text-green-400" />
        </div>
        <h3 className="text-lg font-semibold mb-2">{t('wizard.step3.title')}</h3>
        <p className="text-sm text-muted-foreground">
          {t('wizard.step3.subtitle')}
        </p>
      </div>

      <div className="space-y-4 rounded-lg border p-6">
        <div className="flex justify-between items-center">
          <span className="text-sm text-muted-foreground">
            {t('wizard.step3.mode')}
          </span>
          <Badge variant={getModeColor()}>{getModeLabel()}</Badge>
        </div>

        {mode !== 'import' && (
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">
              {t('wizard.step3.initialDeposit')}
            </span>
            <span className="font-semibold">{formatCurrency(initialDeposit)}</span>
          </div>
        )}

        <div className="pt-4 border-t space-y-2">
          <h4 className="font-medium text-sm">What happens next?</h4>
          <ul className="space-y-2 text-sm text-muted-foreground">
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
              <span>Your portfolio will be initialized</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
              <span>AI will analyze top dividend stocks</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
              <span>Recommended positions will be suggested</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
              <span>Monthly rebalancing will maintain optimal allocation</span>
            </li>
          </ul>
        </div>
      </div>

      {error && (
        <div className="rounded-lg bg-destructive/10 border border-destructive/20 p-4">
          <p className="text-sm text-destructive">{error}</p>
        </div>
      )}

      <div className="flex gap-2">
        <Button
          variant="outline"
          onClick={onBack}
          className="flex-1"
          disabled={loading}
        >
          {t('wizard.back')}
        </Button>
        <Button
          onClick={handleConfirm}
          className="flex-1"
          disabled={loading}
        >
          {loading ? 'Setting up...' : t('wizard.step3.confirm')}
        </Button>
      </div>
    </div>
  );
};
