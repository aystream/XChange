package org.knowm.xchange.mexc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.mexc.dto.account.MEXCBalance;
import org.knowm.xchange.mexc.dto.marketdata.MEXCFundingRate;
import org.knowm.xchange.mexc.dto.trade.MEXCOrder;
import org.knowm.xchange.mexc.dto.trade.MEXCOrderRequestPayload;

public class MEXCAdapters {

  public static Wallet adaptMEXCBalances(Map<String, MEXCBalance> mexcBalances) {
    List<Balance> balances = new ArrayList<>(mexcBalances.size());
    for (Map.Entry<String, MEXCBalance> mexcBalance : mexcBalances.entrySet()) {
      MEXCBalance mexcBalanceValue = mexcBalance.getValue();
      BigDecimal available = new BigDecimal(mexcBalanceValue.getAvailable());
      BigDecimal frozen = new BigDecimal(mexcBalanceValue.getFrozen());
      balances.add(
          new Balance(new Currency(mexcBalance.getKey()), frozen.add(available), available));
    }
    return Wallet.Builder.from(balances).build();
  }

  public static String convertToMEXCSymbol(String instrumentName) {
    return instrumentName.replace("/", "_").toUpperCase();
  }

  private static Instrument adaptSymbol(String symbol) {
    String[] symbolTokenized = symbol.split("_");
    return new CurrencyPair(symbolTokenized[0], symbolTokenized[1]);
  }

  public static MEXCOrderRequestPayload adaptOrder(LimitOrder limitOrder) {
    return new MEXCOrderRequestPayload(
        convertToMEXCSymbol(limitOrder.getInstrument().toString()),
        limitOrder.getLimitPrice().toString(),
        limitOrder.getOriginalAmount().toString(),
        limitOrder.getType().toString(),
        "LIMIT_ORDER",
        null);
  }

  public static Order adaptOrder(MEXCOrder mexcOrder) {

    BigDecimal dealQuantity = new BigDecimal(mexcOrder.getDealQuantity());
    LimitOrder limitOrder =
        new LimitOrder(
            Order.OrderType.valueOf(mexcOrder.getType()),
            new BigDecimal(mexcOrder.getQuantity()),
            dealQuantity,
            adaptSymbol(mexcOrder.getSymbol()),
            mexcOrder.getId(),
            new Date(mexcOrder.getCreateTime()),
            new BigDecimal(mexcOrder.getPrice())) {};
    BigDecimal dealAmount = new BigDecimal(mexcOrder.getDealAmount());
    BigDecimal averagePrice = getAveragePrice(dealQuantity, dealAmount);
    limitOrder.setAveragePrice(averagePrice);
    limitOrder.setOrderStatus(Order.OrderStatus.valueOf(mexcOrder.getState()));
    return limitOrder;
  }

  private static BigDecimal getAveragePrice(BigDecimal dealQuantity, BigDecimal dealAmount) {
    if (dealQuantity.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return dealAmount.divide(dealQuantity, RoundingMode.HALF_EVEN);
  }

  public static Instrument symbolToInstrument(String symbol) {
    if (symbol == null) {
      return null;
    }
    // MEXC symbol format: BTCUSDT (no separator)
    // Try to parse as futures contract - assume last 4 chars are quote currency
    if (symbol.length() > 4) {
      String quoteCurrency = symbol.substring(symbol.length() - 4);
      if (quoteCurrency.equals("USDT") || quoteCurrency.equals("USDC")) {
        String baseCurrency = symbol.substring(0, symbol.length() - 4);
        CurrencyPair pair = new CurrencyPair(baseCurrency, quoteCurrency);
        return new FuturesContract(pair, "PERP");
      }
      // Try 3-char quote currency (e.g., USD, BTC, ETH)
      String quoteCurrency3 = symbol.substring(symbol.length() - 3);
      String baseCurrency3 = symbol.substring(0, symbol.length() - 3);
      CurrencyPair pair = new CurrencyPair(baseCurrency3, quoteCurrency3);
      return new FuturesContract(pair, "PERP");
    }
    return null;
  }

  public static FundingRate adaptFundingRate(MEXCFundingRate mexcRate) {
    if (mexcRate.getFundingRate() == null || mexcRate.getNextFundingTime() == null) {
      return null;
    }

    Instrument instrument = symbolToInstrument(mexcRate.getSymbol());
    if (instrument == null) {
      return null;
    }

    // MEXC provides 8-hour funding rate, convert to 1-hour rate
    BigDecimal fundingRate8h = mexcRate.getFundingRate();
    BigDecimal fundingRate1h =
        fundingRate8h.divide(
            BigDecimal.valueOf(8), fundingRate8h.scale() + 3, RoundingMode.HALF_EVEN);

    // nextFundingTime is in milliseconds
    Date nextFundingTime = new Date(mexcRate.getNextFundingTime());

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

  public static FundingRates adaptFundingRates(List<MEXCFundingRate> mexcRates) {
    List<FundingRate> fundingRates = new ArrayList<>();
    for (MEXCFundingRate rate : mexcRates) {
      FundingRate fundingRate = adaptFundingRate(rate);
      if (fundingRate != null) {
        fundingRates.add(fundingRate);
      }
    }
    return new FundingRates(fundingRates);
  }
}
