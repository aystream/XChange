package org.knowm.xchange.examples.mexc.marketdata;

import java.io.IOException;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.mexc.MEXCExchange;
import org.knowm.xchange.service.marketdata.MarketDataService;

/**
 * Example showing how to fetch funding rates from MEXC futures exchange.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Fetching a single funding rate for a specific futures contract</li>
 *   <li>Fetching all available funding rates</li>
 * </ul>
 *
 * <p>Note: MEXC provides 8-hour funding rates which are automatically converted to 1-hour rates.
 * Symbol format: BTCUSDT (no separator between base and quote currencies).
 */
public class MEXCFundingRateDemo {

  public static void main(String[] args) throws IOException {
    // Create MEXC exchange instance
    Exchange exchange = ExchangeFactory.INSTANCE.createExchange(MEXCExchange.class);
    MarketDataService marketDataService = exchange.getMarketDataService();

    // Example 1: Get funding rate for a specific futures contract
    System.out.println("=== Single Funding Rate ===");
    FuturesContract btcUsdtPerp = new FuturesContract(CurrencyPair.BTC_USDT, "PERP");
    FundingRate fundingRate = marketDataService.getFundingRate(btcUsdtPerp);

    if (fundingRate != null) {
      System.out.println("Instrument: " + fundingRate.getInstrument());
      System.out.println("Funding Rate (1h): " + fundingRate.getFundingRate1h());
      System.out.println("Funding Rate (8h): " + fundingRate.getFundingRate8h());
      System.out.println("Next Funding Time: " + fundingRate.getFundingRateDate());
      System.out.println("Effective In Minutes: " + fundingRate.getFundingRateEffectiveInMinutes());
    }

    // Example 2: Get all funding rates
    System.out.println("\n=== All Funding Rates ===");
    FundingRates allRates = marketDataService.getFundingRates();
    System.out.println("Total funding rates: " + allRates.getFundingRates().size());

    // Display first 10 rates with positive funding (shorts pay longs)
    System.out.println("\n=== Top 10 Positive Funding Rates (Shorts pay Longs) ===");
    allRates.getFundingRates().stream()
        .filter(rate -> rate.getFundingRate1h() != null &&
                       rate.getFundingRate1h().signum() > 0)
        .sorted((r1, r2) -> r2.getFundingRate1h().compareTo(r1.getFundingRate1h()))
        .limit(10)
        .forEach(rate -> {
          System.out.println("\nInstrument: " + rate.getInstrument());
          System.out.println("  Rate (1h): " + rate.getFundingRate1h() + " (" +
                           rate.getFundingRate1h().multiply(java.math.BigDecimal.valueOf(100)) + "%)");
          System.out.println("  Rate (8h): " + rate.getFundingRate8h());
          System.out.println("  Next funding in: " + rate.getFundingRateEffectiveInMinutes() + " minutes");
        });

    // Display first 10 rates with negative funding (longs pay shorts)
    System.out.println("\n=== Top 10 Negative Funding Rates (Longs pay Shorts) ===");
    allRates.getFundingRates().stream()
        .filter(rate -> rate.getFundingRate1h() != null &&
                       rate.getFundingRate1h().signum() < 0)
        .sorted((r1, r2) -> r1.getFundingRate1h().compareTo(r2.getFundingRate1h()))
        .limit(10)
        .forEach(rate -> {
          System.out.println("\nInstrument: " + rate.getInstrument());
          System.out.println("  Rate (1h): " + rate.getFundingRate1h() + " (" +
                           rate.getFundingRate1h().multiply(java.math.BigDecimal.valueOf(100)) + "%)");
          System.out.println("  Rate (8h): " + rate.getFundingRate8h());
          System.out.println("  Next funding in: " + rate.getFundingRateEffectiveInMinutes() + " minutes");
        });
  }
}
