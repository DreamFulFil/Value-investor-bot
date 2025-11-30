import React, { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getTradingConfig, saveTradingConfig } from '../lib/api';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({ isOpen, onClose }) => {
  const queryClient = useQueryClient();
  const { data: config } = useQuery({
    queryKey: ['tradingConfig'],
    queryFn: getTradingConfig,
    enabled: isOpen, // Only fetch when the modal is open
  });

  const [telegramEnabled, setTelegramEnabled] = useState(false);
  const [botToken, setBotToken] = useState('');
  const [chatId, setChatId] = useState('');

  useEffect(() => {
    if (config) {
      setTelegramEnabled(config.telegramEnabled || false);
      setBotToken(config.telegramBotToken || '');
      setChatId(config.telegramChatId || '');
    }
  }, [config]);

  const mutation = useMutation({
    mutationFn: saveTradingConfig,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tradingConfig'] });
      queryClient.invalidateQueries({ queryKey: ['appConfig'] }); // Also invalidate appConfig as it might be related
      onClose();
    },
  });

  const handleSave = () => {
    mutation.mutate({
      ...config,
      telegramEnabled,
      telegramBotToken: botToken,
      telegramChatId: chatId,
    });
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-800 p-8 rounded-lg shadow-xl w-1/3">
        <h2 className="text-2xl font-bold mb-4 text-gray-900 dark:text-white">Settings</h2>
        <fieldset className="space-y-4">
          <legend className="text-lg font-medium text-gray-800 dark:text-gray-200">Telegram Notifications</legend>
          <div className="flex items-start">
            <div className="flex h-5 items-center">
              <input
                id="telegram-enabled"
                type="checkbox"
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                checked={telegramEnabled}
                onChange={(e) => setTelegramEnabled(e.target.checked)}
              />
            </div>
            <div className="ml-3 text-sm">
              <label htmlFor="telegram-enabled" className="font-medium text-gray-700 dark:text-gray-300">
                Enable Telegram Notifications
              </label>
              <p className="text-gray-500 dark:text-gray-400">Get notified after every rebalance.</p>
            </div>
          </div>
          {telegramEnabled && (
            <>
              <div>
                <label htmlFor="bot-token" className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Bot Token
                </label>
                <input
                  type="password"
                  id="bot-token"
                  value={botToken}
                  onChange={(e) => setBotToken(e.target.value)}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm sm:text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                />
              </div>
              <div>
                <label htmlFor="chat-id" className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                  Chat ID
                </label>
                <input
                  type="text"
                  id="chat-id"
                  value={chatId}
                  onChange={(e) => setChatId(e.target.value)}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm sm:text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                />
              </div>
            </>
          )}
        </fieldset>
        <div className="flex justify-end space-x-4 mt-8">
          <button onClick={onClose} className="px-4 py-2 bg-gray-200 dark:bg-gray-600 rounded">
            Cancel
          </button>
          <button onClick={handleSave} className="px-4 py-2 bg-blue-600 text-white rounded" disabled={mutation.isPending}>
            {mutation.isPending ? 'Saving...' : 'Save Settings'}
          </button>
        </div>
      </div>
    </div>
  );
};
