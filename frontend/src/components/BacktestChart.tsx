import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { getBacktestChartData } from '../lib/api';
import { StatCard } from './StatCard';

export const BacktestChart: React.FC = () => {
  const { data, error, isLoading } = useQuery({
    queryKey: ['backtestChart'],
    queryFn: getBacktestChartData,
  });

  if (isLoading) {
    return <StatCard title="Backtest" value="Loading chart..." />;
  }

  if (error || !data) {
    return <StatCard title="Backtest" value={error ? error.message : 'No data available.'} className="text-red-500" />;
  }

  const formattedData = data.dataPoints.map(p => ({
      ...p,
      date: new Date(p.date).toLocaleDateString(),
  }));

  return (
    <div className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow-md h-96">
      <h2 className="text-lg font-semibold mb-4 text-gray-800 dark:text-gray-200">Portfolio Backtest (1 Year)</h2>
      <ResponsiveContainer width="100%" height="85%">
        <LineChart
          data={formattedData}
          margin={{
            top: 5,
            right: 30,
            left: 20,
            bottom: 5,
          }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(128, 128, 128, 0.3)" />
          <XAxis dataKey="date" stroke="rgb(156 163 175)" />
          <YAxis stroke="rgb(156 163 175)"/>
          <Tooltip />
          <Legend />
          <Line type="monotone" dataKey="value" stroke="#8884d8" activeDot={{ r: 8 }} name="Portfolio Value" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};
