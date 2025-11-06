package info.bitrich.xchangestream.mexc;

import com.fasterxml.jackson.databind.JsonNode;
import dto.MEXCSubscribeMessage;
import dto.MEXCSubscribeMessage.MEXCSubscribeParam;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import info.bitrich.xchangestream.service.netty.WebSocketClientCompressionAllowClientNoContextHandler;
import info.bitrich.xchangestream.service.netty.WebSocketClientHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEXCStreamingService extends JsonNettyStreamingService {

  private final Logger LOG = LoggerFactory.getLogger(MEXCStreamingService.class);
  private final Observable<Long> pingPongSrc = Observable.interval(15, 20, TimeUnit.SECONDS);
  private Disposable pingPongSubscription;

  public MEXCStreamingService(String apiUrl) {
    super(apiUrl);
  }

  @Override
  public Completable connect() {
    Completable conn = super.connect();
    return conn.andThen(
        (CompletableSource)
            (completable) -> {
              pingPongDisconnectIfConnected();
              pingPongSubscription =
                  pingPongSrc.subscribe(o -> this.sendMessage("{\"method\":\"ping\"}"));
              completable.onComplete();
            });
  }

  @Override
  protected String getChannelNameFromMessage(JsonNode message) {
    if (message.has("channel")) {
      String channel = message.get("channel").asText();
      // For ticker messages, append the symbol to create unique channel
      if (channel.equals("push.ticker") && message.has("symbol")) {
        return channel + "." + message.get("symbol").asText();
      }
      return channel;
    }
    return "";
  }

  @Override
  public String getSubscribeMessage(String channelName, Object... args) throws IOException {
    LOG.info("getSubscribeMessage {}", channelName);
    // channelName format: "push.ticker.BTC_USDT"
    String[] parts = channelName.split("\\.", 3);
    if (parts.length == 3) {
      String channel = parts[0] + "." + parts[1]; // "push.ticker"
      String symbol = parts[2]; // "BTC_USDT"
      return objectMapper.writeValueAsString(
          new MEXCSubscribeMessage("sub." + channel, new MEXCSubscribeParam(symbol)));
    }
    throw new IOException("Invalid channel name format: " + channelName);
  }

  @Override
  public String getUnsubscribeMessage(String channelName, Object... args) throws IOException {
    LOG.info("getUnsubscribeMessage {}", channelName);
    String[] parts = channelName.split("\\.", 3);
    if (parts.length == 3) {
      String channel = parts[0] + "." + parts[1];
      String symbol = parts[2];
      return objectMapper.writeValueAsString(
          new MEXCSubscribeMessage("unsub." + channel, new MEXCSubscribeParam(symbol)));
    }
    throw new IOException("Invalid channel name format: " + channelName);
  }

  @Override
  public void messageHandler(String message) {
    LOG.debug("Received message: {}", message);
    JsonNode jsonNode;
    try {
      jsonNode = objectMapper.readTree(message);
    } catch (IOException e) {
      LOG.error("Error parsing incoming message to JSON: {}", message);
      return;
    }

    // Handle pong response
    if (jsonNode.has("channel") && jsonNode.get("channel").asText().equals("pong")) {
      LOG.debug("Received PONG message");
      return;
    }

    // Handle subscription confirmations
    if (jsonNode.has("channel")
        && (jsonNode.get("channel").asText().equals("rs.error")
            || jsonNode.get("channel").asText().startsWith("rs.sub."))) {
      LOG.debug("Subscription response: {}", message);
      return;
    }

    handleMessage(jsonNode);
  }

  public void pingPongDisconnectIfConnected() {
    if (pingPongSubscription != null && !pingPongSubscription.isDisposed()) {
      pingPongSubscription.dispose();
    }
  }

  @Override
  protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
    return WebSocketClientCompressionAllowClientNoContextHandler.INSTANCE;
  }

  @Override
  protected WebSocketClientHandler getWebSocketClientHandler(
      WebSocketClientHandshaker handshake, WebSocketClientHandler.WebSocketMessageHandler handler) {
    LOG.info("Registering MEXCWebSocketClientHandler");
    return new WebSocketClientHandler(handshake, handler);
  }

  @Override
  public Completable disconnect() {
    pingPongDisconnectIfConnected();
    return super.disconnect();
  }
}
