#!/usr/bin/env python3
"""Test script to explore Shioaji contracts"""
import logging
from config import Config
from shioaji_client import ShioajiClient

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize and login
config = Config()
client = ShioajiClient(config)

success, message = client.login()
if not success:
    print(f"Login failed: {message}")
    exit(1)

print("Login successful!")
print(f"API object: {client.api}")
print(f"Has Contracts: {hasattr(client.api, 'Contracts')}")

if hasattr(client.api, 'Contracts'):
    print(f"Contracts: {client.api.Contracts}")
    print(f"Has Stocks: {hasattr(client.api.Contracts, 'Stocks')}")

    if hasattr(client.api.Contracts, 'Stocks'):
        stocks = client.api.Contracts.Stocks
        print(f"Stocks: {stocks}")
        print(f"Stocks attributes: {dir(stocks)}")

        # Check if US exists
        if hasattr(stocks, 'US'):
            us_stocks = stocks.US
            print(f"US Stocks type: {type(us_stocks)}")
            print(f"US Stocks: {us_stocks}")

            # Try to get first few items
            try:
                if hasattr(us_stocks, '__iter__'):
                    count = 0
                    for contract in us_stocks:
                        print(f"Contract {count}: {contract}")
                        if hasattr(contract, '__dict__'):
                            print(f"  Attributes: {contract.__dict__}")
                        count += 1
                        if count >= 3:
                            break
                elif hasattr(us_stocks, '__getitem__'):
                    print(f"US Stocks is dict-like, keys: {list(us_stocks.keys())[:10]}")
            except Exception as e:
                print(f"Error iterating US stocks: {e}")
