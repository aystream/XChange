package info.bitrich.xchangestream.gateio;

import info.bitrich.xchangestream.gateio.dto.response.balance.BalancePayload;
import info.bitrich.xchangestream.gateio.dto.response.balance.GateioSingleSpotBalanceNotification;
import info.bitrich.xchangestream.gateio.dto.response.orderbook.GateioOrderBookNotification;
import info.bitrich.xchangestream.gateio.dto.response.orderbook.OrderBookPayload;
import info.bitrich.xchangestream.gateio.dto.response.ticker.FuturesTickerPayload;
import info.bitrich.xchangestream.gateio.dto.response.ticker.GateioFuturesTickerNotification;
import info.bitrich.xchangestream.gateio.dto.response.ticker.GateioTickerNotification;
import info.bitrich.xchangestream.gateio.dto.response.ticker.TickerPayload;
import info.bitrich.xchangestream.gateio.dto.response.trade.GateioTradeNotification;
import info.bitrich.xchangestream.gateio.dto.response.trade.TradePayload;
import info.bitrich.xchangestream.gateio.dto.response.usertrade.GateioSingleUserTradeNotification;
import info.bitrich.xchangestream.gateio.dto.response.usertrade.UserTradePayload;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.instrument.Instrument;

@UtilityClass
public class GateioStreamingAdapters {

  public Ticker toTicker(GateioTickerNotification notification) {
    TickerPayload tickerPayload = notification.getResult();

    return new Ticker.Builder()
        .timestamp(Date.from(notification.getTimeMs()))
        .instrument(tickerPayload.getCurrencyPair())
        .last(tickerPayload.getLastPrice())
        .ask(tickerPayload.getLowestAsk())
        .bid(tickerPayload.getHighestBid())
        .percentageChange(tickerPayload.getChangePercent24h())
        .volume(tickerPayload.getBaseVolume())
        .quoteVolume(tickerPayload.getQuoteVolume())
        .high(tickerPayload.getHighPrice24h())
        .low(tickerPayload.getLowPrice24h())
        .build();
  }

  public Trade toTrade(GateioTradeNotification notification) {
    TradePayload tradePayload = notification.getResult();

    return Trade.builder()
        .type(tradePayload.getSide())
        .originalAmount(tradePayload.getAmount())
        .instrument(tradePayload.getCurrencyPair())
        .price(tradePayload.getPrice())
        .timestamp(Date.from(tradePayload.getTimeMs()))
        .id(String.valueOf(tradePayload.getId()))
        .build();
  }

  public UserTrade toUserTrade(GateioSingleUserTradeNotification notification) {
    UserTradePayload userTradePayload = notification.getResult();

    return UserTrade.builder()
        .type(userTradePayload.getSide())
        .originalAmount(userTradePayload.getAmount())
        .instrument(userTradePayload.getCurrencyPair())
        .price(userTradePayload.getPrice())
        .timestamp(Date.from(userTradePayload.getTimeMs()))
        .id(String.valueOf(userTradePayload.getId()))
        .orderId(String.valueOf(userTradePayload.getOrderId()))
        .feeAmount(userTradePayload.getFee())
        .feeCurrency(userTradePayload.getFeeCurrency())
        .orderUserReference(userTradePayload.getRemark())
        .build();
  }

  public Balance toBalance(GateioSingleSpotBalanceNotification notification) {
    BalancePayload balancePayload = notification.getResult();

    return new Balance.Builder()
        .currency(balancePayload.getCurrency())
        .total(balancePayload.getTotal())
        .available(balancePayload.getAvailable())
        .frozen(balancePayload.getFreeze())
        .timestamp(Date.from(balancePayload.getTimeMs()))
        .build();
  }

  public OrderBook toOrderBook(GateioOrderBookNotification notification) {
    OrderBookPayload orderBookPayload = notification.getResult();

    Stream<LimitOrder> asks =
        orderBookPayload.getAsks().stream()
            .map(
                priceSizeEntry ->
                    new LimitOrder(
                        OrderType.ASK,
                        priceSizeEntry.getSize(),
                        orderBookPayload.getCurrencyPair(),
                        null,
                        null,
                        priceSizeEntry.getPrice()));

    Stream<LimitOrder> bids =
        orderBookPayload.getBids().stream()
            .map(
                priceSizeEntry ->
                    new LimitOrder(
                        OrderType.BID,
                        priceSizeEntry.getSize(),
                        orderBookPayload.getCurrencyPair(),
                        null,
                        null,
                        priceSizeEntry.getPrice()));

    return new OrderBook(Date.from(orderBookPayload.getTimestamp()), asks, bids);
  }

  public static Instrument contractToInstrument(String contract) {
    if (contract == null) {
      return null;
    }
    // Gate.io contract format: BTC_USDT, BTC_USD
    String[] parts = contract.split("_");
    if (parts.length == 2) {
      CurrencyPair pair = new CurrencyPair(parts[0], parts[1]);
      return new FuturesContract(pair, "PERP");
    }
    return null;
  }

  public FundingRate toFundingRate(GateioFuturesTickerNotification notification) {
    FuturesTickerPayload payload = notification.getResult();

    if (payload.getFundingRate() == null || payload.getContract() == null) {
      return null;
    }

    Instrument instrument = contractToInstrument(payload.getContract());
    if (instrument == null) {
      return null;
    }

    // Gate.io provides 8-hour funding rate, convert to 1-hour rate
    BigDecimal fundingRate8h = payload.getFundingRate();
    BigDecimal fundingRate1h =
        fundingRate8h.divide(
            BigDecimal.valueOf(8), fundingRate8h.scale() + 3, RoundingMode.HALF_EVEN);

    // Estimate next funding time (Gate.io funding happens every 8 hours at 00:00, 08:00, 16:00 UTC)
    long now = System.currentTimeMillis();
    long eightHoursMs = 8 * 60 * 60 * 1000;
    long nextFundingMs = ((now / eightHoursMs) + 1) * eightHoursMs;
    Date nextFundingTime = new Date(nextFundingMs);

    long effectiveInMinutes = (nextFundingMs - now) / (1000 * 60);

    return new FundingRate.Builder()
        .instrument(instrument)
        .fundingRate1h(fundingRate1h)
        .fundingRate8h(fundingRate8h)
        .fundingRateDate(nextFundingTime)
        .fundingRateEffectiveInMinutes(effectiveInMinutes)
        .build();
  }
}
