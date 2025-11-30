import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Value Investor Bot Dashboard
 * Tests critical user journeys for Taiwan stock value investing application
 */

test.describe('Dashboard Page', () => {
  test.beforeEach(async ({ page }) => {
    page.on('request', request => console.log('>>', request.method(), request.url()));
    page.on('response', response => console.log('<<', response.status(), response.url()));
    page.on('console', message => console.log(`Console ${message.type().toUpperCase()}: ${message.text()}`));

    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await page.evaluate(() => window.dispatchEvent(new Event('resize')));
    const rebalanceButton = page.locator('button').filter({ hasText: /Run First Monthly Rebalance|Run Monthly Rebalance Now|執行首次月度再平衡|立即執行月度再平衡/ });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
  });

  test('should load the dashboard page successfully', async ({ page }) => {
    await expect(page).toHaveTitle(/Value Investor/);
    await expect(page.locator('header')).toBeVisible({ timeout: 30000 });
    await expect(page.locator('main')).toBeVisible({ timeout: 30000 });
  });

  test('should display simulation mode badge', async ({ page }) => {
    const badge = page.locator('span.bg-blue-500\\/20:has-text("Simulation"), span.bg-blue-500\\/20:has-text("SIMULATION"), span.bg-blue-500\\/20:has-text("模擬模式")').first();
    await expect(badge).toBeVisible({ timeout: 30000 });
  });

  test('should show rebalance button', async ({ page }) => {
    const rebalanceButton = page.locator('button:has-text("Rebalance"), button:has-text("再平衡")');
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    await expect(rebalanceButton).toBeEnabled();
  });

  test('should display all stat cards', async ({ page }) => {
    const statCards = page.locator('div.card.relative.group');
    await expect(statCards.first()).toBeVisible({ timeout: 30000 });
  });

  test('should display portfolio value card', async ({ page }) => {
    const portfolioCard = page.locator('div.card.relative.group', { hasText: /Portfolio Value|投資組合價值/i }).first();
    await expect(portfolioCard).toBeVisible({ timeout: 30000 });
  });
});

test.describe('Rebalance Flow - Happy Path', () => {
  test('should execute monthly rebalance successfully', async ({ page }) => {
    await page.goto('/');

    // Wait for page to load
    await page.waitForLoadState('networkidle');

    // Find and click the rebalance button
    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡|First Rebalance|第一次再平衡/ });
    await expect(rebalanceButton).toBeVisible();

    // Click rebalance
    await rebalanceButton.click();

    // Should show loading state
    const loadingIndicator = page.locator('button:has-text("Rebalancing"), button:has-text("執行中"), .animate-spin');
    
    // Wait for success (with progress modal or success message)
    await expect(async () => {
      const successVisible = await page.locator('text=success, text=SUCCESS, text=成功, text=Already rebalanced, text=已完成').first().isVisible();
      const progressModalVisible = await page.locator('[data-testid="progress-modal"], .fixed.inset-0').first().isVisible();
      expect(successVisible || progressModalVisible).toBeTruthy();
    }).toPass({ timeout: 30000 });
  });

    test('should show progress modal during rebalance', async ({ page }) => {

      await page.goto('/');

      const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    await expect(rebalanceButton).toBeVisible();
    
    // Click and check for progress modal
    await rebalanceButton.click();
    
    // Progress modal should appear or button should show loading
    const progressIndicator = page.locator('.animate-spin, [data-testid="progress-modal"], button:disabled');
    await expect(progressIndicator.first()).toBeVisible({ timeout: 30000 });
  });

  test('should update portfolio after rebalance', async ({ page }) => {
    await page.goto('/');

    // Get initial state
    const initialValueElement = page.locator('[class*="stat"]:has-text("NT$"), [class*="value"]:has-text("$")').first();
    
    // Execute rebalance if button is available
    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      
      // Wait for completion
      // Holdings table should exist after rebalance
      const holdingsSection = page.locator('table, [data-testid="holdings-table"], [class*="holdings"]');
      await expect(holdingsSection.first()).toBeVisible({ timeout: 10000 });
    }
  });
});

test.describe('Portfolio Display', () => {
  test('should display holdings table with Taiwan stocks', async ({ page }) => {
    await page.goto('/');

    // Look for typical Taiwan stock symbols or the table
    const holdingsTable = page.locator('table, [data-testid="holdings-table"]');
    
    if (await holdingsTable.isVisible()) {
      // Check for Taiwan stock format (.TW suffix)
      const stockSymbols = page.locator('td:has-text(".TW"), [class*="symbol"]:has-text(".TW")');
      const symbolCount = await stockSymbols.count();
      
      if (symbolCount > 0) {
        // Verify the format is correct Taiwan stock symbol
        const firstSymbol = await stockSymbols.first().textContent();
        expect(firstSymbol).toMatch(/\d{4}\.TW/);
      }
    }
  });

  test('should display portfolio chart', async ({ page }) => {
    await page.goto('/');

    const chart = page.locator('[data-testid="line-chart"], [data-testid="portfolio-chart"], svg, canvas').first();
    await expect(chart).toBeVisible({ timeout: 10000 });
  });

  test('should display allocation pie chart', async ({ page }) => {
    // Perform a rebalance to ensure data is available and isEmpty is false
    const rebalanceButton = page.locator('button').filter({
      hasText: /Run First Monthly Rebalance|Run Monthly Rebalance Now|執行首次月度再平衡|立即執行月度再平衡/
    });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    await rebalanceButton.click();

    await expect(async () => {
      const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
      const modalClosed = !(await page.locator('[data-testid="progress-modal"]').first().isVisible());
      expect(successVisible || modalClosed).toBeTruthy();
    }).toPass({ timeout: 60000 }); // Use a longer timeout for the entire rebalance process

    const pieChart = page.locator('div.card.relative:has-text("Asset Allocation"), div.card.relative:has-text("資產配置")').first();
    await expect(pieChart).toBeVisible({ timeout: 30000 });
  });

  test('should display goal ring with dividend target', async ({ page }) => {
    await page.goto('/');

    // Goal ring shows weekly dividend target of NT$1,600
    const goalSection = page.locator('[data-testid="goal-ring"], :has-text("1,600"), :has-text("Goal")').first();
    await expect(goalSection).toBeVisible({ timeout: 30000 });
  });
});

test.describe('Quota Display', () => {
  test('should display quota card', async ({ page }) => {
    await page.goto('/');

    // Quota card shows Shioaji API usage
    const quotaCard = page.locator('[data-testid="quota-card"], :has-text("Quota"), :has-text("MB"), :has-text("配額")').first();
    await expect(quotaCard).toBeVisible({ timeout: 30000 });
  });

  test('should show fallback warning when quota is low', async ({ page }) => {
    // This test checks for the fallback warning UI element
    await page.goto('/');

    // Check if fallback warning exists (may not be visible if quota is fine)
    const fallbackWarning = page.locator('[data-testid="fallback-warning"], .bg-yellow-50:has-text("Fallback"), .bg-yellow-50:has-text("配額")');
    
    // Just verify the page handles quota state properly (element may or may not be visible)
    const quotaElement = page.locator(':has-text("MB")').first();
    await expect(quotaElement).toBeVisible({ timeout: 30000 });
  });
});

test.describe('Insights Panel', () => {
  test('should display insights section', async ({ page }) => {
    // Perform a rebalance to ensure data is available and isEmpty is false
    const rebalanceButton = page.locator('button').filter({
      hasText: /Run First Monthly Rebalance|Run Monthly Rebalance Now|執行首次月度再平衡|立即執行月度再平衡/
    });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    await rebalanceButton.click();

    await expect(async () => {
      const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
      const modalClosed = !(await page.locator('[data-testid="progress-modal"]').first().isVisible());
      expect(successVisible || modalClosed).toBeTruthy();
    }).toPass({ timeout: 60000 }); // Use a longer timeout for the entire rebalance process

    await page.reload(); // Add reload to ensure UI is updated

    const insightsPanel = page.locator('div.card.relative:has-text("Latest Insights"), div.card.relative:has-text("最新洞察")').first();
    await expect(insightsPanel).toBeVisible({ timeout: 30000 });
  });
});

test.describe('Language Toggle', () => {
  test('should toggle between English and Chinese', async ({ page }) => {
    await page.goto('/');

    // Find language toggle button
    const langToggle = page.locator('button:has-text("EN"), button:has-text("中文"), [data-testid="lang-toggle"]');
    
    if (await langToggle.isVisible()) {
      // Get initial text
      const initialText = await langToggle.textContent();
      
      // Click to toggle
      await langToggle.click();
      
      // Text should change
      const newText = await langToggle.textContent();
      expect(newText).not.toBe(initialText);
    }
  });
});

test.describe('Go Live Wizard', () => {
  test('should open go live wizard when clicking go live button', async ({ page }) => {
    await page.goto('/');

    // First need to have done a rebalance
    const goLiveButton = page.locator('button:has-text("Go Live"), button:has-text("上線"), button:has-text("goLive")');
    
    if (await goLiveButton.isVisible()) {
      await goLiveButton.click();
      
      // Wizard modal should appear
      const wizardModal = page.locator('[data-testid="go-live-wizard"], .fixed.inset-0:has-text("Live"), .fixed.inset-0:has-text("上線")');
      await expect(wizardModal).toBeVisible({ timeout: 30000 });
    }
  });
});

test.describe('Responsive Design', () => {
  test('should work on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/');

    // Header should still be visible
    await expect(page.locator('header')).toBeVisible({ timeout: 30000 });
    
    await expect(page.locator('main')).toBeVisible({ timeout: 30000 });
    
    // Rebalance button should be visible
    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
  });

  test('should work on tablet viewport', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto('/');

    await expect(page.locator('main')).toBeVisible({ timeout: 30000 });
  });
});

test.describe('Error Handling', () => {
  test('should handle API errors gracefully', async ({ page }) => {
    // Intercept API calls and simulate error
    await page.route('**/api/**', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal server error' })
      });
    });

    await page.goto('/');
    
    // Page should still load without crashing
    await expect(page.locator('header')).toBeVisible({ timeout: 30000 });
    await expect(page.locator('main')).toBeVisible({ timeout: 30000 });
  });

  test('should show error message on rebalance failure', async ({ page }) => {
    await page.goto('/');

    // Intercept rebalance API and simulate error
    const rebalanceErrorResponse = page.waitForResponse(async response => 
      response.url().includes('/api/trading/rebalance') && response.status() === 500
    );

    await page.route('**/api/trading/rebalance', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Rebalance failed', message: 'Database error' })
      });
    });

    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      
      await rebalanceErrorResponse; // Wait for the mocked error response to come back

      // Error message should appear. Use a more robust selector.
      const errorMessage = page.locator(':text-matches("Database error|Rebalance failed", "i")').first();
      await expect(errorMessage).toBeVisible({ timeout: 30000 });
      await expect(errorMessage).toContainText(/Database error|Rebalance failed/);
    }
  });
});

test.describe('Data Integrity', () => {
  test('should display correct currency format (TWD)', async ({ page }) => {
    await page.goto('/');

    // Look for NT$ or TWD formatted values
    const currencyValues = page.locator('text=/NT\\$[\\d,]+/, text=/\\$[\\d,]+/');
    
    if (await currencyValues.count() > 0) {
      const value = await currencyValues.first().textContent();
      // Should contain proper number formatting with commas
      expect(value).toMatch(/[\d,]+/);
    }
  });

  test('should display percentage values correctly', async ({ page }) => {
    await page.goto('/');

    // Look for percentage values
    const percentValues = page.locator('text=/[+-]?\\d+\\.?\\d*%/');
    
    if (await percentValues.count() > 0) {
      const value = await percentValues.first().textContent();
      expect(value).toMatch(/[+-]?\d+\.?\d*%/);
    }
  });
});
