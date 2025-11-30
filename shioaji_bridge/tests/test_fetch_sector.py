import unittest
from unittest.mock import patch, MagicMock
import json
import io
import sys
import os

# Adjust the path to import the script from the parent directory
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from fetch_sector import get_sector, main

class TestFetchSector(unittest.TestCase):

    @patch('yfinance.Ticker')
    def test_get_sector_success_tw_stock(self, mock_ticker_cls):
        mock_ticker_instance = MagicMock()
        mock_ticker_instance.info = {'sector': 'Technology'}
        mock_ticker_cls.return_value = mock_ticker_instance

        with patch('sys.stdout', new_callable=io.StringIO) as mock_stdout:
            get_sector('2330') # TSMC
            output = json.loads(mock_stdout.getvalue().strip())
        
        mock_ticker_cls.assert_called_with('2330.TW')
        self.assertEqual(output['sector'], 'Technology')

    @patch('yfinance.Ticker')
    def test_get_sector_success_us_stock(self, mock_ticker_cls):
        mock_ticker_instance = MagicMock()
        mock_ticker_instance.info = {'sector': 'Consumer Cyclical'}
        mock_ticker_cls.return_value = mock_ticker_instance

        with patch('sys.stdout', new_callable=io.StringIO) as mock_stdout:
            get_sector('AMZN')
            output = json.loads(mock_stdout.getvalue().strip())
        
        mock_ticker_cls.assert_called_with('AMZN')
        self.assertEqual(output['sector'], 'Consumer Cyclical')

    @patch('yfinance.Ticker')
    def test_get_sector_not_available(self, mock_ticker_cls):
        # This mock will return no sector info for both .TW and .TWO suffixes
        mock_ticker_instance = MagicMock()
        mock_ticker_instance.info = {}
        mock_ticker_cls.return_value = mock_ticker_instance

        with patch('sys.stdout', new_callable=io.StringIO) as mock_stdout:
            get_sector('9999')
            output = json.loads(mock_stdout.getvalue().strip())

        self.assertIn('error', output)
        self.assertEqual(output['error'], 'Sector information not available.')

    def test_no_ticker_provided(self):
        with patch('sys.argv', ['fetch_sector.py']),
             patch('sys.stdout', new_callable=io.StringIO) as mock_stdout:
            main()
            output = json.loads(mock_stdout.getvalue().strip())

        self.assertEqual(output, {'error': 'No ticker symbol provided.'})

if __name__ == '__main__':
    unittest.main()
