package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class MEXCTickerData {
  @JsonProperty("symbol")
  private String symbol;

  @JsonProperty("lastPrice")
  private BigDecimal lastPrice;

  @JsonProperty("bid1")
  private BigDecimal bid1;

  @JsonProperty("ask1")
  private BigDecimal ask1;

  @JsonProperty("volume24")
  private BigDecimal volume24;

  @JsonProperty("high24Price")
  private BigDecimal high24Price;

  @JsonProperty("lower24Price")
  private BigDecimal lower24Price;

  @JsonProperty("riseFallRate")
  private BigDecimal riseFallRate;

  @JsonProperty("riseFallValue")
  private BigDecimal riseFallValue;

  @JsonProperty("fairPrice")
  private BigDecimal fairPrice;

  @JsonProperty("fundingRate")
  private BigDecimal fundingRate;

  @JsonProperty("indexPrice")
  private BigDecimal indexPrice;

  @JsonProperty("contractId")
  private Long contractId;

  @JsonProperty("maxBidPrice")
  private BigDecimal maxBidPrice;

  @JsonProperty("minAskPrice")
  private BigDecimal minAskPrice;

  @JsonProperty("holdVol")
  private BigDecimal holdVol;

  @JsonProperty("timestamp")
  private Long timestamp;
}
