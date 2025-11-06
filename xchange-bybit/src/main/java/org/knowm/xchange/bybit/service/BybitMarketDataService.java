package org.knowm.xchange.bybit.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.knowm.xchange.bybit.BybitAdapters;
import org.knowm.xchange.bybit.BybitExchange;
import org.knowm.xchange.bybit.dto.BybitCategory;
import org.knowm.xchange.bybit.dto.BybitResult;
import org.knowm.xchange.bybit.dto.marketdata.tickers.BybitTicker;
import org.knowm.xchange.bybit.dto.marketdata.tickers.BybitTickers;
import org.knowm.xchange.bybit.dto.marketdata.tickers.linear.BybitLinearInverseTicker;
import org.knowm.xchange.bybit.dto.marketdata.tickers.option.BybitOptionTicker;
import org.knowm.xchange.bybit.dto.marketdata.tickers.spot.BybitSpotTicker;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.marketdata.params.Params;
import org.knowm.xchange.utils.Assert;

public class BybitMarketDataService extends BybitMarketDataServiceRaw implements MarketDataService {

  public BybitMarketDataService(BybitExchange exchange, ResilienceRegistries resilienceRegistries) {
    super(exchange, resilienceRegistries);
  }

  @Override
  public Ticker getTicker(Instrument instrument, Object... args) throws IOException {
    Assert.notNull(instrument, "Null instrument");

    BybitCategory category = BybitAdapters.getCategory(instrument);

    BybitResult<BybitTickers<BybitTicker>> response =
        getTicker24h(category, BybitAdapters.convertToBybitSymbol(instrument));

    if (response.getResult().getList().isEmpty()) {
      return new Ticker.Builder().build();
    } else {
      BybitTicker bybitTicker = response.getResult().getList().get(0);

      switch (category) {
        case SPOT:
          return BybitAdapters.adaptBybitSpotTicker(
              instrument, response.getTime(), (BybitSpotTicker) bybitTicker);
        case LINEAR:
        case INVERSE:
          return BybitAdapters.adaptBybitLinearInverseTicker(
              instrument, response.getTime(), (BybitLinearInverseTicker) bybitTicker);
        case OPTION:
          return BybitAdapters.adaptBybitOptionTicker(
              instrument, response.getTime(), (BybitOptionTicker) bybitTicker);
        default:
          throw new IllegalStateException("Unexpected value: " + category);
      }
    }
  }

  @Override
  public Ticker getTicker(CurrencyPair currencyPair, Object... args) throws IOException {
    return getTicker((Instrument) currencyPair, args);
  }

  @Override
  public List<Ticker> getTickers(Params params) throws IOException {
    // get category
    BybitCategory category;
    if (params == null) {
      category = BybitCategory.SPOT;
    } else if (!(params instanceof BybitCategory)) {
      throw new IllegalArgumentException("Params must be instance of BybitCategory");
    } else {
      category = (BybitCategory) params;
    }

    if (category == BybitCategory.OPTION) {
      throw new NotYetImplementedForExchangeException("category OPTION not yet implemented");
    }
    BybitResult<BybitTickers<BybitTicker>> response = getTickers(category);
    List<Ticker> result = new ArrayList<>();
    for (BybitTicker ticker : response.getResult().getList()) {
      switch (category) {
        case SPOT:
          result.add(
              BybitAdapters.adaptBybitSpotTicker(
                  BybitAdapters.convertBybitSymbolToInstrument(ticker.getSymbol(), category),
                  response.getTime(),
                  (BybitSpotTicker) ticker));
          break;
        case LINEAR:
        case INVERSE:
          result.add(
              BybitAdapters.adaptBybitLinearInverseTicker(
                  BybitAdapters.convertBybitSymbolToInstrument(ticker.getSymbol(), category),
                  response.getTime(),
                  (BybitLinearInverseTicker) ticker));
          break;
        default:
      }
    }
    return result;
  }

  @Override
  public FundingRate getFundingRate(Instrument instrument) throws IOException {
    Assert.notNull(instrument, "Null instrument");

    BybitCategory category = BybitAdapters.getCategory(instrument);

    // Funding rates are only available for linear and inverse perpetual contracts
    if (category != BybitCategory.LINEAR && category != BybitCategory.INVERSE) {
      throw new NotYetImplementedForExchangeException(
          "Funding rates are only available for LINEAR and INVERSE categories");
    }

    BybitResult<BybitTickers<BybitTicker>> response =
        getTicker24h(category, BybitAdapters.convertToBybitSymbol(instrument));

    if (response.getResult().getList().isEmpty()) {
      return null;
    }

    BybitLinearInverseTicker ticker =
        (BybitLinearInverseTicker) response.getResult().getList().get(0);
    return BybitAdapters.adaptFundingRate(ticker, instrument);
  }

  @Override
  public FundingRates getFundingRates() throws IOException {
    // Get funding rates for both LINEAR and INVERSE categories
    List<BybitLinearInverseTicker> allTickers = new ArrayList<>();

    // Get LINEAR tickers
    try {
      BybitResult<BybitTickers<BybitTicker>> linearResponse = getTickers(BybitCategory.LINEAR);
      for (BybitTicker ticker : linearResponse.getResult().getList()) {
        if (ticker instanceof BybitLinearInverseTicker) {
          allTickers.add((BybitLinearInverseTicker) ticker);
        }
      }
    } catch (Exception e) {
      // Continue if LINEAR fails
    }

    // Get INVERSE tickers
    try {
      BybitResult<BybitTickers<BybitTicker>> inverseResponse = getTickers(BybitCategory.INVERSE);
      for (BybitTicker ticker : inverseResponse.getResult().getList()) {
        if (ticker instanceof BybitLinearInverseTicker) {
          allTickers.add((BybitLinearInverseTicker) ticker);
        }
      }
    } catch (Exception e) {
      // Continue if INVERSE fails
    }

    List<FundingRate> fundingRates = new ArrayList<>();
    for (BybitLinearInverseTicker ticker : allTickers) {
      BybitCategory category =
          BybitAdapters.getCategory(
              BybitAdapters.convertBybitSymbolToInstrument(ticker.getSymbol(), BybitCategory.LINEAR));
      Instrument instrument = BybitAdapters.convertBybitSymbolToInstrument(ticker.getSymbol(), category);
      FundingRate fundingRate = BybitAdapters.adaptFundingRate(ticker, instrument);
      if (fundingRate != null) {
        fundingRates.add(fundingRate);
      }
    }

    return new FundingRates(fundingRates);
  }
}
