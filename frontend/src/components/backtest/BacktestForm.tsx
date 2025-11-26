import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { formatCurrency } from '@/lib/formatters';
import { Calendar } from 'lucide-react';

interface BacktestFormProps {
  onRun: (params: BacktestParams) => void;
  loading: boolean;
}

export interface BacktestParams {
  startDate: string;
  endDate: string;
  monthlyInvestment: number;
  symbols: string[];
}

export const BacktestForm = ({ onRun, loading }: BacktestFormProps) => {
  const { t } = useTranslation();

  const [startDate, setStartDate] = useState(() => {
    const date = new Date();
    date.setFullYear(date.getFullYear() - 5);
    return date.toISOString().split('T')[0];
  });

  const [endDate, setEndDate] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });

  const [monthlyInvestment, setMonthlyInvestment] = useState(16000);

  const [selectedStocks] = useState<string[]>([
    '2330.TW', '2317.TW', '2881.TW', '2882.TW', '2412.TW'
  ]);

  const handleRun = () => {
    onRun({
      startDate,
      endDate,
      monthlyInvestment,
      symbols: selectedStocks,
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('backtest.title')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="startDate" className="flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              {t('backtest.startDate')}
            </Label>
            <input
              id="startDate"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="endDate" className="flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              {t('backtest.endDate')}
            </Label>
            <input
              id="endDate"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex justify-between">
            <Label>{t('backtest.monthlyInvestment')}</Label>
            <span className="text-lg font-semibold text-primary">
              {formatCurrency(monthlyInvestment)}
            </span>
          </div>
          <Slider
            value={[monthlyInvestment]}
            onValueChange={(value: number[]) => setMonthlyInvestment(value[0])}
            min={4000}
            max={64000}
            step={1000}
            className="w-full"
          />
          <div className="flex justify-between text-sm text-muted-foreground">
            <span>NT$4,000</span>
            <span>NT$64,000</span>
          </div>
        </div>

        <div className="space-y-2">
          <Label>{t('backtest.targetStocks')}</Label>
          <div className="rounded-lg border p-4 bg-muted/50">
            <div className="flex flex-wrap gap-2">
              {selectedStocks.map((symbol) => (
                <div
                  key={symbol}
                  className="px-3 py-1 bg-primary text-primary-foreground rounded-full text-sm font-medium"
                >
                  {symbol}
                </div>
              ))}
            </div>
            <p className="text-xs text-muted-foreground mt-2">
              Using top dividend stocks from our universe
            </p>
          </div>
        </div>

        <Button
          onClick={handleRun}
          disabled={loading}
          className="w-full"
          size="lg"
        >
          {loading ? 'Running Backtest...' : t('backtest.runBacktest')}
        </Button>
      </CardContent>
    </Card>
  );
};
