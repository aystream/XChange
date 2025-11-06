package org.knowm.xchange.bitget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.knowm.xchange.bitget.dto.marketdata.BitgetContractDto;
import org.knowm.xchange.bitget.dto.marketdata.BitgetFuturesTickerDto;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.InstrumentMetaData;
import org.knowm.xchange.instrument.Instrument;

@UtilityClass
public class BitgetFuturesAdapters {

  public String toString(Instrument instrument) {
    return instrument == null
        ? null
        : instrument.getBase().toString() + instrument.getCounter().toString();
  }

  public String toString(Currency currency) {
    return Optional.ofNullable(currency).map(Currency::getCurrencyCode).orElse(null);
  }

  public InstrumentMetaData toInstrumentMetaData(BitgetContractDto bitgetContractDto) {
    InstrumentMetaData.InstrumentMetaDataBuilder builder =
        InstrumentMetaData.builder()
            .tradingFee(bitgetContractDto.getTakerFeeRate())
            .minimumAmount(bitgetContractDto.getMinTradeAssetAmount())
            .priceScale(bitgetContractDto.getPricePrecision())
            .volumeScale(bitgetContractDto.getAssetAmountPrecision())
            .amountStepSize(bitgetContractDto.getAssetAmountStepSize())
            .marketOrderEnabled(
                bitgetContractDto.getSymbolStatus() == BitgetContractDto.SymbolStatus.NORMAL);

    // set price step
    if (bitgetContractDto.getPriceEndStep() != null
        && bitgetContractDto.getPriceEndStep() > 0
        && bitgetContractDto.getPricePrecision() != null) {
      builder.priceStepSize(
          BigDecimal.ONE
              .scaleByPowerOfTen(-bitgetContractDto.getPricePrecision())
              .multiply(BigDecimal.valueOf(bitgetContractDto.getPriceEndStep())));
    }

    // set min quote amount for USDT
    if (bitgetContractDto
        .getFuturesContract()
        .getCurrencyPair()
        .getCounter()
        .equals(Currency.USDT)) {
      builder.counterMinimumAmount(bitgetContractDto.getMinTradeUSDT());
    }

    return builder.build();
  }

  public Ticker toTicker(BitgetFuturesTickerDto bitgetFuturesTickerDto) {
    if (bitgetFuturesTickerDto.getInstrument() == null) {
      return null;
    }
    return new Ticker.Builder()
        .instrument(bitgetFuturesTickerDto.getInstrument())
        .open(bitgetFuturesTickerDto.getOpen24h())
        .last(bitgetFuturesTickerDto.getLastPrice())
        .bid(bitgetFuturesTickerDto.getBestBidPrice())
        .ask(bitgetFuturesTickerDto.getBestAskPrice())
        .high(bitgetFuturesTickerDto.getHigh24h())
        .low(bitgetFuturesTickerDto.getLow24h())
        .volume(bitgetFuturesTickerDto.getAssetVolume24h())
        .quoteVolume(bitgetFuturesTickerDto.getQuoteVolume24h())
        .timestamp(toDate(bitgetFuturesTickerDto.getTimestamp()))
        .bidSize(bitgetFuturesTickerDto.getBestBidSize())
        .askSize(bitgetFuturesTickerDto.getBestAskSize())
        .percentageChange(bitgetFuturesTickerDto.getChange24h())
        .build();
  }

  public Date toDate(Instant instant) {
    return Optional.ofNullable(instant).map(Date::from).orElse(null);
  }

  public FundingRate adaptFundingRate(
      BitgetFuturesTickerDto ticker, Instrument instrument) {
    if (ticker.getFundingRate() == null || ticker.getNextFundingTime() == null) {
      return null;
    }

    // Bitget provides 8-hour funding rate, convert to 1-hour rate
    BigDecimal fundingRate8h = ticker.getFundingRate();
    BigDecimal fundingRate1h =
        fundingRate8h.divide(
            BigDecimal.valueOf(8), fundingRate8h.scale() + 3, RoundingMode.HALF_EVEN);

    // Parse the next funding time from string format (e.g., "1730275200000")
    Date nextFundingTime;
    try {
      long timestamp = Long.parseLong(ticker.getNextFundingTime());
      nextFundingTime = new Date(timestamp);
    } catch (NumberFormatException e) {
      return null;
    }

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

  public FundingRates adaptFundingRates(List<BitgetFuturesTickerDto> tickers) {
    List<FundingRate> fundingRates = new ArrayList<>();
    for (BitgetFuturesTickerDto ticker : tickers) {
      Instrument instrument = ticker.getInstrument();
      if (instrument != null) {
        FundingRate fundingRate = adaptFundingRate(ticker, instrument);
        if (fundingRate != null) {
          fundingRates.add(fundingRate);
        }
      }
    }
    return new FundingRates(fundingRates);
  }
}
