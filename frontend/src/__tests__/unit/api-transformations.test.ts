import { describe, it, expect } from 'vitest';

/**
 * Unit Tests for API data transformation functions.
 * Tests null safety, edge cases, and defensive coding patterns.
 */
describe('API Data Transformation Unit Tests', () => {
  describe('Position data mapping', () => {
    // This mirrors the logic in fetchPositions()
    const mapPosition = (p: {
      symbol?: string;
      quantity?: number | null;
      currentPrice?: number | null;
      marketValue?: number | null;
      averagePrice?: number | null;
      unrealizedPL?: number | null;
    }, totalValue: number) => ({
      symbol: p.symbol ?? '',
      name: p.symbol ?? '',
      shares: p.quantity ?? 0,
      currentPrice: p.currentPrice ?? 0,
      marketValue: p.marketValue ?? 0,
      costBasis: (p.quantity ?? 0) * (p.averagePrice ?? 0),
      unrealizedGain: p.unrealizedPL ?? 0,
      weight: totalValue > 0 ? ((p.marketValue ?? 0) / totalValue) * 100 : 0,
    });

    it('maps valid position correctly', () => {
      const backendPosition = {
        symbol: "2330.TW",
        quantity: 100,
        currentPrice: 580,
        marketValue: 58000,
        averagePrice: 550,
        unrealizedPL: 3000,
      };

      const result = mapPosition(backendPosition, 100000);

      expect(result.symbol).toBe("2330.TW");
      expect(result.shares).toBe(100);
      expect(result.currentPrice).toBe(580);
      expect(result.marketValue).toBe(58000);
      expect(result.costBasis).toBe(55000);
      expect(result.unrealizedGain).toBe(3000);
      expect(result.weight).toBeCloseTo(58, 1);
    });

    it('handles null quantity', () => {
      const result = mapPosition({ symbol: "2330.TW", quantity: null }, 100000);
      expect(result.shares).toBe(0);
    });

    it('handles undefined quantity', () => {
      const result = mapPosition({ symbol: "2330.TW" }, 100000);
      expect(result.shares).toBe(0);
    });

    it('handles null marketValue', () => {
      const result = mapPosition({ symbol: "2330.TW", marketValue: null }, 100000);
      expect(result.marketValue).toBe(0);
      expect(result.weight).toBe(0);
    });

    it('handles null unrealizedPL', () => {
      const result = mapPosition({ symbol: "2330.TW", unrealizedPL: null }, 100000);
      expect(result.unrealizedGain).toBe(0);
    });

    it('handles zero totalValue (division by zero)', () => {
      const result = mapPosition({ symbol: "2330.TW", marketValue: 1000 }, 0);
      expect(result.weight).toBe(0);
    });

    it('handles negative unrealizedPL', () => {
      const result = mapPosition({ symbol: "2330.TW", unrealizedPL: -500 }, 100000);
      expect(result.unrealizedGain).toBe(-500);
    });

    it('handles missing symbol', () => {
      const result = mapPosition({}, 100000);
      expect(result.symbol).toBe('');
      expect(result.name).toBe('');
    });

    it('calculates costBasis correctly with nulls', () => {
      const result = mapPosition({
        symbol: "2330.TW",
        quantity: null,
        averagePrice: 100,
      }, 100000);
      expect(result.costBasis).toBe(0); // 0 * 100 = 0
    });
  });

  describe('Portfolio history mapping', () => {
    const mapHistoryItem = (item: {
      timestamp?: string;
      snapshotDate?: string;
      totalValue?: number | null;
    }) => ({
      date: item.timestamp ?? item.snapshotDate ?? new Date().toISOString(),
      value: item.totalValue ?? 0,
    });

    it('maps valid history item', () => {
      const result = mapHistoryItem({
        timestamp: "2025-01-15T10:30:00",
        totalValue: 50000,
      });

      expect(result.date).toBe("2025-01-15T10:30:00");
      expect(result.value).toBe(50000);
    });

    it('handles null totalValue', () => {
      const result = mapHistoryItem({
        timestamp: "2025-01-15T10:30:00",
        totalValue: null,
      });
      expect(result.value).toBe(0);
    });

    it('uses snapshotDate as fallback when timestamp missing', () => {
      const result = mapHistoryItem({
        snapshotDate: "2025-01-15",
        totalValue: 50000,
      });
      expect(result.date).toBe("2025-01-15");
    });

    it('uses current date as fallback when both dates missing', () => {
      const before = new Date().toISOString().slice(0, 10);
      const result = mapHistoryItem({ totalValue: 50000 });
      const after = new Date().toISOString().slice(0, 10);

      // Date should be today
      expect(result.date.slice(0, 10)).toMatch(new RegExp(`${before}|${after}`));
    });
  });

  describe('toLocaleString safety', () => {
    // This tests the exact pattern that caused the original bug

    it('calling toLocaleString on undefined throws', () => {
      const value: number | undefined = undefined;
      expect(() => (value as unknown as number).toLocaleString()).toThrow();
    });

    it('calling toLocaleString on null throws', () => {
      const value: number | null = null;
      expect(() => (value as unknown as number).toLocaleString()).toThrow();
    });

    it('defensive pattern prevents crash', () => {
      const value: number | undefined = undefined;
      const safe = (value ?? 0).toLocaleString();
      expect(safe).toBe('0');
    });

    it('defensive pattern works with valid number', () => {
      const value: number | undefined = 1234.56;
      const safe = (value ?? 0).toLocaleString();
      expect(safe).toContain('1');
      expect(safe).toContain('234');
    });

    it('defensive pattern works with zero', () => {
      const value: number | undefined = 0;
      const safe = (value ?? 0).toLocaleString();
      expect(safe).toBe('0');
    });

    it('defensive pattern works with negative', () => {
      const value: number | undefined = -1234.56;
      const safe = (value ?? 0).toLocaleString();
      expect(safe).toContain('1');
      expect(safe).toContain('234');
    });
  });

  describe('Array safety', () => {
    it('calling map on undefined throws', () => {
      const arr: number[] | undefined = undefined;
      expect(() => (arr as unknown as number[]).map(x => x)).toThrow();
    });

    it('defensive pattern prevents crash', () => {
      const arr: number[] | undefined = undefined;
      const safe = (arr ?? []).map(x => x * 2);
      expect(safe).toEqual([]);
    });

    it('defensive pattern works with valid array', () => {
      const arr: number[] | undefined = [1, 2, 3];
      const safe = (arr ?? []).map(x => x * 2);
      expect(safe).toEqual([2, 4, 6]);
    });

    it('defensive pattern works with empty array', () => {
      const arr: number[] | undefined = [];
      const safe = (arr ?? []).map(x => x * 2);
      expect(safe).toEqual([]);
    });
  });

  describe('PortfolioSummary mapping', () => {
    const mapSummary = (data: {
      totalValue?: number | null;
      totalCost?: number | null;
      unrealizedGain?: number | null;
      returnPercentage?: number | null;
      cashBalance?: number | null;
      positions?: unknown[] | null;
    }) => ({
      totalValue: data.totalValue ?? 0,
      totalCost: data.totalCost ?? 0,
      totalReturn: data.unrealizedGain ?? 0,
      returnPercentage: data.returnPercentage ?? 0,
      cashBalance: data.cashBalance ?? 0,
      positionCount: data.positions?.length ?? 0,
    });

    it('maps valid summary', () => {
      const result = mapSummary({
        totalValue: 100000,
        totalCost: 90000,
        unrealizedGain: 10000,
        returnPercentage: 11.11,
        cashBalance: 5000,
        positions: [{}, {}, {}],
      });

      expect(result.totalValue).toBe(100000);
      expect(result.positionCount).toBe(3);
    });

    it('handles all nulls', () => {
      const result = mapSummary({
        totalValue: null,
        totalCost: null,
        unrealizedGain: null,
        returnPercentage: null,
        cashBalance: null,
        positions: null,
      });

      expect(result.totalValue).toBe(0);
      expect(result.positionCount).toBe(0);
    });

    it('handles empty object', () => {
      const result = mapSummary({});

      expect(result.totalValue).toBe(0);
      expect(result.positionCount).toBe(0);
    });
  });
});
