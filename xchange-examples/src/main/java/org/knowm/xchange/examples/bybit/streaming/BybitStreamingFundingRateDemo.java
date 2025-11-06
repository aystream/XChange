package org.knowm.xchange.examples.bybit.streaming;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.dto.marketdata.FundingRate;

/**
 * Example showing how to subscribe to real-time funding rate updates from Bybit via WebSocket.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Connecting to Bybit WebSocket API</li>
 *   <li>Subscribing to funding rate updates for a specific futures contract</li>
 *   <li>Receiving real-time updates via RxJava Observable</li>
 * </ul>
 *
 * <p>The subscription uses the "tickers" channel which provides funding rate information
 * along with other ticker data.
 */
public class BybitStreamingFundingRateDemo {

  public static void main(String[] args) throws InterruptedException {
    // Create Bybit streaming exchange instance
    StreamingExchange exchange =
        StreamingExchangeFactory.INSTANCE.createExchange(
            info.bitrich.xchangestream.bybit.BybitStreamingExchange.class);

    // Connect to WebSocket API
    System.out.println("Connecting to Bybit WebSocket...");
    exchange.connect().blockingAwait();
    System.out.println("Connected!");

    StreamingMarketDataService streamingMarketDataService =
        exchange.getStreamingMarketDataService();

    // Subscribe to funding rate updates for BTC/USDT perpetual futures
    FuturesContract btcUsdtPerp = new FuturesContract(CurrencyPair.BTC_USDT, "PERP");

    System.out.println("\nSubscribing to funding rate updates for " + btcUsdtPerp);
    System.out.println("(Press Ctrl+C to stop)\n");

    streamingMarketDataService
        .getFundingRate(btcUsdtPerp)
        .subscribe(
            fundingRate -> {
              System.out.println("=== Funding Rate Update ===");
              System.out.println("Instrument: " + fundingRate.getInstrument());
              System.out.println("Funding Rate (1h): " + fundingRate.getFundingRate1h());
              System.out.println("Funding Rate (8h): " + fundingRate.getFundingRate8h());
              System.out.println("Next Funding Time: " + fundingRate.getFundingRateDate());
              System.out.println(
                  "Effective In Minutes: " + fundingRate.getFundingRateEffectiveInMinutes());
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
