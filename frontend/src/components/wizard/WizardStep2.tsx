import { useTranslation } from 'react-i18next';
import { Slider } from '@/components/ui/slider';
import { Button } from '@/components/ui/button';
import { formatCurrency } from '@/lib/formatters';
import { WizardMode } from './GoLiveWizard';

interface WizardStep2Props {
  mode: WizardMode;
  initialDeposit: number;
  onDepositChange: (amount: number) => void;
  onNext: () => void;
  onBack: () => void;
}

export const WizardStep2 = ({
  mode,
  initialDeposit,
  onDepositChange,
  onNext,
  onBack,
}: WizardStep2Props) => {
  const { t } = useTranslation();

  // Skip deposit for import mode
  if (mode === 'import') {
    return (
      <div className="space-y-6">
        <div className="text-center">
          <h3 className="text-lg font-semibold mb-2">
            Import Your Portfolio
          </h3>
          <p className="text-sm text-muted-foreground">
            This feature is coming soon. For now, please start from zero or use backtest mode.
          </p>
        </div>

        <div className="flex gap-2">
          <Button variant="outline" onClick={onBack} className="flex-1">
            {t('wizard.back')}
          </Button>
          <Button onClick={onNext} className="flex-1">
            {t('wizard.next')}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h3 className="text-lg font-semibold mb-2">
          {t('wizard.step2.title')}
        </h3>
        <p className="text-sm text-muted-foreground">
          {t('wizard.step2.subtitle')}
        </p>
      </div>

      <div className="space-y-4">
        <div className="text-center">
          <div className="text-4xl font-bold mb-2 bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
            {formatCurrency(initialDeposit)}
          </div>
          <p className="text-sm text-muted-foreground">
            {t('wizard.step2.amount')}
          </p>
        </div>

        <Slider
          value={[initialDeposit]}
          onValueChange={(value: number[]) => onDepositChange(value[0])}
          min={0}
          max={160000}
          step={1000}
          className="w-full"
        />

        <div className="flex justify-between text-sm text-muted-foreground">
          <span>NT$0</span>
          <span>NT$160,000</span>
        </div>

        <div className="rounded-lg bg-muted p-4 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Recommended start:</span>
            <span className="font-medium">NT$16,000 - NT$64,000</span>
          </div>
          <p className="text-xs text-muted-foreground">
            Starting with at least NT$16,000 allows for better diversification across 5+ stocks.
          </p>
        </div>
      </div>

      <div className="flex gap-2">
        <Button variant="outline" onClick={onBack} className="flex-1">
          {t('wizard.back')}
        </Button>
        <Button onClick={onNext} className="flex-1">
          {t('wizard.next')}
        </Button>
      </div>
    </div>
  );
};
