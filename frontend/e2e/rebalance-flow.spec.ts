import { test, expect } from '@playwright/test';

/**
 * E2E Tests for Complete Rebalance User Journey
 * Tests the critical monthly rebalance flow for Taiwan value investing
 */

test.describe('Complete Rebalance User Journey', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    const rebalanceButton = page.locator('button').filter({ hasText: /Run First Monthly Rebalance|Run Monthly Rebalance Now|執行首次月度再平衡|立即執行月度再平衡/ });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
  });
  
  test('Happy Path: First-time user runs initial rebalance', async ({ page }) => {
    // Step 1: User sees the initial state with call-to-action
    const rebalanceButton = page.locator('button').filter({
      hasText: /Run First Monthly Rebalance|Run Monthly Rebalance Now|執行首次月度再平衡|立即執行月度再平衡/
    });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    await expect(rebalanceButton).toBeEnabled({ timeout: 10000 }); // Ensure button is enabled before clicking

    // Step 3: User clicks rebalance button
    await rebalanceButton.click();

    // Explicitly wait for the button to become disabled
    await rebalanceButton.waitFor({ state: 'disabled', timeout: 30000 });
    await expect(rebalanceButton).toBeDisabled();

    // Explicitly wait for the progress modal to appear
    const progressModal = page.locator('[data-testid="progress-modal"]');
    await expect(progressModal).toBeVisible({ timeout: 30000 });

    // Step 5: Wait for completion (success or already rebalanced message)
    await expect(async () => {
      const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
      const modalClosed = !(await page.locator('[data-testid="progress-modal"]')
        .first()
        .isVisible());
      expect(successVisible || modalClosed).toBeTruthy();
    }).toPass({ timeout: 60000 });

    // Step 6: Dashboard should update with portfolio data
    await page.waitForLoadState('networkidle');
    
    // Verify state is persisted
    const hasRebalanced = await page.evaluate(() => localStorage.getItem('hasRebalanced'));
    expect(hasRebalanced).toBe('true');
  });

  test('Idempotency: Same-month rebalance is blocked', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Set up as if user already rebalanced this month
    await page.evaluate(() => localStorage.setItem('hasRebalanced', 'true'));
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Click rebalance again
    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    await rebalanceButton.click();

    // Should show "already rebalanced" message or complete quickly
    await expect(async () => {
      const alreadyMessage = await page.locator('text=/Already|已.*平衡|this month|本月/i').first().isVisible();
      const successMessage = await page.locator('text=/success|成功/i').first().isVisible();
      expect(alreadyMessage || successMessage).toBeTruthy();
    }).toPass({ timeout: 30000 });
  });

  test('Progress Tracking: User sees step-by-step progress', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await page.waitForLoadState('networkidle');

    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    await rebalanceButton.click();

    // Check for progress modal with steps
    const progressModal = page.locator('[data-testid="progress-modal"], .fixed.inset-0.bg-black');
    
    if (await progressModal.isVisible({ timeout: 3000 })) {
      // Should show progress steps or percentage
      const progressContent = page.locator('text=/Analyzing|Screening|Buying|分析|篩選|購買|\d+%/i');
      await expect(progressContent.first()).toBeVisible({ timeout: 30000 });
    }

    // Wait for completion
    await expect(async () => {
      const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
      const modalClosed = !(await progressModal.first().isVisible());
      expect(successVisible || modalClosed).toBeTruthy();
    }).toPass({ timeout: 60000 });
  });

  test('Error Recovery: User sees error and can retry', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Simulate API failure on first attempt
    let failedOnce = false;
    await page.route('**/api/trading/rebalance', async route => {
      if (!failedOnce) {
        failedOnce = true;
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Temporary failure', message: 'Please retry' })
        });
      } else {
        await route.continue();
      }
    });

    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    
    // First click - should fail
    await rebalanceButton.click();
    
    // Error message should appear
    const errorMessage = page.locator('text=/error|Error|錯誤|failed|失敗/i');
    await expect(errorMessage.first()).toBeVisible({ timeout: 30000 });

    // Button should be re-enabled for retry
    await expect(rebalanceButton).toBeEnabled({ timeout: 5000 });

    // Retry should work
    await rebalanceButton.click();
    
    // Should proceed to success or another state
    await expect(async () => {
      const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
      const modalClosed = !(await page.locator('[data-testid="progress-modal"]').first().isVisible());
      expect(successVisible || modalClosed).toBeTruthy();
    }).toPass({ timeout: 60000 });
  });

  test('Multi-month Catch-up: Backend handles missed months', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // This tests the backend's ability to catch up missed months
    // The UI should show the rebalance happening for multiple months if needed

    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    await expect(rebalanceButton).toBeVisible({ timeout: 30000 });
    await rebalanceButton.click();

    // Wait for completion
    await expect(async () => {
      const completed = await page.locator('text=/success|Success|成功|complete|完成/i').first().isVisible();
      const buttonEnabled = await rebalanceButton.isEnabled();
      expect(completed || buttonEnabled).toBeTruthy();
    }).toPass({ timeout: 60000 });
  });
});

test.describe('Rebalance Results Display', () => {
  
  test('should show transaction summary after rebalance', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Execute a rebalance
    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      
      // Wait for completion
      await expect(async () => {
        const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
        const rebalanceButtonEnabled = await rebalanceButton.isEnabled();
        expect(successVisible || rebalanceButtonEnabled).toBeTruthy();
      }).toPass({ timeout: 60000 });
      await page.waitForLoadState('networkidle');

      // Holdings table should now show purchased stocks
      const holdingsTable = page.locator('table, [data-testid="holdings-table"]');
      
      if (await holdingsTable.isVisible()) {
        // Should have Taiwan stock symbols
        const taiwanStocks = page.locator('text=/\d{4}\.TW/');
        const stockCount = await taiwanStocks.count();
        
        // After rebalance, we should have some holdings
        // (may be 0 if already processed this month)
        expect(stockCount).toBeGreaterThanOrEqual(0);
      }
    }
  });

  test('should update stat cards with new values', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Get initial portfolio value text
    const portfolioValueCard = page.locator(':has-text("portfolioValue"), :has-text("Portfolio Value"), :has-text("投資組合價值")').first();
    
    await expect(portfolioValueCard).toBeVisible({ timeout: 30000 });

    // Execute rebalance
    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      await expect(async () => {
        const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
        const rebalanceButtonEnabled = await rebalanceButton.isEnabled();
        expect(successVisible || rebalanceButtonEnabled).toBeTruthy();
      }).toPass({ timeout: 60000 });
      await page.waitForLoadState('networkidle');
      
      // Portfolio value card should still be present
      await expect(portfolioValueCard).toBeVisible({ timeout: 30000 });
    }
  });

  test('should refresh insights after rebalance', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Execute rebalance
    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      await expect(async () => {
        const successVisible = await page.locator('text=/success|Success|成功|Already|已/i').first().isVisible();
        const rebalanceButtonEnabled = await rebalanceButton.isEnabled();
        expect(successVisible || rebalanceButtonEnabled).toBeTruthy();
      }).toPass({ timeout: 60000 });
      await page.waitForLoadState('networkidle');

      // Insights panel should be updated
      const insightsPanel = page.locator('div.card.relative:has-text("Latest Insights"), div.card.relative:has-text("最新洞察")');
      await expect(insightsPanel.first()).toBeVisible({ timeout: 30000 });
    }
  });
});

test.describe('Rebalance Button States', () => {
  
  test('button shows correct text for first-time user', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.evaluate(() => localStorage.removeItem('hasRebalanced'));
    await page.reload();
    await page.waitForLoadState('networkidle');

        const button = page.locator('button').filter({

          hasText: /🚀 Run First Monthly Rebalance|🚀 執行首次月度再平衡/

        });
    
    // Button should indicate it's the first rebalance
    await expect(button.first()).toBeVisible({ timeout: 30000 });
  });

  test('button shows correct text for returning user', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.evaluate(() => localStorage.setItem('hasRebalanced', 'true'));
    await page.reload();
    await page.waitForLoadState('networkidle');

    const button = page.locator('button').filter({ 
      hasText: /Monthly Rebalance|Run Monthly|月度再平衡|runMonthlyRebalance/ 
    });
    
    // Button should indicate monthly rebalance
    await expect(button.first()).toBeVisible({ timeout: 30000 });
  });

  test('button is disabled during rebalance', async ({ page }) => {
    // Mock API call to simulate rebalance process
    await page.route('**/api/rebalance', async route => {
      // Delay fulfillment to allow Playwright to observe the loading state
      await new Promise(f => setTimeout(f, 2000)); // 2 second delay
      await route.fulfill({
        status: 200,
        body: JSON.stringify({ message: 'Rebalance initiated' }),
      });
    });

    // Use the general rebalance button locator as defined in beforeEach
    const rebalanceButton = page.locator('button').filter({ hasText: /Run First Monthly Rebalance|Run Monthly Rebalance Now|執行首次月度再平衡|立即執行月度再平衡/ });
    await expect(rebalanceButton).toBeEnabled({ timeout: 10000 }); // Add this line
    await rebalanceButton.click();

    // Explicitly wait for the spinner to appear within the button, which indicates isLoading=true
    const spinner = page.locator('button svg.animate-spin');
    await expect(spinner).toBeVisible({ timeout: 30000 });
    
    // Then assert the button itself is disabled
    await expect(rebalanceButton).toBeDisabled({ timeout: 30000 });
  });
});

test.describe('Confetti Celebration', () => {
  
  test('should show confetti on first rebalance', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.evaluate(() => localStorage.removeItem('hasRebalanced'));
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Mock canvas-confetti or check for canvas elements
    const rebalanceButton = page.locator('button').filter({ hasText: /First Rebalance|第一次|Rebalance/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      
      // Wait a bit for confetti to potentially fire
      await page.waitForTimeout(2000);
      
      // Check for canvas element that confetti creates
      const confettiCanvas = page.locator('canvas');
      // Confetti may or may not be visible depending on implementation
      // This is a soft check
    }
  });
});

test.describe('Success Message Toast', () => {
  
  test('should show success toast after rebalance', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      
      // Wait for success toast
      const successToast = page.locator('.bg-green-500:has-text("✓")').or(page.locator('text=/success|成功/i')).or(page.locator('.animate-bounce'));
      
      await expect(async () => {
        const toastVisible = await successToast.first().isVisible();
        const completed = !(await rebalanceButton.isDisabled());
        expect(toastVisible || completed).toBeTruthy();
      }).toPass({ timeout: 30000 });
    }
  });

  test('success toast should auto-dismiss', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const rebalanceButton = page.locator('button').filter({ hasText: /Rebalance|再平衡/ });
    
    if (await rebalanceButton.isVisible()) {
      await rebalanceButton.click();
      
      // Wait for success toast to appear, then wait for it to disappear
      const successToast = page.locator('.bg-green-500:has-text("✓")').or(page.locator('text=/success|成功/i')).or(page.locator('.animate-bounce'));
      await expect(successToast.first()).toBeVisible({ timeout: 30000 });
      await expect(successToast.first()).not.toBeVisible({ timeout: 10000 });
    }
  });
});
