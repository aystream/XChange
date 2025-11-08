package org.knowm.xchange.examples.gateio.streaming;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.marketdata.FundingRate;

/**
 * Example showing how to subscribe to real-time funding rate updates from Gate.io via WebSocket.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Connecting to Gate.io WebSocket API</li>
 *   <li>Subscribing to funding rate updates for USDT perpetual futures</li>
 *   <li>Receiving real-time updates via RxJava Observable</li>
 *   <li>Specifying the "settle" type (usdt, btc, usd)</li>
 * </ul>
 *
 * <p>Gate.io funding occurs every 8 hours at 00:00, 08:00, and 16:00 UTC.
 * The API provides 8-hour funding rates which are automatically converted to 1-hour rates.
 */
public class GateioStreamingFundingRateDemo {

  public static void main(String[] args) throws InterruptedException {
    // Create Gate.io streaming exchange instance with futures WebSocket URL
    info.bitrich.xchangestream.gateio.GateioStreamingExchange gateioExchange =
        new info.bitrich.xchangestream.gateio.GateioStreamingExchange();
    org.knowm.xchange.ExchangeSpecification spec = gateioExchange.getDefaultExchangeSpecification();

    // Set the futures WebSocket URL for USDT perpetual futures
    spec.setSslUri("wss://fx-ws.gateio.ws/v4/ws/usdt");

    gateioExchange.applySpecification(spec);
    StreamingExchange exchange = gateioExchange;

    // Connect to WebSocket API
    System.out.println("Connecting to Gate.io Futures WebSocket...");
    System.out.println("URL: wss://fx-ws.gateio.ws/v4/ws/usdt");
    exchange.connect().blockingAwait();
    System.out.println("Connected!");

    StreamingMarketDataService streamingMarketDataService =
        exchange.getStreamingMarketDataService();

    // Subscribe to funding rate updates for BTC/USDT perpetual futures
    FuturesContract btcUsdtPerp = new FuturesContract(CurrencyPair.BTC_USDT, "PERP");

    System.out.println("\nSubscribing to funding rate updates for " + btcUsdtPerp);
    System.out.println("Settle type: USDT (default)");
    System.out.println("Funding schedule: Every 8 hours at 00:00, 08:00, 16:00 UTC");
    System.out.println("(Press Ctrl+C to stop)\n");

    // Subscribe with "usdt" settle type for USDT perpetual futures
    // You can also specify: getFundingRate(btcUsdtPerp, "btc") or "usd" for BTC or USD settled contracts
    streamingMarketDataService
        .getFundingRate(btcUsdtPerp, "usdt")
        .subscribe(
            fundingRate -> {
              System.out.println("=== Funding Rate Update ===");
              System.out.println("Time: " + new java.util.Date());
              System.out.println("Instrument: " + fundingRate.getInstrument());
              System.out.println("Funding Rate (1h): " + fundingRate.getFundingRate1h());
              System.out.println("Funding Rate (8h): " + fundingRate.getFundingRate8h());
              System.out.println("Next Funding Time: " + fundingRate.getFundingRateDate());
              System.out.println(
                  "Effective In: " + fundingRate.getFundingRateEffectiveInMinutes() + " minutes");

              // Show percentage format
              java.math.BigDecimal rate1hPercent =
                  fundingRate.getFundingRate1h().multiply(java.math.BigDecimal.valueOf(100));
              java.math.BigDecimal rate8hPercent =
                  fundingRate.getFundingRate8h().multiply(java.math.BigDecimal.valueOf(100));

              System.out.println("\nFormatted:");
              System.out.println("  1h rate: " + rate1hPercent + "%");
              System.out.println("  8h rate: " + rate8hPercent + "%");

              // Determine if longs or shorts are paying
              if (fundingRate.getFundingRate1h().signum() > 0) {
                System.out.println("  → Longs pay Shorts (market is bullish)");
              } else if (fundingRate.getFundingRate1h().signum() < 0) {
                System.out.println("  → Shorts pay Longs (market is bearish)");
              } else {
                System.out.println("  → No funding payment (neutral)");
              }
              System.out.println();
            },
            throwable -> System.err.println("Error: " + throwable.getMessage()));

    // Example: Subscribe to another instrument (uncomment to use)
    // FuturesContract ethUsdtPerp = new FuturesContract(CurrencyPair.ETH_USDT, "PERP");
    // streamingMarketDataService.getFundingRate(ethUsdtPerp, "usdt")
    //     .subscribe(...);

    // Keep the application running to receive updates
    Thread.sleep(120000); // Run for 2 minutes

    // Disconnect
    System.out.println("\nDisconnecting...");
    exchange.disconnect().blockingAwait();
    System.out.println("Disconnected");
  }
}
