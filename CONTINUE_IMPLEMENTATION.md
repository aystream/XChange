# Quick Start Guide - Continue Funding Rate Implementation

## 🎯 Current Status
**Completed:** Bybit (REST+WS), Bitget (WS only)
**Remaining:** Bitget REST, Gate.io (REST+WS), MEXC (REST only)

---

## 📋 Step-by-Step Continuation Plan

### Step 1: Complete Bitget REST API (~30 min)

#### Files to Modify:

1. **Create DTO wrapper class** (if needed):
```java
// xchange-bitget/src/main/java/org/knowm/xchange/bitget/dto/marketdata/BitgetFundingRateResponse.java
@Data
@Builder
@Jacksonized
public class BitgetFundingRateResponse {
    @JsonProperty("code")
    private String code;

    @JsonProperty("data")
    private List<BitgetFuturesTickerDto> data;
}
```

2. **Add raw service methods:**
```java
// xchange-bitget/src/main/java/org/knowm/xchange/bitget/service/BitgetMarketDataServiceRaw.java
public BitgetFundingRateResponse getBitgetFundingRates(String productType) throws IOException {
    // GET /api/v2/mix/market/current-fund-rate?productType=usdt-futures
}

public BitgetFundingRateResponse getBitgetFundingRate(String symbol, String productType) throws IOException {
    // GET /api/v2/mix/market/current-fund-rate?symbol={symbol}&productType={productType}
}
```

3. **Add adapter methods:**
```java
// xchange-bitget/src/main/java/org/knowm/xchange/bitget/BitgetAdapters.java
public static FundingRate adaptFundingRate(BitgetFuturesTickerDto dto) {
    // Convert similar to Bybit/Bitget streaming adapter
    // 8h -> 1h conversion
    // Parse nextFundingTime from string to Date
}

public static FundingRates adaptFundingRates(List<BitgetFuturesTickerDto> dtos) {
    // Map and collect
}
```

4. **Implement service methods:**
```java
// xchange-bitget/src/main/java/org/knowm/xchange/bitget/service/BitgetMarketDataService.java
@Override
public FundingRate getFundingRate(Instrument instrument) throws IOException {
    // Call raw service
    // Adapt single result
}

@Override
public FundingRates getFundingRates() throws IOException {
    // Call raw service with null symbol
    // Adapt all results
}
```

---

### Step 2: Implement Gate.io REST+WebSocket (~1.5 hours)

#### REST API:

1. **Create DTO:**
```java
// xchange-gateio-v4/src/main/java/org/knowm/xchange/gateio/dto/marketdata/GateioFundingRate.java
@Data
@Builder
@Jacksonized
public class GateioFundingRate {
    @JsonProperty("t")
    private Long timestamp;

    @JsonProperty("r")
    private BigDecimal rate;

    @JsonProperty("contract")
    private String contract;
}
```

2. **Add to API interface:**
```java
// xchange-gateio-v4/src/main/java/org/knowm/xchange/gateio/GateioV4.java
@GET
@Path("/futures/{settle}/funding_rate")
List<GateioFundingRate> getFundingRates(
    @PathParam("settle") String settle,
    @QueryParam("contract") String contract
) throws IOException;
```

3. **Implement service + adapters** (similar pattern to Bybit)

#### WebSocket API:

1. **Modify streaming service:**
```java
// xchange-stream-gateio/src/main/java/info/bitrich/xchangestream/gateio/GateioStreamingMarketDataService.java
@Override
public Observable<FundingRate> getFundingRate(Instrument instrument, Object... args) {
    String settle = (String) ArrayUtils.get(args, 0, "usdt");

    return service
        .subscribeChannel("futures.tickers", settle, instrument)
        .map(...)
        .map(GateioStreamingAdapters::toFundingRate);
}
```

2. **Create WebSocket DTO** for futures ticker with funding_rate field

3. **Add adapter method** in `GateioStreamingAdapters`

---

### Step 3: Implement MEXC REST API (~45 min)

**Note:** No WebSocket (module doesn't exist)

1. **Create DTO:**
```java
// xchange-mexc/src/main/java/org/knowm/xchange/mexc/dto/marketdata/MEXCFundingRate.java
@Data
@Builder
@Jacksonized
public class MEXCFundingRate {
    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("fundingRate")
    private BigDecimal fundingRate;

    @JsonProperty("nextSettleTime")
    private Long nextSettleTime;

    @JsonProperty("timestamp")
    private Long timestamp;
}
```

2. **Add API interface method** (check if `MEXC.java` or similar exists)

3. **Implement service + adapters** (standard pattern)

---

### Step 4: Create Examples (~30 min)

Create in `xchange-examples/src/main/java/org/knowm/xchange/examples/`:

#### REST Examples:
- `bybit/market/BybitFundingRateExample.java`
- `bitget/market/BitgetFundingRateExample.java`
- `gateio/market/GateioFundingRateExample.java`
- `mexc/market/MEXCFundingRateExample.java`

#### WebSocket Examples:
- `bybit/streaming/BybitStreamingFundingRateExample.java`
- `bitget/streaming/BitgetStreamingFundingRateExample.java`
- `gateio/streaming/GateioStreamingFundingRateExample.java`

See templates in `FUNDING_RATE_IMPLEMENTATION.md`

---

## 🔍 Quick Reference

### Exchange API Endpoints:

**Bitget REST:**
```
GET https://api.bitget.com/api/v2/mix/market/current-fund-rate
Params: symbol (optional), productType (usdt-futures/coin-futures/usdc-futures)
```

**Gate.io REST:**
```
GET https://fx-api.gateio.ws/api/v4/futures/{settle}/funding_rate
Params: settle (usdt/btc), contract (required)
```

**Gate.io WebSocket:**
```
wss://fx-ws.gateio.ws/v4/ws/{settle}
Channel: futures.tickers
Fields: funding_rate, funding_rate_indicative
```

**MEXC REST:**
```
GET https://contract.mexc.com/api/v1/contract/funding_rate/{symbol}
Response: fundingRate, nextSettleTime, timestamp
```

---

## 🧪 Testing Commands

```bash
# Compile all modified modules
mvn clean compile -pl xchange-bybit,xchange-bitget,xchange-gateio-v4,xchange-mexc -am

# Compile streaming modules
mvn clean compile -pl xchange-stream-bybit,xchange-stream-bitget,xchange-stream-gateio -am

# Run tests (skip integration tests by default)
mvn test -pl xchange-bybit,xchange-bitget,xchange-gateio-v4,xchange-mexc

# Format code
mvn com.spotify.fmt:fmt-maven-plugin:format

# Full build
mvn clean install -DskipTests
```

---

## 📝 Checklist

### Bitget REST
- [ ] Create/verify DTO for funding rate response
- [ ] Add raw service methods
- [ ] Add adapter methods
- [ ] Implement `getFundingRate(Instrument)`
- [ ] Implement `getFundingRates()`
- [ ] Test compilation

### Gate.io Full
- [ ] Create funding rate DTO
- [ ] Add REST API interface method
- [ ] Add raw service methods
- [ ] Add REST adapters
- [ ] Implement REST service methods
- [ ] Create WebSocket DTO
- [ ] Add WebSocket `getFundingRate()` method
- [ ] Add WebSocket adapter
- [ ] Test compilation

### MEXC REST
- [ ] Create funding rate DTO
- [ ] Add/verify API interface
- [ ] Add raw service methods
- [ ] Add adapter methods
- [ ] Implement service methods
- [ ] Test compilation

### Examples
- [ ] Create 4 REST examples
- [ ] Create 3 WebSocket examples
- [ ] Test all examples compile

### Final
- [ ] Run full build: `mvn clean install -DskipTests`
- [ ] Format code: `mvn fmt:format`
- [ ] Update CLAUDE.md with funding rate info
- [ ] Create PR with detailed description

---

## 🚀 Quick Commands for New Session

```bash
# Navigate to project
cd /Users/alex/Documents/Projects/XChange

# Read implementation status
cat FUNDING_RATE_IMPLEMENTATION.md

# Start with Bitget REST
# Edit: xchange-bitget/src/main/java/org/knowm/xchange/bitget/service/BitgetMarketDataService.java
```

---

## 📞 Key Files Reference

### Already Modified (don't break these):
- `xchange-bybit/src/main/java/org/knowm/xchange/bybit/BybitAdapters.java` (lines 575-611)
- `xchange-bybit/src/main/java/org/knowm/xchange/bybit/service/BybitMarketDataService.java` (lines 108-174)
- `xchange-stream-bybit/.../BybitStreamingMarketDataService.java` (lines 193-211)
- `xchange-stream-bybit/.../BybitStreamingAdapters.java` (lines 324-348)
- `xchange-stream-bitget/.../BitgetChannel.java` (added futures types)
- `xchange-stream-bitget/.../BitgetStreamingMarketDataService.java` (lines 59-70)
- `xchange-stream-bitget/.../BitgetStreamingAdapters.java` (lines 143-171)

### Already Created (can reference):
- `xchange-bitget/.../BitgetFuturesTickerDto.java`
- `xchange-stream-bitget/.../BitgetFuturesTickerNotification.java`

---

**Good luck! You've got 50% done, 50% to go! 💪**
