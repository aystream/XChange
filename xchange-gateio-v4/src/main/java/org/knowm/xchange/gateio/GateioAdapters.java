package org.knowm.xchange.gateio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.InstrumentMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.gateio.dto.account.GateioAccountBookRecord;
import org.knowm.xchange.gateio.dto.account.GateioOrder;
import org.knowm.xchange.gateio.dto.account.GateioWithdrawalRequest;
import org.knowm.xchange.gateio.dto.marketdata.GateioCurrencyPairDetails;
import org.knowm.xchange.gateio.dto.marketdata.GateioFundingRate;
import org.knowm.xchange.gateio.dto.marketdata.GateioOrderBook;
import org.knowm.xchange.gateio.dto.marketdata.GateioTicker;
import org.knowm.xchange.gateio.dto.trade.GateioUserTrade;
import org.knowm.xchange.gateio.dto.trade.GateioUserTradeRaw;
import org.knowm.xchange.gateio.service.params.GateioWithdrawFundsParams;
import org.knowm.xchange.instrument.Instrument;

@UtilityClass
public class GateioAdapters {

  public final BigDecimal PARTIALLY_FILLED_SCALE = new BigDecimal("0.1");

  public String toString(Instrument instrument) {
    if (instrument == null) {
      return null;
    } else {
      return String.format(
              "%s_%s",
              instrument.getBase().getCurrencyCode(), instrument.getCounter().getCurrencyCode())
          .toUpperCase(Locale.ROOT);
    }
  }

  public OrderBook toOrderBook(GateioOrderBook gateioOrderBook, Instrument instrument) {
    List<LimitOrder> asks =
        gateioOrderBook.getAsks().stream()
            .map(
                priceSizeEntry ->
                    new LimitOrder(
                        OrderType.ASK,
                        priceSizeEntry.getSize(),
                        instrument,
                        null,
                        null,
                        priceSizeEntry.getPrice()))
            .collect(Collectors.toList());

    List<LimitOrder> bids =
        gateioOrderBook.getBids().stream()
            .map(
                priceSizeEntry ->
                    new LimitOrder(
                        OrderType.BID,
                        priceSizeEntry.getSize(),
                        instrument,
                        null,
                        null,
                        priceSizeEntry.getPrice()))
            .collect(Collectors.toList());

    return new OrderBook(Date.from(gateioOrderBook.getGeneratedAt()), asks, bids);
  }

  public InstrumentMetaData toInstrumentMetaData(
      GateioCurrencyPairDetails gateioCurrencyPairDetails) {
    return InstrumentMetaData.builder()
        .tradingFee(gateioCurrencyPairDetails.getFee())
        .minimumAmount(gateioCurrencyPairDetails.getMinAssetAmount())
        .counterMinimumAmount(gateioCurrencyPairDetails.getMinQuoteAmount())
        .volumeScale(gateioCurrencyPairDetails.getAssetScale())
        .priceScale(gateioCurrencyPairDetails.getQuoteScale())
        .build();
  }

  public String toString(OrderStatus orderStatus) {
    switch (orderStatus) {
      case OPEN:
        return "open";
      case CLOSED:
        return "finished";
      default:
        throw new IllegalArgumentException("Can't map " + orderStatus);
    }
  }

  public OrderStatus toOrderStatus(GateioOrder gateioOrder) {
    switch (gateioOrder.getStatus()) {
      case "open":
        return OrderStatus.OPEN;

      case "closed":
        // if more than `PARTIALLY_FILLED_SCALE` left to fill -> set to `PARTIALLY_FILLED`
        if (gateioOrder
                .getAmountLeftToFill()
                .compareTo(gateioOrder.getAmount().multiply(PARTIALLY_FILLED_SCALE))
            > 0) {
          return OrderStatus.PARTIALLY_FILLED;
        } else {
          return OrderStatus.FILLED;
        }

      case "filled":
        return OrderStatus.FILLED;

      case "cancelled":
      case "stp":
        return OrderStatus.CANCELED;

      default:
        throw new IllegalArgumentException("Can't map " + gateioOrder.getStatus());
    }
  }

  public GateioOrder toGateioOrder(MarketOrder marketOrder) {
    return GateioOrder.builder()
        .currencyPair((CurrencyPair) marketOrder.getInstrument())
        .side(marketOrder.getType())
        .clientOrderId(marketOrder.getUserReference())
        .account("spot")
        .type("market")
        .timeInForce("ioc")
        .amount(marketOrder.getOriginalAmount())
        .build();
  }

  public GateioOrder toGateioOrder(LimitOrder limitOrder) {
    return GateioOrder.builder()
        .currencyPair((CurrencyPair) limitOrder.getInstrument())
        .side(limitOrder.getType())
        .clientOrderId(limitOrder.getUserReference())
        .account("spot")
        .type("limit")
        .timeInForce("gtc")
        .price(limitOrder.getLimitPrice())
        .amount(limitOrder.getOriginalAmount())
        .build();
  }

  public Order toOrder(GateioOrder gateioOrder) {
    Order.Builder builder;
    Instrument instrument = gateioOrder.getCurrencyPair();
    OrderType orderType = gateioOrder.getSide();

    switch (gateioOrder.getType()) {
      case "market":
        builder = new MarketOrder.Builder(orderType, instrument);
        break;
      case "limit":
        builder = new LimitOrder.Builder(orderType, instrument).limitPrice(gateioOrder.getPrice());
        break;
      default:
        throw new IllegalArgumentException("Can't map " + gateioOrder.getType());
    }

    // if filled then calculate amounts
    OrderStatus status = toOrderStatus(gateioOrder);

    if (status == OrderStatus.FILLED || status == OrderStatus.PARTIALLY_FILLED) {
      if (orderType == OrderType.BID) {
        builder.cumulativeAmount(gateioOrder.getFilledTotalQuote());
      } else if (orderType == OrderType.ASK) {
        BigDecimal filledAssetAmount =
            gateioOrder
                .getFilledTotalQuote()
                .divide(gateioOrder.getAvgDealPrice(), MathContext.DECIMAL32);
        builder.cumulativeAmount(filledAssetAmount);
      } else {
        throw new IllegalArgumentException("Can't map " + orderType);
      }
    }

    return builder
        .id(gateioOrder.getId())
        .originalAmount(gateioOrder.getAmount())
        .userReference(gateioOrder.getClientOrderId())
        .timestamp(Date.from(gateioOrder.getCreatedAt()))
        .orderStatus(status)
        .averagePrice(gateioOrder.getAvgDealPrice())
        .fee(gateioOrder.getFee())
        .build();
  }

  public UserTrade toUserTrade(GateioUserTradeRaw gateioUserTradeRaw) {
    return GateioUserTrade.builder()
        .type(gateioUserTradeRaw.getSide())
        .originalAmount(gateioUserTradeRaw.getAmount())
        .instrument(gateioUserTradeRaw.getCurrencyPair())
        .price(gateioUserTradeRaw.getPrice())
        .timestamp(Date.from(gateioUserTradeRaw.getTimeMs()))
        .id(String.valueOf(gateioUserTradeRaw.getId()))
        .orderId(String.valueOf(gateioUserTradeRaw.getOrderId()))
        .feeAmount(gateioUserTradeRaw.getFee())
        .feeCurrency(gateioUserTradeRaw.getFeeCurrency())
        .orderUserReference(gateioUserTradeRaw.getRemark())
        .role(gateioUserTradeRaw.getRole())
        .build();
  }

  public GateioWithdrawalRequest toGateioWithdrawalRequest(GateioWithdrawFundsParams p) {
    return GateioWithdrawalRequest.builder()
        .clientRecordId(p.getClientRecordId())
        .address(p.getAddress())
        .tag(p.getAddressTag())
        .chain(p.getChain())
        .amount(p.getAmount())
        .currency(p.getCurrency())
        .build();
  }

  public Ticker toTicker(GateioTicker gateioTicker) {
    return new Ticker.Builder()
        .instrument(gateioTicker.getCurrencyPair())
        .last(gateioTicker.getLastPrice())
        .bid(gateioTicker.getHighestBid())
        .bidSize(gateioTicker.getHighestBidSize())
        .ask(gateioTicker.getLowestAsk())
        .askSize(gateioTicker.getLowestAskSize())
        .high(gateioTicker.getMaxPrice24h())
        .low(gateioTicker.getMinPrice24h())
        .volume(gateioTicker.getAssetVolume())
        .quoteVolume(gateioTicker.getQuoteVolume())
        .percentageChange(gateioTicker.getChangePercentage24h())
        .build();
  }

  public FundingRecord toFundingRecords(GateioAccountBookRecord gateioAccountBookRecord) {
    return FundingRecord.builder()
        .internalId(gateioAccountBookRecord.getId())
        .date(Date.from(gateioAccountBookRecord.getTimestamp()))
        .currency(gateioAccountBookRecord.getCurrency())
        .balance(gateioAccountBookRecord.getBalance())
        .type(gateioAccountBookRecord.getType())
        .amount(gateioAccountBookRecord.getChange().abs())
        .description(gateioAccountBookRecord.getTypeDescription())
        .build();
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

  public static FundingRate adaptFundingRate(GateioFundingRate gateioRate) {
    if (gateioRate.getRate() == null || gateioRate.getTimestamp() == null) {
      return null;
    }

    Instrument instrument = contractToInstrument(gateioRate.getContract());
    if (instrument == null) {
      return null;
    }

    // Gate.io provides 8-hour funding rate, convert to 1-hour rate
    BigDecimal fundingRate8h = gateioRate.getRate();
    BigDecimal fundingRate1h =
        fundingRate8h.divide(
            BigDecimal.valueOf(8), fundingRate8h.scale() + 3, RoundingMode.HALF_EVEN);

    // Timestamp is in seconds, convert to milliseconds
    long timestampMillis = gateioRate.getTimestamp() * 1000;
    Date nextFundingTime = new Date(timestampMillis);

    long effectiveInMinutes =
        (nextFundingTime.getTime() - System.currentTimeMillis()) / (1000 * 60);

    return new FundingRate.Builder()
        .instrument(instrument)
        .fundingRate1h(fundingRate1h)
        .fundingRate8h(fundingRate8h)
        .fundingRateDate(nextFundingTime)
        .fundingRateEffectiveInMinutes(effectiveInMinutes)
        .build();
  }

  public static FundingRates adaptFundingRates(List<GateioFundingRate> gateioRates) {
    List<FundingRate> fundingRates = new ArrayList<>();
    for (GateioFundingRate rate : gateioRates) {
      FundingRate fundingRate = adaptFundingRate(rate);
      if (fundingRate != null) {
        fundingRates.add(fundingRate);
      }
    }
    return new FundingRates(fundingRates);
  }
}
