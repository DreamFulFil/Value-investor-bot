import { test, expect } from '@playwright/test';

test.describe('Market Switch Feature', () => {
  test('should toggle between TW and US markets in the header', async ({ page }) => {
    await page.goto('/');

    const twLabel = page.locator('span', { hasText: 'TW' }).first();
    const usLabel = page.locator('span', { hasText: 'US' }).first();
    const toggleButton = page.getByRole('button', { name: /TW/ });
    
    // Default state should be TW
    await expect(twLabel).toHaveClass(/text-blue-600/);
    await expect(usLabel).toHaveClass(/text-gray-500/);

    // Listen for the alert dialog that confirms the switch
    const dialogPromise = page.waitForEvent('dialog');

    // Click the toggle to switch to US
    await toggleButton.click();
    
    // Verify the alert message
    const dialog = await dialogPromise;
    expect(dialog.message()).toContain('Market switched to US');
    await dialog.dismiss();

    // Verify UI state has switched to US
    await expect(twLabel).toHaveClass(/text-gray-500/);
    await expect(usLabel).toHaveClass(/text-blue-600/);
  });
});
