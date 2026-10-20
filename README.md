# Rigour — Institutional Cryptocurrency Analysis Platform

Rigour is an institutional-grade cryptocurrency analysis Android application built with **Jetpack Compose**, **Kotlin**, and modern reactive architecture.

## Features Overview

1. **Live Market Data Engine**
   - Direct integration with **CoinMarketCap Pro API** (`/v1/cryptocurrency/listings/latest` and `/v1/cryptocurrency/quotes/latest`).
   - Resilient zero-configuration fallback to live public market feeds (Binance 24h tickers & OHLCV klines) to ensure instantaneous quotes out-of-the-box.
   - Real-time search by cryptocurrency name or symbol (e.g. BTC, ETH, SOL, XRP, BNB, etc.).
   - Interactive watchlists and favorites.

2. **Technical Analysis & Signal Confluence Engine**
   - **RSI (Relative Strength Index):** 14-period default. RSI > 70 denotes overbought conditions (sell signal); RSI < 30 denotes oversold conditions (buy signal).
   - **MACD (Moving Average Convergence Divergence):** Standard 12, 26, 9 parameters. Evaluates EMA crossovers and histogram expansion to determine bullish vs bearish momentum.
   - **Bollinger Bands:** 20-period SMA and 2 standard deviations (2σ). Flags lower band support touches (buy signal) and upper band resistance touches (sell signal).
   - **Confluence Signal:** Synthesizes indicator votes into definitive ratings: **STRONG BUY**, **BUY**, **NEUTRAL**, **SELL**, **STRONG SELL** with engine conviction percentage (50% - 98%).
   - **Price Targets & Pivot Levels:** Calculates projected short-term targets, medium-term targets, risk-managed stop losses, and classical Pivot Point, R1, R2, S1, S2 matrices.
   - **Interactive Canvas Candlestick Chart:** High-definition candles, 2σ Bollinger envelope shading, SMA center line, and dynamic price axis scaling.

3. **Subscription & Paywall System (USDT BEP20)**
   - **Monthly Plan:** 9.99 USDT (30 days access)
   - **Yearly Plan:** 85 USDT (365 days access — Save 29%)
   - **Network:** USDT on **BNB Smart Chain (BEP20)**
   - **Receiving Wallet Address:** `0x736CC22F2b19adcAE6Ef4e5e13eab6BbF35B1Aab`
   - **Anti-Replay Security:** Each Transaction Hash (TxID) can only be verified once. Redemptions are validated against on-chain receipts and stored in a persistent local Room database table.
   - **Sandbox Mode:** Instant test activation mode allowing reviewers to test and evaluate the full unlocked Pro experience without sending mainnet funds.

4. **Multi-Language & RTL Support**
   - 6 Supported Languages:
     - 🇺🇸 **English** (LTR)
     - 🇮🇷 **Persian / Farsi (فارسی)** (RTL)
     - 🇸🇦 **Arabic (العربية)** (RTL)
     - 🇹🇷 **Turkish (Türkçe)** (LTR)
     - 🇷🇺 **Russian (Русский)** (LTR)
     - 🇩🇪 **German (Deutsch)** (LTR)
   - Automatic dynamic layout direction switching (`LayoutDirection.Rtl` vs `LayoutDirection.Ltr`).

5. **Contact Us & Support**
   - Official Support Email: `h.nasiri3247@gmail.com`
   - Direct Android email app intent launcher and feedback form.

---

## Configuration & Environment Variables

### 1. CoinMarketCap API Key (Optional)
If you have a CoinMarketCap Pro API Key:
- Go to the in-app **Settings** screen and enter your key in the **CoinMarketCap Pro API Key** field.
- Alternatively, declare it in `.env`:
  ```
  CMC_PRO_API_KEY=your_cmc_pro_api_key_here
  ```
- *Note:* If no key is provided, Rigour automatically uses high-speed public market feeds without any disruption.

### 2. BscScan API Key (Optional)
For high-rate on-chain transaction receipt querying:
- Enter your BscScan API Key in the **Settings** screen under **BscScan API Key**.
- If left blank, public RPC nodes (`bsc-dataseed1.binance.org`, `binance.llamarpc.com`) are utilized automatically.

---

## Architecture
- **Language:** Kotlin 2.2
- **UI Toolkit:** Jetpack Compose + Material 3 (Obsidian Dark Theme)
- **Local Persistence:** Room Database + KSP
- **Networking:** OkHttp & Retrofit
- **Concurrency:** Kotlin Coroutines & StateFlow
