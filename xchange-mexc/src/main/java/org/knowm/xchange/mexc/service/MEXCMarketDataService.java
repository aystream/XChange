package org.knowm.xchange.mexc.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.mexc.MEXCAdapters;
import org.knowm.xchange.mexc.dto.marketdata.MEXCFundingRate;
import org.knowm.xchange.service.marketdata.MarketDataService;

public class MEXCMarketDataService extends MEXCMarketDataServiceRaw implements MarketDataService {

  public MEXCMarketDataService(Exchange exchange) {
    super(exchange);
  }

  @Override
  public FundingRate getFundingRate(Instrument instrument) throws IOException {
    Objects.requireNonNull(instrument, "Instrument cannot be null");

    try {
      // Convert instrument to MEXC symbol format (e.g., BTCUSDT)
      String symbol =
          instrument.getBase().getCurrencyCode() + instrument.getCounter().getCurrencyCode();
      List<MEXCFundingRate> rates = getMEXCFundingRate(symbol);

      if (rates == null || rates.isEmpty()) {
        return null;
      }

      return MEXCAdapters.adaptFundingRate(rates.get(0));
    } catch (MEXCException e) {
      throw e;
    }
  }

  @Override
  public FundingRates getFundingRates() throws IOException {
    try {
      List<MEXCFundingRate> rates = getMEXCFundingRates();
      return MEXCAdapters.adaptFundingRates(rates);
    } catch (MEXCException e) {
      throw e;
    }
  }
}
