# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

XChange is a Java library providing a consistent API for interacting with 60+ cryptocurrency exchanges. It supports both REST (polling) and WebSocket (streaming) APIs using a modular, plugin-based architecture.

**License:** MIT
**Java Version:** 11
**Build Tool:** Maven

## Build & Test Commands

```bash
# Clean build and run unit tests
mvn clean test

# Run unit and integration tests (integration tests disabled by default)
mvn clean verify -DskipIntegrationTests=false

# Install to local Maven repository
mvn clean install

# Format code (Google Java Format)
mvn com.spotify.fmt:fmt-maven-plugin:format

# Format and organize POM files
mvn com.github.ekryd.sortpom:sortpom-maven-plugin:sort

# Generate JavaDocs
mvn javadoc:aggregate

# Check for dependency updates
mvn versions:display-dependency-updates

# Run tests for a single module
mvn clean test -pl xchange-binance

# Build a single module (with dependencies)
mvn clean install -pl xchange-binance -am
```

## Architecture Overview

### Modular Structure

The codebase follows a **plugin-based architecture** with clear separation:

- **xchange-core**: Foundation layer with Exchange abstraction, service interfaces, common DTOs, and client infrastructure
- **xchange-{exchange}**: Exchange-specific implementations (60+ modules)
- **xchange-stream-core**: Streaming/WebSocket foundation using RxJava
- **xchange-stream-{exchange}**: Exchange-specific streaming implementations
- **xchange-examples**: Example code demonstrating library usage

### Core Abstractions

**Exchange**: Main entry point (`org.knowm.xchange.Exchange`)
- Created via `ExchangeFactory.INSTANCE.createExchange(ExchangeClass.class)`
- Provides three service interfaces:
  - `MarketDataService` - Public market data (tickers, order books, trades)
  - `TradeService` - Order management (place, cancel, query orders)
  - `AccountService` - Account operations (balances, deposits, withdrawals)
- `getExchangeMetaData()` returns trading rules, fees, and limits
- `remoteInit()` loads remote metadata from exchange API

**ExchangeSpecification**: Configuration object
- Contains API credentials (apiKey, secretKey, userName, password)
- URIs (sslUri, plainTextUri, overrideWebsocketApiUri)
- Timeouts, rate limits, and exchange-specific parameters

**Service Pattern**: All services extend `BaseService`
- Default implementations throw `NotYetImplementedForExchangeException`
- Exchange modules override only implemented methods
- "Raw" service classes provide direct API access
- High-level service classes use adapters for DTO conversion

### Key Design Patterns

1. **Factory Pattern**: `ExchangeFactory` singleton creates exchange instances
2. **Strategy Pattern**: Pluggable service implementations per exchange
3. **Adapter Pattern**: Exchange-specific DTOs converted to common XChange DTOs (e.g., `BinanceAdapters.java`)
4. **Template Method**: `BaseExchange.initServices()` defines initialization flow
5. **Proxy Pattern**: RESCU library generates REST API proxies from JAX-RS annotated interfaces

### REST vs Streaming APIs

**REST API** (xchange-{exchange}):
- Synchronous, blocking HTTP calls
- Uses RESCU library with JAX-RS annotations (`@GET`, `@POST`, `@Path`)
- Returns data directly or throws `IOException`
- Example: `Ticker ticker = marketDataService.getTicker(CurrencyPair.BTC_USD);`

**Streaming API** (xchange-stream-{exchange}):
- Asynchronous, non-blocking WebSocket connections
- Uses RxJava 3 Observables for reactive streams
- `connect()` and `disconnect()` return `Completable`
- Service methods return `Observable<T>` that emit real-time updates
- Example: `exchange.getStreamingMarketDataService().getTicker(pair).subscribe(...)`

## Implementing New Exchange Support

### Standard Module Structure

```
xchange-{exchangename}/
├── src/main/java/org/knowm/xchange/{exchangename}/
│   ├── {Exchange}Exchange.java              # Main exchange class (extends BaseExchange)
│   ├── {Exchange}.java                       # Public REST API interface (JAX-RS annotated)
│   ├── {Exchange}Authenticated.java         # Private REST API interface
│   ├── {Exchange}Adapters.java              # DTO conversion utilities
│   └── service/
│       ├── {Exchange}BaseService.java       # Common service logic
│       ├── {Exchange}MarketDataService.java # Implements MarketDataService
│       ├── {Exchange}TradeService.java      # Implements TradeService
│       └── {Exchange}AccountService.java    # Implements AccountService
├── src/main/resources/
│   └── {exchangename}.json                   # Static metadata (optional)
└── src/test/
    └── java/org/knowm/xchange/{exchangename}/
        ├── service/                          # Service unit tests
        └── dto/                              # DTO/adapter tests
```

### Implementation Steps

1. **Create Exchange Class**
   - Extend `BaseExchange`
   - Implement `initServices()` to instantiate service implementations
   - Override `getDefaultExchangeSpecification()` with API URLs and exchange name

2. **Define REST API Interfaces**
   - Create public interface with `@Path` and `@GET/@POST` annotations
   - Create authenticated interface with signature parameters
   - Use `@QueryParam`, `@FormParam`, `@HeaderParam` for parameters

3. **Implement Services**
   - Extend `BaseExchangeService<{Exchange}Exchange>`
   - Create "Raw" methods that call REST API directly
   - Create high-level methods that use adapters

4. **Create Adapters**
   - Static utility class with conversion methods
   - Convert exchange DTOs ↔ XChange common DTOs
   - Handle symbol mappings (e.g., "BTCUSDT" → "BTC/USDT")

5. **Authentication Implementation**
   - Extend `BaseParamsDigest` for signature generation (HMAC-SHA256, etc.)
   - Implement exchange-specific signing logic
   - Use `@HeaderParam(X_API_KEY)` and `@QueryParam(SIGNATURE)` patterns

6. **Metadata Management**
   - Create `{exchangename}.json` in resources with static metadata
   - OR implement `remoteInit()` to fetch metadata dynamically
   - Include instrument metadata (min/max amounts, price scales, fees)

7. **Testing**
   - Unit tests for adapters and DTOs
   - Integration tests (name as `*Integration.java`, disabled by default)
   - Use sandbox/testnet APIs when available

### Important Conventions

- **Symbol Mapping**: Exchange symbols differ from XChange pairs; handle in adapters
- **Nonce Handling**: Most exchanges use timestamps; override `getNonceFactory()` if needed
- **Rate Limiting**: Implement using Resilience4j registries from `ExchangeSpecification`
- **Error Handling**: Convert exchange errors to XChange exception hierarchy
- **Optional Parameters**: Use `Params` pattern for exchange-specific parameters
- **Partial Implementation**: Only implement supported operations; others throw `NotYetImplementedForExchangeException`

## Project Structure Details

### xchange-core Key Packages

- `org.knowm.xchange` - Core interfaces: Exchange, ExchangeFactory, ExchangeSpecification
- `org.knowm.xchange.service.*` - Service interfaces and base implementations
- `org.knowm.xchange.dto.*` - Common DTOs (marketdata, trade, account, meta)
- `org.knowm.xchange.currency` - Currency and CurrencyPair definitions
- `org.knowm.xchange.instrument` - Instrument abstractions (spot, futures, options)
- `org.knowm.xchange.client` - REST client infrastructure, proxy builder
- `org.knowm.xchange.exceptions` - Exception hierarchy
- `org.knowm.xchange.utils` - Helper utilities (nonce, assertions, etc.)

### Common DTOs Location

All in `xchange-core/src/main/java/org/knowm/xchange/dto/`:
- **marketdata/**: Ticker, OrderBook, Trade, FundingRate
- **trade/**: LimitOrder, MarketOrder, StopOrder, UserTrades, OpenOrders
- **account/**: AccountInfo, Balance, Wallet, FundingRecord
- **meta/**: ExchangeMetaData, InstrumentMetaData, CurrencyMetaData

### Service Files

- `org.knowm.xchange.service.marketdata.MarketDataService`
- `org.knowm.xchange.service.trade.TradeService`
- `org.knowm.xchange.service.account.AccountService`

### Exchange Factory

- `org.knowm.xchange.ExchangeFactory` - Singleton enum pattern for creating exchanges

## Code Style

The project uses **Google Java Format** via `fmt-maven-plugin`. Run formatting before committing:

```bash
mvn com.spotify.fmt:fmt-maven-plugin:format
```

POM files are formatted with `sortpom-maven-plugin`:

```bash
mvn com.github.ekryd.sortpom:sortpom-maven-plugin:sort
```

## Integration Tests

Integration tests are disabled by default (`skipIntegrationTests=true`) as they require API keys and hit live exchanges. Name integration tests with `*Integration.java` suffix.

To run integration tests:
```bash
mvn clean verify -DskipIntegrationTests=false
```

## Dependencies

Key dependencies (managed in parent POM):
- **RESCU** (3.1) - REST client library with JAX-RS annotation support
- **RxJava 3** (3.1.12) - Reactive streams for WebSocket APIs
- **Jackson** (2.19.1) - JSON serialization/deserialization
- **SLF4J** (2.0.17) + Logback (1.5.18) - Logging
- **Netty** (4.2.7.Final) - WebSocket transport
- **JUnit 5** (5.12.2) + Mockito (5.18.0) - Testing
- **Resilience4j** (1.7.1) - Rate limiting and retry logic
- **Lombok** (1.18.42) - Boilerplate reduction

## Working with Examples

The `xchange-examples` module contains runnable examples for each exchange:
```
xchange-examples/src/main/java/org/knowm/xchange/examples/{exchange}/
```

Examples demonstrate:
- Public market data access
- Authenticated trading operations
- Streaming API usage
- Metadata queries

Run examples from your IDE or via Maven exec plugin.
