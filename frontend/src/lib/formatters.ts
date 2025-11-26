import { format, formatDistanceToNow, parseISO } from 'date-fns';

/**
 * Format number as TWD currency
 */
export const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
};

/**
 * Format number as currency with optional decimals
 */
export const formatMoney = (value: number, decimals: number = 0): string => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: 'TWD',
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
};

/**
 * Format number as NT$ string
 */
export const formatNTD = (value: number): string => {
  return `NT$${value.toLocaleString('zh-TW')}`;
};

/**
 * Format number as percentage
 */
export const formatPercentage = (value: number, decimals: number = 2): string => {
  return `${value >= 0 ? '+' : ''}${value.toFixed(decimals)}%`;
};

/**
 * Format number as percentage without sign
 */
export const formatPercent = (value: number, decimals: number = 2): string => {
  return `${value.toFixed(decimals)}%`;
};

/**
 * Format large numbers with K, M, B suffixes
 */
export const formatCompactNumber = (value: number): string => {
  if (value >= 1_000_000_000) {
    return `NT$${(value / 1_000_000_000).toFixed(2)}B`;
  }
  if (value >= 1_000_000) {
    return `NT$${(value / 1_000_000).toFixed(2)}M`;
  }
  if (value >= 1_000) {
    return `NT$${(value / 1_000).toFixed(0)}K`;
  }
  return formatCurrency(value);
};

/**
 * Format date to readable string
 */
export const formatDate = (date: string | Date, formatStr: string = 'MMM dd, yyyy'): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date;
  return format(dateObj, formatStr);
};

/**
 * Format date to relative time (e.g., "2 hours ago")
 */
export const formatRelativeTime = (date: string | Date): string => {
  const dateObj = typeof date === 'string' ? parseISO(date) : date;
  return formatDistanceToNow(dateObj, { addSuffix: true });
};

/**
 * Format date to short format (e.g., "01/15/2024")
 */
export const formatShortDate = (date: string | Date): string => {
  return formatDate(date, 'MM/dd/yyyy');
};

/**
 * Format date to long format (e.g., "January 15, 2024")
 */
export const formatLongDate = (date: string | Date): string => {
  return formatDate(date, 'MMMM dd, yyyy');
};

/**
 * Format number with thousands separator
 */
export const formatNumber = (value: number, decimals: number = 0): string => {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
};

/**
 * Format shares (always 0 decimals)
 */
export const formatShares = (value: number): string => {
  return formatNumber(value, 0);
};

/**
 * Get color class for positive/negative values
 */
export const getChangeColor = (value: number): string => {
  if (value > 0) return 'text-green-600 dark:text-green-400';
  if (value < 0) return 'text-red-600 dark:text-red-400';
  return 'text-muted-foreground';
};

/**
 * Get background color class for positive/negative values
 */
export const getChangeBgColor = (value: number): string => {
  if (value > 0) return 'bg-green-100 dark:bg-green-900/20';
  if (value < 0) return 'bg-red-100 dark:bg-red-900/20';
  return 'bg-muted';
};

/**
 * Format Sharpe ratio
 */
export const formatSharpeRatio = (value: number): string => {
  return value.toFixed(2);
};

/**
 * Format drawdown (always negative or zero)
 */
export const formatDrawdown = (value: number): string => {
  return `${value.toFixed(2)}%`;
};

/**
 * Parse date string to Date object
 */
export const parseDate = (dateStr: string): Date => {
  return parseISO(dateStr);
};

/**
 * Format date for API (ISO format)
 */
export const formatApiDate = (date: Date): string => {
  return format(date, 'yyyy-MM-dd');
};
