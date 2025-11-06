package info.bitrich.xchangestream.bitget;

import info.bitrich.xchangestream.bitget.dto.common.BitgetChannel.ChannelType;
import info.bitrich.xchangestream.bitget.dto.common.BitgetChannel.MarketType;
import info.bitrich.xchangestream.bitget.dto.response.BitgetFuturesTickerNotification;
import info.bitrich.xchangestream.bitget.dto.response.BitgetTickerNotification;
import info.bitrich.xchangestream.bitget.dto.response.BitgetWsOrderBookSnapshotNotification;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.instrument.Instrument;

public class BitgetStreamingMarketDataService implements StreamingMarketDataService {

  private final BitgetStreamingService service;

  public BitgetStreamingMarketDataService(BitgetStreamingService service) {
    this.service = service;
  }

  /**
   * @param currencyPair Currency pair of the order book
   * @param args Order book level: {@link Integer} 1, 5 or 15
   */
  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
    Integer orderBookLevel = (Integer) ArrayUtils.get(args, 0, null);
    Validate.notNull(orderBookLevel, "Not implemented");
    Validate.inclusiveBetween(1, 15, orderBookLevel, "Not implemented");

    ChannelType channelType;
    if (orderBookLevel == 1) {
      channelType = ChannelType.DEPTH1;
    } else if (orderBookLevel <= 5) {
      channelType = ChannelType.DEPTH5;
    } else {
      channelType = ChannelType.DEPTH15;
    }

    return service
        .subscribeChannel(null, channelType, MarketType.SPOT, currencyPair)
        .map(BitgetWsOrderBookSnapshotNotification.class::cast)
        .map(notification -> BitgetStreamingAdapters.toOrderBook(notification, currencyPair));
  }

  @Override
  public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
    return service
        .subscribeChannel(null, ChannelType.TICKER, MarketType.SPOT, currencyPair)
        .map(BitgetTickerNotification.class::cast)
        .map(BitgetStreamingAdapters::toTicker);
  }

  @Override
  public Observable<FundingRate> getFundingRate(Instrument instrument, Object... args) {
    // Default to USDT-FUTURES if no market type specified
    MarketType marketType =
        (MarketType) ArrayUtils.get(args, 0, MarketType.USDT_FUTURES);

    return service
        .subscribeChannel(null, ChannelType.TICKER, marketType, instrument)
        .map(BitgetFuturesTickerNotification.class::cast)
        .map(notification -> BitgetStreamingAdapters.toFundingRate(notification, instrument))
        .filter(fundingRate -> fundingRate != null);
  }
}
