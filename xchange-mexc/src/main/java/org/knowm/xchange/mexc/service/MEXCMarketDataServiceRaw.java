package org.knowm.xchange.mexc.service;

import java.io.IOException;
import java.util.List;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.mexc.dto.marketdata.MEXCFundingRate;

public class MEXCMarketDataServiceRaw extends MEXCBaseService {
  public MEXCMarketDataServiceRaw(Exchange exchange) {
    super(exchange);
  }

  public List<MEXCFundingRate> getMEXCFundingRates() throws IOException {
    return mexc.getFundingRate(null);
  }

  public List<MEXCFundingRate> getMEXCFundingRate(String symbol) throws IOException {
    return mexc.getFundingRate(symbol);
  }
}
