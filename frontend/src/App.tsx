import { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { DashboardOverview } from '@/components/dashboard/DashboardOverview';
import { BacktestLab } from '@/components/backtest/BacktestLab';
import { LearningTab } from '@/components/learning/LearningTab';
import { GoalsTab } from '@/components/goals/GoalsTab';
import { SettingsPage } from '@/components/settings/SettingsPage';
import { GoLiveWizard } from '@/components/wizard/GoLiveWizard';
import Confetti from 'react-confetti';
import { useWindowSize } from '@/hooks/useWindowSize';

function App() {
  const [showWizard, setShowWizard] = useState(false);
  const [showConfetti, setShowConfetti] = useState(false);
  const { width, height } = useWindowSize();

  useEffect(() => {
    // Check if wizard has been completed
    const wizardCompleted = localStorage.getItem('wizard_completed');
    if (!wizardCompleted) {
      setShowWizard(true);
    }

    // Listen for rebalance success events
    const handleRebalanceSuccess = () => {
      setShowConfetti(true);
      setTimeout(() => setShowConfetti(false), 5000);
    };

    window.addEventListener('rebalance-success', handleRebalanceSuccess);
    return () => window.removeEventListener('rebalance-success', handleRebalanceSuccess);
  }, []);

  const handleWizardComplete = () => {
    setShowWizard(false);
    setShowConfetti(true);
    setTimeout(() => setShowConfetti(false), 5000);
  };

  return (
    <>
      {showConfetti && (
        <Confetti
          width={width}
          height={height}
          recycle={false}
          numberOfPieces={500}
        />
      )}

      <Router>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            <Route index element={<DashboardOverview />} />
            <Route path="backtest" element={<BacktestLab />} />
            <Route path="learning" element={<LearningTab />} />
            <Route path="goals" element={<GoalsTab />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
        </Routes>
      </Router>

      <GoLiveWizard open={showWizard} onComplete={handleWizardComplete} />
    </>
  );
}

export default App;
