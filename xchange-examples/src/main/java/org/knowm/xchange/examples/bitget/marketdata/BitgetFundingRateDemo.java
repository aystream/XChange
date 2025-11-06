package org.knowm.xchange.examples.bitget.marketdata;

import java.io.IOException;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.bitget.BitgetFuturesExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.service.marketdata.MarketDataService;

/**
 * Example showing how to fetch funding rates from Bitget futures exchange.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Fetching a single funding rate for a specific futures contract</li>
 *   <li>Fetching all available funding rates</li>
 * </ul>
 *
 * <p>Note: Bitget provides 8-hour funding rates which are automatically converted to 1-hour rates.
 */
public class BitgetFundingRateDemo {

  public static void main(String[] args) throws IOException {
    // Create Bitget Futures exchange instance
    Exchange exchange = ExchangeFactory.INSTANCE.createExchange(BitgetFuturesExchange.class);
    MarketDataService marketDataService = exchange.getMarketDataService();

    // Example 1: Get funding rate for a specific futures contract
    System.out.println("=== Single Funding Rate ===");
    FuturesContract ethUsdtPerp = new FuturesContract(CurrencyPair.ETH_USDT, "PERP");
    FundingRate fundingRate = marketDataService.getFundingRate(ethUsdtPerp);

    if (fundingRate != null) {
      System.out.println("Instrument: " + fundingRate.getInstrument());
      System.out.println("Funding Rate (1h): " + fundingRate.getFundingRate1h());
      System.out.println("Funding Rate (8h): " + fundingRate.getFundingRate8h());
      System.out.println("Next Funding Time: " + fundingRate.getFundingRateDate());
      System.out.println("Effective In Minutes: " + fundingRate.getFundingRateEffectiveInMinutes());
    }

    // Example 2: Get all funding rates for USDT perpetual futures
    System.out.println("\n=== All Funding Rates ===");
    FundingRates allRates = marketDataService.getFundingRates();
    System.out.println("Total funding rates: " + allRates.getFundingRates().size());

    // Display first 5 rates
    allRates.getFundingRates().stream()
        .limit(5)
        .forEach(rate -> {
          System.out.println("\nInstrument: " + rate.getInstrument());
          System.out.println("  Rate (1h): " + rate.getFundingRate1h());
          System.out.println("  Rate (8h): " + rate.getFundingRate8h());
          System.out.println("  Next funding: " + rate.getFundingRateDate());
        });
  }
}
