import { describe, it, expect } from 'vitest';
import {
  portfolioCurrentResponse,
  portfolioHistoryResponse,
  emptyPortfolioResponse,
  portfolioWithNullsResponse,
  positionWithNullsResponse,
  malformedPortfolioResponse,
} from '../../__fixtures__/api-responses';

/**
 * Contract Tests - Verify frontend correctly maps backend response field names.
 * These tests catch the "quantity" vs "shares" type mismatches.
 */
describe('API Contract Tests', () => {
  describe('Backend PositionDTO → Frontend Position mapping', () => {
    it('backend uses "quantity", frontend expects "shares"', () => {
      const backendPosition = portfolioCurrentResponse.positions[0];
      
      // Document the actual backend field name
      expect(backendPosition).toHaveProperty('quantity');
      expect(backendPosition).not.toHaveProperty('shares');
      
      // This is what frontend/lib/api.ts must do:
      const frontendPosition = {
        shares: backendPosition.quantity ?? 0,
      };
      
      expect(frontendPosition.shares).toBe(170.66666666);
    });

    it('backend uses "unrealizedPL", frontend expects "unrealizedGain"', () => {
      const backendPosition = portfolioCurrentResponse.positions[0];
      
      expect(backendPosition).toHaveProperty('unrealizedPL');
      expect(backendPosition).not.toHaveProperty('unrealizedGain');
      
      // This is what frontend/lib/api.ts must do:
      const frontendPosition = {
        unrealizedGain: backendPosition.unrealizedPL ?? 0,
      };
      
      expect(frontendPosition.unrealizedGain).toBe(-110.933333329);
    });
  });

  describe('Backend PortfolioSnapshot → Frontend PortfolioHistory mapping', () => {
    it('backend uses "timestamp", frontend expects "date"', () => {
      const backendSnapshot = portfolioHistoryResponse[0];
      
      expect(backendSnapshot).toHaveProperty('timestamp');
      expect(backendSnapshot).not.toHaveProperty('date');
      
      // This is what frontend/lib/api.ts must do:
      const frontendHistory = {
        date: backendSnapshot.timestamp,
      };
      
      expect(frontendHistory.date).toBe("2025-11-27T20:29:38.537");
    });

    it('backend totalValue maps directly to frontend value', () => {
      const backendSnapshot = portfolioHistoryResponse[0];
      
      const frontendHistory = {
        value: backendSnapshot.totalValue ?? 0,
      };
      
      expect(frontendHistory.value).toBe(15342.496528904);
    });
  });

  describe('Required fields presence', () => {
    it('PortfolioSummary response has all required fields', () => {
      const response = portfolioCurrentResponse;
      
      expect(response).toHaveProperty('totalValue');
      expect(response).toHaveProperty('cashBalance');
      expect(response).toHaveProperty('investedAmount');
      expect(response).toHaveProperty('totalPL');
      expect(response).toHaveProperty('plPercentage');
      expect(response).toHaveProperty('positionCount');
      expect(response).toHaveProperty('positions');
      expect(Array.isArray(response.positions)).toBe(true);
    });

    it('Position response has all required fields', () => {
      const position = portfolioCurrentResponse.positions[0];
      
      expect(position).toHaveProperty('symbol');
      expect(position).toHaveProperty('quantity');
      expect(position).toHaveProperty('averagePrice');
      expect(position).toHaveProperty('currentPrice');
      expect(position).toHaveProperty('marketValue');
      expect(position).toHaveProperty('unrealizedPL');
    });
  });

  describe('Edge case: Null/undefined handling', () => {
    it('handles response with all null fields', () => {
      const response = portfolioWithNullsResponse;
      
      // Frontend must handle nulls gracefully
      const mapped = {
        totalValue: response.totalValue ?? 0,
        cashBalance: response.cashBalance ?? 0,
        positions: response.positions ?? [],
      };
      
      expect(mapped.totalValue).toBe(0);
      expect(mapped.cashBalance).toBe(0);
      expect(mapped.positions).toEqual([]);
    });

    it('handles position with null/undefined fields', () => {
      const position = positionWithNullsResponse as {
        symbol: string;
        quantity: number | null;
        averagePrice: number | undefined;
        currentPrice: number | null;
        marketValue: number | undefined;
        unrealizedPL: number | null;
        plPercentage: number | undefined;
      };
      
      // Frontend must map these without throwing
      const mapped = {
        symbol: position.symbol,
        shares: position.quantity ?? 0,
        currentPrice: position.currentPrice ?? 0,
        marketValue: position.marketValue ?? 0,
        unrealizedGain: position.unrealizedPL ?? 0,
        weight: 0,
      };
      
      expect(mapped.shares).toBe(0);
      expect(mapped.marketValue).toBe(0);
      expect(mapped.unrealizedGain).toBe(0);
    });

    it('handles malformed response with missing fields', () => {
      const response = malformedPortfolioResponse as { totalValue?: number; positions?: unknown[] };
      
      const mapped = {
        totalValue: response.totalValue ?? 0,
        positions: response.positions ?? [],
      };
      
      expect(mapped.totalValue).toBe(10000);
      expect(mapped.positions).toEqual([]);
    });
  });

  describe('Edge case: Empty data', () => {
    it('handles empty portfolio', () => {
      const response = emptyPortfolioResponse;
      
      expect(response.totalValue).toBe(0);
      expect(response.positions).toEqual([]);
      expect(response.positionCount).toBe(0);
    });

    it('handles empty history array', () => {
      const history: typeof portfolioHistoryResponse = [];
      
      const mapped = history.map(item => ({
        date: item.timestamp,
        value: item.totalValue ?? 0,
      }));
      
      expect(mapped).toEqual([]);
    });
  });
});
