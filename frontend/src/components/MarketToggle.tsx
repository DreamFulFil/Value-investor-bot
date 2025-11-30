import React from 'react';

export const MarketToggle = () => {
  const [market, setMarket] = React.useState('TW');

  const handleToggle = () => {
    const newMarket = market === 'TW' ? 'US' : 'TW';
    setMarket(newMarket);
    if (newMarket === 'US') {
      alert(`Market switched to US.\n\nNote: US Market access is a premium feature ($500/month). Data will be sourced from Yahoo Finance.`);
    } else {
      alert(`Market switched to TW.`);
    }
    // In a real app, this would trigger a global state change and data refetch for the entire dashboard.
    // A simple way to simulate this is to reload the page.
    // window.location.reload(); 
  };

  return (
    <div className="flex items-center space-x-2">
      <span className={`font-bold ${market === 'TW' ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500'}`}>TW</span>
      <button onClick={handleToggle} className="relative inline-flex h-6 w-11 items-center rounded-full bg-gray-300 dark:bg-gray-600">
        <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition ${market === 'TW' ? 'translate-x-1' : 'translate-x-6'}`} />
      </button>
      <span className={`font-bold ${market === 'US' ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500'}`}>US</span>
    </div>
  );
};
