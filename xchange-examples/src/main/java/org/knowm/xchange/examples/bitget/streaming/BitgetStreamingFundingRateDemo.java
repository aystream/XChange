package org.knowm.xchange.examples.bitget.streaming;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.marketdata.FundingRate;

/**
 * Example showing how to subscribe to real-time funding rate updates from Bitget via WebSocket.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Connecting to Bitget WebSocket API</li>
 *   <li>Subscribing to funding rate updates for USDT perpetual futures</li>
 *   <li>Receiving real-time updates via RxJava Observable</li>
 * </ul>
 *
 * <p>The subscription uses the "futures/ticker" channel which provides funding rate information
 * along with other ticker data for USDT-FUTURES contracts.
 */
public class BitgetStreamingFundingRateDemo {

  public static void main(String[] args) throws InterruptedException {
    // Create Bitget streaming exchange instance
    StreamingExchange exchange =
        StreamingExchangeFactory.INSTANCE.createExchange(
            info.bitrich.xchangestream.bitget.BitgetStreamingExchange.class);

    // Connect to WebSocket API
    System.out.println("Connecting to Bitget WebSocket...");
    exchange.connect().blockingAwait();
    System.out.println("Connected!");

    StreamingMarketDataService streamingMarketDataService =
        exchange.getStreamingMarketDataService();

    // Subscribe to funding rate updates for ETH/USDT perpetual futures
    FuturesContract ethUsdtPerp = new FuturesContract(CurrencyPair.ETH_USDT, "PERP");

    System.out.println("\nSubscribing to funding rate updates for " + ethUsdtPerp);
    System.out.println("Note: Bitget provides 8-hour funding rates, converted to 1-hour rates");
    System.out.println("(Press Ctrl+C to stop)\n");

    streamingMarketDataService
        .getFundingRate(ethUsdtPerp)
        .subscribe(
            fundingRate -> {
              System.out.println("=== Funding Rate Update ===");
              System.out.println("Time: " + new java.util.Date());
              System.out.println("Instrument: " + fundingRate.getInstrument());
              System.out.println("Funding Rate (1h): " + fundingRate.getFundingRate1h());
              System.out.println("Funding Rate (8h): " + fundingRate.getFundingRate8h());
              System.out.println("Next Funding Time: " + fundingRate.getFundingRateDate());
              System.out.println(
                  "Effective In Minutes: " + fundingRate.getFundingRateEffectiveInMinutes());

              // Calculate annualized rate for comparison
              java.math.BigDecimal annualizedRate =
                  fundingRate
                      .getFundingRate8h()
                      .multiply(java.math.BigDecimal.valueOf(365 * 3)); // 3 times per day
              System.out.println(
                  "Annualized Rate: "
                      + annualizedRate.multiply(java.math.BigDecimal.valueOf(100))
                      + "%");
              System.out.println();
            },
            throwable -> System.err.println("Error: " + throwable.getMessage()));

    // Keep the application running to receive updates
    Thread.sleep(60000); // Run for 60 seconds

    // Disconnect
    System.out.println("\nDisconnecting...");
    exchange.disconnect().blockingAwait();
    System.out.println("Disconnected");
  }
}
