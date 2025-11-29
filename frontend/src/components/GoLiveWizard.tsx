import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { activateLiveMode } from '../lib/api';

interface GoLiveWizardProps {
  isOpen: boolean;
  onClose: () => void;
  currentBacktestValue: number;
  onGoLive: (option: 'fresh' | 'gradual' | 'oneshot', amount: number) => void;
}

export function GoLiveWizard({ isOpen, onClose, currentBacktestValue, onGoLive }: GoLiveWizardProps) {
  const { i18n } = useTranslation();
  const [selectedOption, setSelectedOption] = useState<'fresh' | 'gradual' | 'oneshot' | null>(null);
  const [depositAmount, setDepositAmount] = useState(16000);
  const [catchUpMonths, setCatchUpMonths] = useState(12);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [confirmStep, setConfirmStep] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isZhTW = i18n.language === 'zh';

  if (!isOpen) return null;

  const options = [
    {
      id: 'fresh' as const,
      title: isZhTW ? '🆕 從零開始' : '🆕 Start Fresh',
      description: isZhTW 
        ? '存入 NT$16,000（或任意金額）從今天開始建立新的真實投資組合，忽略回測歷史。'
        : 'Deposit NT$16,000 (or any amount) and begin new real portfolio from today. Ignores backtest history.',
      recommended: isZhTW ? '推薦給：新手投資者' : 'Best for: New investors',
    },
    {
      id: 'gradual' as const,
      title: isZhTW ? '📈 逐步追趕' : '📈 Gradual Catch-Up',
      description: isZhTW
        ? '在 6-18 個月內逐步同步真實投資組合到回測狀態。每月額外存入資金直到匹配。'
        : 'Sync real portfolio to backtest gradually over 6-18 months. Deposit extra each month until matched.',
      recommended: isZhTW ? '推薦給：預算有限者' : 'Best for: Budget-conscious',
    },
    {
      id: 'oneshot' as const,
      title: isZhTW ? '💰 一次到位' : '💰 One-Shot Match',
      description: isZhTW
        ? `立即存入全額 NT$${currentBacktestValue.toLocaleString()} 以即時匹配當前回測投資組合價值。`
        : `Deposit full amount NT$${currentBacktestValue.toLocaleString()} today to instantly match backtest portfolio.`,
      recommended: isZhTW ? '推薦給：準備好全力投入者' : 'Best for: Ready to commit',
    },
  ];

  const handleConfirm = async () => {
    if (!selectedOption) return;
    
    if (!confirmStep) {
      setConfirmStep(true);
      return;
    }
    
    let amount = depositAmount;
    if (selectedOption === 'oneshot') {
      amount = currentBacktestValue;
    } else if (selectedOption === 'gradual') {
      amount = Math.ceil(currentBacktestValue / catchUpMonths);
    }
    
    setIsSubmitting(true);
    setError(null);
    
    try {
      const result = await activateLiveMode(selectedOption, amount);
      
      if (result.success) {
        onGoLive(selectedOption, amount);
        onClose();
        // Reload to reflect new LIVE mode
        window.location.reload();
      } else {
        setError(result.message);
      }
    } catch (e) {
      setError(isZhTW ? '啟用失敗，請稍後再試' : 'Failed to activate. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
              {isZhTW ? '🚀 開始真實投資' : '🚀 Go Live'}
            </h2>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <p className="mt-2 text-gray-600 dark:text-gray-400">
            {isZhTW 
              ? `當前回測投資組合價值：NT$${currentBacktestValue.toLocaleString()}`
              : `Current backtest portfolio value: NT$${currentBacktestValue.toLocaleString()}`}
          </p>
        </div>

        {/* Options */}
        <div className="p-6 space-y-4">
          {options.map((option) => (
            <div
              key={option.id}
              onClick={() => setSelectedOption(option.id)}
              className={`
                p-4 rounded-xl border-2 cursor-pointer transition-all
                ${selectedOption === option.id
                  ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                  : 'border-gray-200 dark:border-gray-700 hover:border-blue-300'
                }
              `}
            >
              <div className="flex items-start gap-3">
                <div className={`
                  w-5 h-5 mt-1 rounded-full border-2 flex items-center justify-center
                  ${selectedOption === option.id ? 'border-blue-500' : 'border-gray-400'}
                `}>
                  {selectedOption === option.id && (
                    <div className="w-3 h-3 rounded-full bg-blue-500" />
                  )}
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-gray-900 dark:text-white">{option.title}</h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{option.description}</p>
                  <p className="text-xs text-blue-600 dark:text-blue-400 mt-2">{option.recommended}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Configuration based on selection */}
        {selectedOption && (
          <div className="px-6 pb-4">
            {selectedOption === 'fresh' && (
              <div className="bg-gray-50 dark:bg-gray-900 p-4 rounded-xl">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {isZhTW ? '初始存款金額 (NT$)' : 'Initial Deposit Amount (NT$)'}
                </label>
                <input
                  type="range"
                  min="0"
                  max="160000"
                  step="1000"
                  value={depositAmount}
                  onChange={(e) => setDepositAmount(Number(e.target.value))}
                  className="w-full"
                />
                <div className="flex justify-between text-sm mt-2">
                  <span className="text-gray-500">NT$0</span>
                  <span className="font-bold text-blue-600">NT${depositAmount.toLocaleString()}</span>
                  <span className="text-gray-500">NT$160,000</span>
                </div>
                <p className="text-xs text-gray-500 mt-2">
                  {isZhTW ? '建議：NT$16,000 - NT$64,000' : 'Recommended: NT$16,000 - NT$64,000'}
                </p>
              </div>
            )}

            {selectedOption === 'gradual' && (
              <div className="bg-gray-50 dark:bg-gray-900 p-4 rounded-xl">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  {isZhTW ? '追趕期間（月數）' : 'Catch-up Period (months)'}
                </label>
                <input
                  type="range"
                  min="6"
                  max="18"
                  step="1"
                  value={catchUpMonths}
                  onChange={(e) => setCatchUpMonths(Number(e.target.value))}
                  className="w-full"
                />
                <div className="flex justify-between text-sm mt-2">
                  <span className="text-gray-500">6 {isZhTW ? '個月' : 'months'}</span>
                  <span className="font-bold text-blue-600">{catchUpMonths} {isZhTW ? '個月' : 'months'}</span>
                  <span className="text-gray-500">18 {isZhTW ? '個月' : 'months'}</span>
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-400 mt-3">
                  {isZhTW 
                    ? `每月額外存入：NT$${Math.ceil(currentBacktestValue / catchUpMonths).toLocaleString()}`
                    : `Monthly extra deposit: NT$${Math.ceil(currentBacktestValue / catchUpMonths).toLocaleString()}`}
                </p>
              </div>
            )}

            {selectedOption === 'oneshot' && (
              <div className="bg-yellow-50 dark:bg-yellow-900/20 p-4 rounded-xl border border-yellow-200 dark:border-yellow-800">
                <p className="text-sm text-yellow-800 dark:text-yellow-200">
                  ⚠️ {isZhTW 
                    ? `您將立即存入 NT$${currentBacktestValue.toLocaleString()}。請確認您有足夠的資金。`
                    : `You will deposit NT$${currentBacktestValue.toLocaleString()} immediately. Ensure you have sufficient funds.`}
                </p>
              </div>
            )}
            
            {/* Final confirmation warning */}
            {confirmStep && (
              <div className="bg-red-50 dark:bg-red-900/20 p-4 rounded-xl border-2 border-red-500">
                <p className="text-sm font-bold text-red-700 dark:text-red-300 mb-2">
                  🚨 {isZhTW ? '最終確認' : 'FINAL CONFIRMATION'}
                </p>
                <p className="text-sm text-red-600 dark:text-red-400">
                  {isZhTW 
                    ? '這是不可逆的操作！一旦啟用真實交易模式，將無法回到模擬模式。您的帳戶將在每月1日執行真實股票交易。'
                    : 'This is PERMANENT and cannot be undone! Once activated, LIVE mode cannot be reverted. Real stock orders will execute on the 1st of each month.'}
                </p>
              </div>
            )}
            
            {error && (
              <div className="bg-red-50 dark:bg-red-900/20 p-3 rounded-xl">
                <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
              </div>
            )}
          </div>
        )}

        {/* Footer */}
        <div className="p-6 border-t border-gray-200 dark:border-gray-700 flex gap-3">
          <button
            onClick={() => { setConfirmStep(false); onClose(); }}
            disabled={isSubmitting}
            className="flex-1 py-3 px-4 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-xl font-medium hover:bg-gray-200 dark:hover:bg-gray-600 disabled:opacity-50"
          >
            {isZhTW ? '取消' : 'Cancel'}
          </button>
          <button
            onClick={handleConfirm}
            disabled={!selectedOption || isSubmitting}
            className={`
              flex-1 py-3 px-4 rounded-xl font-medium transition-all
              ${selectedOption && !isSubmitting
                ? confirmStep 
                  ? 'bg-red-500 text-white hover:bg-red-600 animate-pulse'
                  : 'bg-green-500 text-white hover:bg-green-600'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              }
            `}
          >
            {isSubmitting 
              ? (isZhTW ? '處理中...' : 'Processing...')
              : confirmStep 
                ? (isZhTW ? '🔴 確認啟用真實交易' : '🔴 CONFIRM LIVE TRADING')
                : (isZhTW ? '確認開始' : 'Confirm & Go Live')
            }
          </button>
        </div>
      </div>
    </div>
  );
}
