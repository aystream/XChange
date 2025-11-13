package info.bitrich.xchangestream.bybit;

import static info.bitrich.xchangestream.bybit.BybitStreamingExchange.EXCHANGE_TYPE;

import com.fasterxml.jackson.databind.JsonNode;
import dto.BybitSubscribeMessage;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import info.bitrich.xchangestream.service.netty.WebSocketClientCompressionAllowClientNoContextHandler;
import info.bitrich.xchangestream.service.netty.WebSocketClientHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bybit.dto.BybitCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BybitStreamingService extends JsonNettyStreamingService {

  private final Logger LOG = LoggerFactory.getLogger(BybitStreamingService.class);
  public final String exchange_type;
  private final Observable<Long> pingPongSrc = Observable.interval(15, 20, TimeUnit.SECONDS);
  private Disposable pingPongSubscription;
  private final ExchangeSpecification spec;
  @Setter private WebSocketClientHandler.WebSocketMessageHandler channelInactiveHandler = null;

  public BybitStreamingService(String apiUrl, ExchangeSpecification spec) {
    super(apiUrl);
    this.exchange_type =
        ((BybitCategory) spec.getExchangeSpecificParametersItem(EXCHANGE_TYPE)).getValue();
    this.spec = spec;
    //    this.setEnableLoggingHandler(true);
  }

  @Override
  public Completable connect() {
    Completable conn = super.connect();
    return conn.andThen(
        (CompletableSource)
            (completable) -> {
              pingPongDisconnectIfConnected();
              pingPongSubscription =
                  pingPongSrc.subscribe(o -> this.sendMessage("{\"op\":\"ping\"}"));
              completable.onComplete();
            });
  }

  @Override
  protected String getChannelNameFromMessage(JsonNode message) {
    if (message.has("topic")) {
      return message.get("topic").asText();
    }
    return "";
  }

  @Override
  public String getSubscribeMessage(String channelName, Object... args) throws IOException {
    // Support batch subscriptions: if args contains additional channel names, include them
    List<String> channels = new ArrayList<>();
    channels.add(channelName);

    // Add any additional channel names passed in args
    for (Object arg : args) {
      if (arg instanceof String) {
        channels.add((String) arg);
      }
    }

    LOG.info("getSubscribeMessage for {} channels: {}", channels.size(), channels);
    return objectMapper.writeValueAsString(
        new BybitSubscribeMessage("subscribe", channels));
  }

  @Override
  public String getUnsubscribeMessage(String channelName, Object... args) throws IOException {
    LOG.info("getUnsubscribeMessage {}", channelName);
    return objectMapper.writeValueAsString(
        new BybitSubscribeMessage("unsubscribe", Collections.singletonList(channelName)));
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
    String op = "";
    boolean success = false;
    if (jsonNode.has("op")) {
      op = jsonNode.get("op").asText();
    }
    if (jsonNode.has("success")) {
      success = jsonNode.get("success").asBoolean();
    }
    if (success) {
      switch (op) {
        case "subscribe":
        case "unsubscribe":
          {
            break;
          }
      }
      return;
    } else {
      // different op result of public channels and private channels
      // https://bybit-exchange.github.io/docs/v5/ws/connect#how-to-send-the-heartbeat-packet
      if (op.equals("ping") || op.equals("pong")) {
        LOG.debug("Received PONG message: {}", message);
        return;
      }
    }
    handleMessage(jsonNode);
  }

  public void pingPongDisconnectIfConnected() {
    if (pingPongSubscription != null && !pingPongSubscription.isDisposed()) {
      pingPongSubscription.dispose();
    }
  }

  /**
   * Subscribe to multiple channels in a single WebSocket message.
   * Bybit supports up to 1000 topics per connection.
   *
   * @param channelNames List of channel names to subscribe to (e.g., "tickers.BTCUSDT")
   */
  public void subscribeBatch(List<String> channelNames) {
    try {
      String subscribeMessage = objectMapper.writeValueAsString(
          new BybitSubscribeMessage("subscribe", channelNames));
      LOG.info("Sending batch subscription for {} channels", channelNames.size());
      LOG.debug("Batch subscription channels: {}", channelNames);
      sendMessage(subscribeMessage);
    } catch (IOException e) {
      LOG.error("Failed to send batch subscription", e);
    }
  }

  @Override
  protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
    return WebSocketClientCompressionAllowClientNoContextHandler.INSTANCE;
  }

  @Override
  protected WebSocketClientHandler getWebSocketClientHandler(
      WebSocketClientHandshaker handshake, WebSocketClientHandler.WebSocketMessageHandler handler) {
    LOG.info("Registering BybitWebSocketClientHandler");
    return new BybitWebSocketClientHandler(handshake, handler);
  }

  /**
   * Custom client handler in order to execute an external, user-provided handler on channel events.
   */
  class BybitWebSocketClientHandler extends NettyWebSocketClientHandler {

    public BybitWebSocketClientHandler(
        WebSocketClientHandshaker handshake, WebSocketMessageHandler handler) {
      super(handshake, handler);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      super.channelInactive(ctx);
      if (channelInactiveHandler != null) {
        channelInactiveHandler.onMessage("WebSocket Client disconnected!");
      }
    }
  }
}
