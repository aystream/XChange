package info.bitrich.xchangestream.mexc;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.MEXCWebSocketMessage;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.rxjava3.core.Observable;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.mexc.MEXCExchange;
import org.knowm.xchange.mexc.service.MEXCMarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEXCStreamingMarketDataService implements StreamingMarketDataService {

  private final Logger LOG = LoggerFactory.getLogger(MEXCStreamingMarketDataService.class);
  private final MEXCStreamingService streamingService;
  private final MEXCExchange exchange;
  private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();
  private final ConcurrentMap<String, Date> nextFundingTimeCache = new ConcurrentHashMap<>();

  public static final String TICKER = "push.ticker";

  public MEXCStreamingMarketDataService(MEXCStreamingService streamingService, MEXCExchange exchange) {
    this.streamingService = streamingService;
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
      LOG.info("Pre-fetching all MEXC funding times via batch REST API call...");
      MEXCMarketDataService marketDataService =
          (MEXCMarketDataService) exchange.getMarketDataService();
      // Single batch call to get ALL funding rates at once
      org.knowm.xchange.dto.marketdata.FundingRates allRates = marketDataService.getFundingRates();

      if (allRates != null && allRates.getFundingRates() != null) {
        for (org.knowm.xchange.dto.marketdata.FundingRate rate : allRates.getFundingRates()) {
          String symbol = MEXCStreamAdapters.instrumentToSymbol(rate.getInstrument());
          if (rate.getFundingRateDate() != null) {
            nextFundingTimeCache.put(symbol, rate.getFundingRateDate());
          }
        }
        LOG.info("Successfully pre-fetched {} funding times from MEXC REST API in one batch call",
                 nextFundingTimeCache.size());
      }
    } catch (org.knowm.xchange.exceptions.ExchangeException e) {
      LOG.warn("MEXC REST API returned error ({}): {}. Using local calculation fallback instead.",
               e.getClass().getSimpleName(), e.getMessage());
      // IP may be blocked or rate limited - fall back to local calculation in adapter
    } catch (Exception e) {
      LOG.warn("Failed to pre-fetch funding times from MEXC REST API: {} - {}. Will use local calculation fallback.",
               e.getClass().getSimpleName(), e.getMessage());
      // If batch REST call fails, we'll fall back to local calculation in adapter
    }
  }

  @Override
  public Observable<FundingRate> getFundingRate(Instrument instrument, Object... args) {
    String symbol = MEXCStreamAdapters.instrumentToSymbol(instrument);
    String channelUniqueId = TICKER + "." + symbol;

    // Use pre-fetched funding time from cache (populated in constructor)
    // If not in cache, adapter will fall back to local calculation
    return streamingService
        .subscribeChannel(channelUniqueId)
        .filter(message -> message.has("data"))
        .map(
            jsonNode -> {
              try {
                MEXCWebSocketMessage wsMessage =
                    mapper.treeToValue(jsonNode, MEXCWebSocketMessage.class);
                Date cachedNextFundingTime = nextFundingTimeCache.get(symbol);
                return MEXCStreamAdapters.adaptFundingRate(wsMessage, cachedNextFundingTime);
              } catch (Exception e) {
                LOG.error("Error parsing MEXC ticker message: {}", e.getMessage());
                return null;
              }
            })
        .filter(fundingRate -> fundingRate != null);
  }

  public Date getCachedNextFundingTime(String symbol) {
    return nextFundingTimeCache.get(symbol);
  }
}
