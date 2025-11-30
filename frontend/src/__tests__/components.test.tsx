import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BacktestChart } from '../../src/components/BacktestChart';
import * as api from '../../src/lib/api';
import React from 'react';

// Mocking recharts components for lighter tests
jest.mock('recharts', () => {
    const OriginalModule = jest.requireActual('recharts');
    return {
        ...OriginalModule,
        ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
            <div className="responsive-container">{children}</div>
        ),
        LineChart: ({ children }: { children: React.ReactNode }) => <div data-testid="line-chart">{children}</div>,
        Line: () => <div data-testid="line" />,
        XAxis: () => <div data-testid="x-axis" />,
        YAxis: () => <div data-testid="y-axis" />,
        CartesianGrid: () => <div data-testid="grid" />,
        Tooltip: () => <div data-testid="tooltip" />,
        Legend: () => <div data-testid="legend" />,
    };
});

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: false,
        },
    },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);


describe('BacktestChart', () => {
    beforeEach(() => {
        jest.restoreAllMocks();
    });

    it('renders loading state initially', () => {
        jest.spyOn(api, 'getBacktestChartData').mockReturnValue(new Promise(() => {}));
        render(<BacktestChart />, { wrapper });
        expect(screen.getByText('Loading chart...')).toBeInTheDocument();
    });

    it('renders error state on fetch failure', async () => {
        jest.spyOn(api, 'getBacktestChartData').mockRejectedValue(new Error('API Error'));
        render(<BacktestChart />, { wrapper });
        expect(await screen.findByText('API Error')).toBeInTheDocument();
    });

    it('renders the chart with data on successful fetch', async () => {
        const mockData = {
            dataPoints: [
                { date: '2025-01-01', value: 10000 },
                { date: '2025-02-01', value: 12000 },
            ],
        };
        jest.spyOn(api, 'getBacktestChartData').mockResolvedValue(mockData);
        render(<BacktestChart />, { wrapper });

        await waitFor(() => {
            expect(screen.getByTestId('line-chart')).toBeInTheDocument();
        });

                expect(screen.getByText('Portfolio Backtest (1 Year)')).toBeInTheDocument();

            });

        });

        

        describe('MarketToggle', () => {

          beforeEach(() => {

            // Mock alert

            window.alert = jest.fn();

          });

        

          it('renders with TW market active by default', () => {

            render(<MarketToggle />);

            expect(screen.getByText('TW')).toHaveClass('text-blue-600');

            expect(screen.getByText('US')).toHaveClass('text-gray-500');

          });

        

          it('switches to US market on click and shows alert', () => {

            render(<MarketToggle />);

            const toggleButton = screen.getByRole('button');

            fireEvent.click(toggleButton);

        

            expect(screen.getByText('TW')).toHaveClass('text-gray-500');

            expect(screen.getByText('US')).toHaveClass('text-blue-600');

            expect(window.alert).toHaveBeenCalledWith(expect.stringContaining("Market switched to US"));

          });

        });

        