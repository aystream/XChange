package info.bitrich.xchangestream.mexc;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.MEXCWebSocketMessage;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.rxjava3.core.Observable;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEXCStreamingMarketDataService implements StreamingMarketDataService {

  private final Logger LOG = LoggerFactory.getLogger(MEXCStreamingMarketDataService.class);
  private final MEXCStreamingService streamingService;
  private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

  public static final String TICKER = "push.ticker";

  public MEXCStreamingMarketDataService(MEXCStreamingService streamingService) {
    this.streamingService = streamingService;
  }

  @Override
  public Observable<FundingRate> getFundingRate(Instrument instrument, Object... args) {
    String symbol = MEXCStreamAdapters.instrumentToSymbol(instrument);
    String channelUniqueId = TICKER + "." + symbol;

    return streamingService
        .subscribeChannel(channelUniqueId)
        .filter(message -> message.has("data"))
        .map(
            jsonNode -> {
              try {
                MEXCWebSocketMessage wsMessage =
                    mapper.treeToValue(jsonNode, MEXCWebSocketMessage.class);
                return MEXCStreamAdapters.adaptFundingRate(wsMessage);
              } catch (Exception e) {
                LOG.error("Error parsing MEXC ticker message: {}", e.getMessage());
                return null;
              }
            })
        .filter(fundingRate -> fundingRate != null);
  }
}
