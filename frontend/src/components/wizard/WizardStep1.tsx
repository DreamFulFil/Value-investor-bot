import { useTranslation } from 'react-i18next';
import { DollarSign, Upload, FlaskConical } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { WizardMode } from './GoLiveWizard';

interface WizardStep1Props {
  onSelect: (mode: WizardMode) => void;
}

export const WizardStep1 = ({ onSelect }: WizardStep1Props) => {
  const { t } = useTranslation();

  const options = [
    {
      mode: 'zero' as WizardMode,
      icon: DollarSign,
      title: t('wizard.step1.startFromZero'),
      description: t('wizard.step1.startFromZeroDesc'),
      color: 'from-green-500 to-emerald-600',
    },
    {
      mode: 'import' as WizardMode,
      icon: Upload,
      title: t('wizard.step1.importExisting'),
      description: t('wizard.step1.importExistingDesc'),
      color: 'from-blue-500 to-cyan-600',
    },
    {
      mode: 'backtest' as WizardMode,
      icon: FlaskConical,
      title: t('wizard.step1.backtestOnly'),
      description: t('wizard.step1.backtestOnlyDesc'),
      color: 'from-purple-500 to-pink-600',
    },
  ];

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold text-center">
        {t('wizard.step1.title')}
      </h3>

      <div className="grid gap-4">
        {options.map((option) => (
          <Card
            key={option.mode}
            className="cursor-pointer transition-all hover:shadow-lg hover:scale-[1.02]"
            onClick={() => onSelect(option.mode)}
          >
            <CardContent className="flex items-start gap-4 p-6">
              <div
                className={`w-12 h-12 rounded-lg bg-gradient-to-br ${option.color} flex items-center justify-center flex-shrink-0`}
              >
                <option.icon className="h-6 w-6 text-white" />
              </div>
              <div className="flex-1">
                <h4 className="font-semibold mb-1">{option.title}</h4>
                <p className="text-sm text-muted-foreground">
                  {option.description}
                </p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};
