package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MEXCWebSocketMessage {
  @JsonProperty("channel")
  private String channel;

  @JsonProperty("data")
  private MEXCTickerData data;

  @JsonProperty("symbol")
  private String symbol;

  @JsonProperty("ts")
  private Long timestamp;
}
