package info.bitrich.xchangestream.gateio;

import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.gateio.config.Config;
import info.bitrich.xchangestream.gateio.dto.response.orderbook.GateioOrderBookNotification;
import info.bitrich.xchangestream.gateio.dto.response.ticker.GateioFuturesTickerNotification;
import info.bitrich.xchangestream.gateio.dto.response.ticker.GateioTickerNotification;
import info.bitrich.xchangestream.gateio.dto.response.trade.GateioTradeNotification;
import io.reactivex.rxjava3.core.Observable;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.ArrayUtils;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.gateio.GateioExchange;
import org.knowm.xchange.gateio.service.GateioMarketDataService;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GateioStreamingMarketDataService implements StreamingMarketDataService {

  private static final Logger LOG = LoggerFactory.getLogger(GateioStreamingMarketDataService.class);
  public static final int MAX_DEPTH_DEFAULT = 5;
  public static final int UPDATE_INTERVAL_DEFAULT = 100;
  private final GateioStreamingService service;
  private final GateioExchange exchange;
  private final ConcurrentMap<String, Date> nextFundingTimeCache = new ConcurrentHashMap<>();

  public GateioStreamingMarketDataService(
      GateioStreamingService service, GateioExchange exchange) {
    this.service = service;
    this.exchange = exchange;
    // Pre-fetch all funding times in one batch REST call
    prefetchAllFundingTimes();
  }

  /**
   * Pre-fetch all funding times from REST API in a single batch call.
   * This avoids blocking WebSocket subscriptions with hundreds of sequential REST calls.
   */
  private void prefetchAllFundingTimes() {
    try {
      LOG.info("Pre-fetching all Gate.io funding times via batch REST API call...");
      GateioMarketDataService marketDataService =
          (GateioMarketDataService) exchange.getMarketDataService();
      // Single batch call to get ALL USDT-settled funding rates at once
      org.knowm.xchange.dto.marketdata.FundingRates allRates = marketDataService.getFundingRates();

      if (allRates != null && allRates.getFundingRates() != null) {
        for (org.knowm.xchange.dto.marketdata.FundingRate rate : allRates.getFundingRates()) {
          Instrument instrument = rate.getInstrument();
          String contract = instrument.getBase().getCurrencyCode() + "_" + instrument.getCounter().getCurrencyCode();
          String cacheKey = "usdt:" + contract;
          if (rate.getFundingRateDate() != null) {
            nextFundingTimeCache.put(cacheKey, rate.getFundingRateDate());
          }
        }
        LOG.info("Successfully pre-fetched {} funding times from Gate.io REST API in one batch call",
                 nextFundingTimeCache.size());
      }
    } catch (org.knowm.xchange.exceptions.ExchangeException e) {
      LOG.warn("Gate.io REST API returned error ({}): {}. Using local calculation fallback instead.",
               e.getClass().getSimpleName(), e.getMessage());
      // API may be temporarily unavailable - fall back to local calculation in adapter
    } catch (Exception e) {
      LOG.warn("Failed to pre-fetch funding times from Gate.io REST API: {} - {}. Will use local calculation fallback.",
               e.getClass().getSimpleName(), e.getMessage());
      // If batch REST call fails, we'll fall back to local calculation in adapter
    }
  }

  /**
   * Uses the limited-level snapshot method:
   * https://www.gate.io/docs/apiv4/ws/index.html#limited-level-full-order-book-snapshot
   *
   * @param currencyPair Currency pair of the order book
   * @param args Order book level: {@link Integer}, update speed: {@link Duration}
   */
  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
    Integer orderBookLevel = (Integer) ArrayUtils.get(args, 0, MAX_DEPTH_DEFAULT);
    Duration updateSpeed = (Duration) ArrayUtils.get(args, 1, UPDATE_INTERVAL_DEFAULT);
    return service
        .subscribeChannel(
            Config.SPOT_ORDERBOOK_CHANNEL, new Object[] {currencyPair, orderBookLevel, updateSpeed})
        .map(GateioOrderBookNotification.class::cast)
        .map(GateioStreamingAdapters::toOrderBook);
  }

  @Override
  public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
    return service
        .subscribeChannel(Config.SPOT_TICKERS_CHANNEL, currencyPair)
        .map(GateioTickerNotification.class::cast)
        .map(GateioStreamingAdapters::toTicker);
  }

  @Override
  public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
    return service
        .subscribeChannel(Config.SPOT_TRADES_CHANNEL, currencyPair)
        .map(GateioTradeNotification.class::cast)
        .map(GateioStreamingAdapters::toTrade);
  }

  @Override
  public Observable<FundingRate> getFundingRate(Instrument instrument, Object... args) {
    // First arg is settle type (e.g., "usdt", "btc"), defaults to "usdt"
    String settle = (String) ArrayUtils.get(args, 0, "usdt");

    // Create subscription parameters: [settle, contract]
    // Contract format: BTC_USDT
    String contract =
        instrument.getBase().getCurrencyCode() + "_" + instrument.getCounter().getCurrencyCode();
    Object[] params = new Object[] {settle, contract};

    // Use pre-fetched funding time from cache (populated in constructor)
    // If not in cache, adapter will fall back to local calculation
    String cacheKey = settle + ":" + contract;

    return service
        .subscribeChannel(Config.FUTURES_TICKERS_CHANNEL, params)
        .map(GateioFuturesTickerNotification.class::cast)
        .map(notification -> GateioStreamingAdapters.toFundingRate(notification, nextFundingTimeCache.get(cacheKey)));
  }

  public Date getCachedNextFundingTime(String settle, String contract) {
    return nextFundingTimeCache.get(settle + ":" + contract);
  }
}
