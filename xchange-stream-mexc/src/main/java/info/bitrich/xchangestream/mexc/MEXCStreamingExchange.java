package info.bitrich.xchangestream.mexc;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel.State;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import org.knowm.xchange.mexc.MEXCExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEXCStreamingExchange extends MEXCExchange implements StreamingExchange {

  private final Logger LOG = LoggerFactory.getLogger(MEXCStreamingExchange.class);

  // MEXC Futures WebSocket URL
  public static final String URI = "wss://contract.mexc.com/edge";

  private MEXCStreamingService streamingService;
  private MEXCStreamingMarketDataService streamingMarketDataService;

  @Override
  protected void initServices() {
    super.initServices();
    this.streamingService = new MEXCStreamingService(URI);
    this.streamingMarketDataService = new MEXCStreamingMarketDataService(streamingService);
  }

  @Override
  public Completable connect(ProductSubscription... args) {
    LOG.info("Connect to MEXC Stream");
    return streamingService.connect();
  }

  @Override
  public Completable disconnect() {
    if (streamingService != null) {
      streamingService.pingPongDisconnectIfConnected();
      Completable disconnect = streamingService.disconnect();
      streamingService = null;
      return disconnect;
    }
    return Completable.complete();
  }

  @Override
  public boolean isAlive() {
    return streamingService != null && streamingService.isSocketOpen();
  }

  @Override
  public void useCompressedMessages(boolean compressedMessages) {
    streamingService.useCompressedMessages(compressedMessages);
  }

  @Override
  public MEXCStreamingMarketDataService getStreamingMarketDataService() {
    return streamingMarketDataService;
  }

  @Override
  public Observable<Throwable> reconnectFailure() {
    return streamingService.subscribeReconnectFailure();
  }

  @Override
  public Observable<State> connectionStateObservable() {
    return streamingService.subscribeConnectionState();
  }

  @Override
  public void resubscribeChannels() {
    streamingService.resubscribeChannels();
  }
}
