package info.bitrich.xchangestream.mexc;

import dto.MEXCWebSocketMessage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.instrument.Instrument;

public class MEXCStreamAdapters {

  /**
   * Adapts MEXC WebSocket ticker message to XChange FundingRate.
   *
   * <p>MEXC provides 8-hour funding rates. We convert to 1-hour rate by dividing by 8.
   *
   * @param message MEXC WebSocket message
   * @return XChange FundingRate
   */
  public static FundingRate adaptFundingRate(MEXCWebSocketMessage message) {
    if (message == null || message.getData() == null) {
      return null;
    }

    BigDecimal fundingRate8h = message.getData().getFundingRate();
    if (fundingRate8h == null) {
      return null;
    }

    // Convert 8-hour rate to 1-hour rate
    BigDecimal fundingRate1h =
        fundingRate8h.divide(BigDecimal.valueOf(8), fundingRate8h.scale() + 3, RoundingMode.HALF_EVEN);

    // Parse instrument from symbol (e.g., "BTC_USDT")
    Instrument instrument = symbolToInstrument(message.getData().getSymbol());
    if (instrument == null) {
      return null;
    }

    // MEXC funding occurs every 8 hours (00:00, 08:00, 16:00 UTC)
    // Calculate next funding time
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

  public static FundingRate adaptFundingRate(MEXCWebSocketMessage message, Date cachedNextFundingTime) {
    if (message == null
        || message.getData() == null
        || message.getData().getFundingRate() == null) {
      return null;
    }

    // MEXC provides 8-hour funding rate, convert to 1-hour rate
    BigDecimal fundingRate8h = message.getData().getFundingRate();
    BigDecimal fundingRate1h =
        fundingRate8h.divide(
            BigDecimal.valueOf(8), fundingRate8h.scale() + 3, RoundingMode.HALF_EVEN);

    // Parse instrument from symbol (e.g., "BTC_USDT")
    Instrument instrument = symbolToInstrument(message.getData().getSymbol());
    if (instrument == null) {
      return null;
    }

    // Use cached nextFundingTime from REST API if available, otherwise calculate locally
    Date nextFundingTime;
    long now = System.currentTimeMillis();

    if (cachedNextFundingTime != null && cachedNextFundingTime.getTime() > now) {
      // Use cached value from REST API
      nextFundingTime = cachedNextFundingTime;
    } else {
      // Fall back to local calculation (MEXC funding occurs every 8 hours at 00:00, 08:00, 16:00 UTC)
      long eightHoursMs = 8 * 60 * 60 * 1000;
      long nextFundingMs = ((now / eightHoursMs) + 1) * eightHoursMs;
      nextFundingTime = new Date(nextFundingMs);
    }

    long effectiveInMinutes = (nextFundingTime.getTime() - now) / (1000 * 60);

    return new FundingRate.Builder()
        .instrument(instrument)
        .fundingRate1h(fundingRate1h)
        .fundingRate8h(fundingRate8h)
        .fundingRateDate(nextFundingTime)
        .fundingRateEffectiveInMinutes(effectiveInMinutes)
        .build();
  }

  /**
   * Converts MEXC symbol format to XChange Instrument.
   *
   * <p>MEXC format: BTC_USDT
   *
   * @param symbol MEXC symbol
   * @return XChange Instrument (FuturesContract with PERP)
   */
  public static Instrument symbolToInstrument(String symbol) {
    if (symbol == null || !symbol.contains("_")) {
      return null;
    }

    String[] parts = symbol.split("_");
    if (parts.length != 2) {
      return null;
    }

    CurrencyPair pair = new CurrencyPair(parts[0], parts[1]);
    return new FuturesContract(pair, "PERP");
  }

  /**
   * Converts XChange Instrument to MEXC symbol format.
   *
   * @param instrument XChange Instrument
   * @return MEXC symbol (e.g., "BTC_USDT")
   */
  public static String instrumentToSymbol(Instrument instrument) {
    if (instrument == null) {
      return null;
    }
    return instrument.getBase().getCurrencyCode() + "_" + instrument.getCounter().getCurrencyCode();
  }
}
