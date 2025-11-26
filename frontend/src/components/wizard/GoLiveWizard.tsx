import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { WizardStep1 } from './WizardStep1';
import { WizardStep2 } from './WizardStep2';
import { WizardStep3 } from './WizardStep3';

export type WizardMode = 'zero' | 'import' | 'backtest';

interface WizardData {
  mode: WizardMode;
  initialDeposit: number;
}

interface GoLiveWizardProps {
  open: boolean;
  onComplete: () => void;
}

export const GoLiveWizard = ({ open, onComplete }: GoLiveWizardProps) => {
  const { t } = useTranslation();
  const [step, setStep] = useState(1);
  const [data, setData] = useState<WizardData>({
    mode: 'zero',
    initialDeposit: 1000,
  });

  const handleNext = () => {
    if (step < 3) {
      setStep(step + 1);
    }
  };

  const handleBack = () => {
    if (step > 1) {
      setStep(step - 1);
    }
  };

  const handleModeSelect = (mode: WizardMode) => {
    setData({ ...data, mode });
    handleNext();
  };

  const handleDepositChange = (amount: number) => {
    setData({ ...data, initialDeposit: amount });
  };

  const handleComplete = async () => {
    // Mark wizard as completed
    localStorage.setItem('wizard_completed', 'true');

    // Set trading mode
    if (data.mode === 'backtest') {
      localStorage.setItem('trading_mode', 'SIMULATION');
    } else {
      localStorage.setItem('trading_mode', 'LIVE');
    }

    onComplete();
  };

  return (
    <Dialog open={open}>
      <DialogContent className="sm:max-w-[600px]">
        <div className="space-y-6">
          <div className="text-center">
            <div className="mx-auto w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center mb-4">
              <span className="text-white font-bold text-2xl">VB</span>
            </div>
            <h2 className="text-2xl font-bold">{t('wizard.title')}</h2>
            <p className="text-muted-foreground">{t('wizard.subtitle')}</p>
          </div>

          <div className="flex justify-center gap-2 mb-6">
            {[1, 2, 3].map((s) => (
              <div
                key={s}
                className={`h-2 w-16 rounded-full ${
                  s <= step ? 'bg-primary' : 'bg-muted'
                }`}
              />
            ))}
          </div>

          {step === 1 && <WizardStep1 onSelect={handleModeSelect} />}

          {step === 2 && (
            <WizardStep2
              mode={data.mode}
              initialDeposit={data.initialDeposit}
              onDepositChange={handleDepositChange}
              onNext={handleNext}
              onBack={handleBack}
            />
          )}

          {step === 3 && (
            <WizardStep3
              mode={data.mode}
              initialDeposit={data.initialDeposit}
              onConfirm={handleComplete}
              onBack={handleBack}
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
};
