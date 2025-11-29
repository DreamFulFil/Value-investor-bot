-- Transaction Log Table
CREATE TABLE IF NOT EXISTS transaction_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_date TIMESTAMP NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- BUY, SELL
    symbol VARCHAR(10) NOT NULL,
    quantity DECIMAL(10, 4) NOT NULL,
    price DECIMAL(10, 4) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    commission DECIMAL(10, 2) DEFAULT 0,
    market VARCHAR(10) NOT NULL, -- US, TW
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Position History Table
CREATE TABLE IF NOT EXISTS position_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(10) NOT NULL,
    market VARCHAR(10) NOT NULL,
    quantity DECIMAL(10, 4) NOT NULL,
    average_cost DECIMAL(10, 4) NOT NULL,
    current_price DECIMAL(10, 4),
    market_value DECIMAL(12, 2),
    unrealized_pnl DECIMAL(12, 2),
    unrealized_pnl_percent DECIMAL(8, 4),
    snapshot_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Portfolio Snapshot Table
CREATE TABLE IF NOT EXISTS portfolio_snapshot (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    snapshot_date DATE NOT NULL UNIQUE,
    total_value DECIMAL(12, 2) NOT NULL,
    cash_balance DECIMAL(12, 2) NOT NULL,
    invested_amount DECIMAL(12, 2) NOT NULL,
    total_pnl DECIMAL(12, 2),
    total_pnl_percent DECIMAL(8, 4),
    market VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stock Fundamentals Table
CREATE TABLE IF NOT EXISTS stock_fundamentals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(10) NOT NULL,
    market VARCHAR(10) NOT NULL,
    company_name VARCHAR(255),
    sector VARCHAR(100),
    industry VARCHAR(100),
    market_cap DECIMAL(15, 2),
    pe_ratio DECIMAL(8, 4),
    pb_ratio DECIMAL(8, 4),
    dividend_yield DECIMAL(8, 4),
    eps DECIMAL(10, 4),
    book_value DECIMAL(10, 4),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(symbol, market)
);

-- Analysis Results Table
CREATE TABLE IF NOT EXISTS analysis_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(10) NOT NULL,
    market VARCHAR(10) NOT NULL,
    analysis_date DATE NOT NULL,
    value_score DECIMAL(5, 2),
    recommendation VARCHAR(20), -- BUY, HOLD, SELL
    reasoning TEXT,
    llm_provider VARCHAR(50),
    llm_model VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stock Universe Table (tradeable stocks)
CREATE TABLE IF NOT EXISTS stock_universe (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    sector VARCHAR(100),
    market VARCHAR(10) NOT NULL DEFAULT 'US',
    active BOOLEAN NOT NULL DEFAULT 1,
    added_date DATE NOT NULL,
    removed_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stock Price History Table (daily OHLCV data)
CREATE TABLE IF NOT EXISTS stock_price_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol VARCHAR(10) NOT NULL,
    date DATE NOT NULL,
    open DECIMAL(12, 4) NOT NULL,
    high DECIMAL(12, 4) NOT NULL,
    low DECIMAL(12, 4) NOT NULL,
    close DECIMAL(12, 4) NOT NULL,
    volume BIGINT NOT NULL,
    adjusted_close DECIMAL(12, 4),
    market VARCHAR(10) NOT NULL DEFAULT 'US',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(symbol, date)
);

-- Market Index Table (S&P 500, NASDAQ, etc.)
CREATE TABLE IF NOT EXISTS market_index (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name VARCHAR(20) NOT NULL,
    date DATE NOT NULL,
    value DECIMAL(12, 4) NOT NULL,
    change DECIMAL(10, 4),
    change_percent DECIMAL(8, 4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(index_name, date)
);

-- Daily Learning Tip Table
CREATE TABLE IF NOT EXISTS daily_learning_tip (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tip_date DATE NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    liked BOOLEAN DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insights History Table
CREATE TABLE IF NOT EXISTS insights_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    generated_date DATE NOT NULL UNIQUE,
    insights_content TEXT NOT NULL,
    portfolio_value DECIMAL(12, 2),
    monthly_return DECIMAL(8, 4),
    cash_balance DECIMAL(12, 2),
    total_invested DECIMAL(12, 2),
    positions_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trading Config Table (persistent trading configuration)
CREATE TABLE IF NOT EXISTS trading_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(50) NOT NULL UNIQUE,
    config_value VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_transaction_log_symbol ON transaction_log(symbol);
CREATE INDEX IF NOT EXISTS idx_transaction_log_date ON transaction_log(transaction_date);
CREATE INDEX IF NOT EXISTS idx_position_history_symbol ON position_history(symbol);
CREATE INDEX IF NOT EXISTS idx_position_history_date ON position_history(snapshot_date);
CREATE INDEX IF NOT EXISTS idx_portfolio_snapshot_date ON portfolio_snapshot(snapshot_date);
CREATE INDEX IF NOT EXISTS idx_stock_fundamentals_symbol ON stock_fundamentals(symbol, market);
CREATE INDEX IF NOT EXISTS idx_analysis_results_symbol ON analysis_results(symbol, market, analysis_date);
CREATE INDEX IF NOT EXISTS idx_universe_symbol ON stock_universe(symbol);
CREATE INDEX IF NOT EXISTS idx_universe_market_active ON stock_universe(market, active);
CREATE INDEX IF NOT EXISTS idx_price_history_symbol ON stock_price_history(symbol);
CREATE INDEX IF NOT EXISTS idx_price_history_date ON stock_price_history(date);
CREATE INDEX IF NOT EXISTS idx_price_history_symbol_date ON stock_price_history(symbol, date);
CREATE INDEX IF NOT EXISTS idx_market_index_name ON market_index(index_name);
CREATE INDEX IF NOT EXISTS idx_market_index_date ON market_index(date);
CREATE INDEX IF NOT EXISTS idx_learning_tip_date ON daily_learning_tip(tip_date);
CREATE INDEX IF NOT EXISTS idx_learning_tip_category ON daily_learning_tip(category);
CREATE INDEX IF NOT EXISTS idx_insights_history_date ON insights_history(generated_date);
