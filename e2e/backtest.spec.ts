import { test, expect } from '@playwright/test';

test.describe('Backtest Chart Feature', () => {
  test('should display the backtest chart on the dashboard', async ({ page }) => {
    // Mock the API response for the backtest chart
    await page.route('**/api/backtest/chart', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          dataPoints: [
            { date: '2025-10-29', value: 12000 },
            { date: '2025-11-29', value: 12500 },
          ],
        }),
      });
    });

    await page.goto('/');

    // Check if the chart container is visible
    const backtestChartContainer = page.locator('div:has-text("Portfolio Backtest")').nth(4);
    await expect(backtestChartContainer).toBeVisible();

    // Check for the title
    await expect(backtestChartContainer.locator('h2')).toHaveText('Portfolio Backtest (1 Year)');

    // Check for the chart's presence
    await expect(backtestChartContainer.locator('.recharts-responsive-container')).toBeVisible();
  });
});
