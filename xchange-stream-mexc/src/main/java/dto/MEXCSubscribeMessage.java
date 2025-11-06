package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MEXCSubscribeMessage {
  @JsonProperty("method")
  private String method;

  @JsonProperty("param")
  private MEXCSubscribeParam param;

  @Data
  @AllArgsConstructor
  public static class MEXCSubscribeParam {
    @JsonProperty("symbol")
    private String symbol;
  }
}
